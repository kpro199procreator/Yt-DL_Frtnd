package com.ytmusicdl.app.ui.screens

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ytmusicdl.app.data.db.AppDatabase
import com.ytmusicdl.app.data.db.DownloadHistoryCacheEntity
import com.ytmusicdl.app.service.DownloadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private data class DownloadHistoryItem(
    val filePath: String,
    val title: String,
    val album: String,
    val duration: String,
    val coverBytes: ByteArray?,
)

@Composable
fun DownloadsHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<DownloadHistoryItem>>(emptyList()) }
    var showHistory by remember { mutableStateOf(false) }
    val queue by DownloadService.queueState.collectAsState()

    LaunchedEffect(Unit) {
        val dao = AppDatabase.get(context).historyCacheDao()
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "ytmusicdl")
        val files = dir.takeIf { it.exists() }?.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()

        withContext(Dispatchers.IO) {
            val cached = dao.getAll().associateBy { it.filePath }
            val toUpsert = mutableListOf<DownloadHistoryCacheEntity>()
            val result = files.map { file ->
                val cache = cached[file.absolutePath]
                if (cache != null && cache.lastModified == file.lastModified()) {
                    cache.toUiItem()
                } else {
                    val refreshed = readMetadata(file)
                    toUpsert.add(refreshed)
                    refreshed.toUiItem()
                }
            }
            dao.deleteMissing(files.map { it.absolutePath })
            if (toUpsert.isNotEmpty()) dao.upsertAll(toUpsert)
            withContext(Dispatchers.Main) { items = result }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
            IconButton(onClick = { showHistory = !showHistory }) { Icon(Icons.Default.History, contentDescription = "Historial") }
        }
        Text("Descargas", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Text("Progreso y cola actual", style = MaterialTheme.typography.titleMedium)
        if (queue.isEmpty()) Text("Sin elementos en cola.")
        queue.forEach { q ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text("${q.title} · ${if (q.album.isBlank()) "song" else "album/song"}")
                    Text("Estado: ${q.status}")
                    LinearProgressIndicator(progress = { q.progress / 100f }, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(10.dp))

        if (!showHistory) {
            Text("Pulsa historial para ver descargas previas.")
        } else if (items.isEmpty()) {
            Text("Sin descargas aún", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.filePath }) { item ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (item.coverBytes != null) {
                                val bitmap = remember(item.coverBytes) { BitmapFactory.decodeByteArray(item.coverBytes, 0, item.coverBytes.size) }
                                bitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "Carátula",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(56.dp),
                                    )
                                } ?: Icon(Icons.Default.MusicNote, contentDescription = null)
                            } else {
                                Icon(Icons.Default.MusicNote, contentDescription = null)
                            }

                            Column(Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Álbum: ${item.album}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Duración: ${item.duration}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun readMetadata(file: File): DownloadHistoryCacheEntity {
    val mmr = MediaMetadataRetriever()
    return try {
        mmr.setDataSource(file.absolutePath)
        val durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty().ifBlank { "Álbum desconocido" }
        val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty().ifBlank { file.nameWithoutExtension }
        DownloadHistoryCacheEntity(
            filePath = file.absolutePath,
            title = title,
            album = album,
            duration = formatDuration(durationMs),
            coverBytes = mmr.embeddedPicture,
            lastModified = file.lastModified(),
        )
    } catch (_: Exception) {
        DownloadHistoryCacheEntity(
            filePath = file.absolutePath,
            title = file.nameWithoutExtension,
            album = "Álbum desconocido",
            duration = "--:--",
            coverBytes = null,
            lastModified = file.lastModified(),
        )
    } finally {
        mmr.release()
    }
}

private fun DownloadHistoryCacheEntity.toUiItem() = DownloadHistoryItem(filePath, title, album, duration, coverBytes)

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "--:--"
    val totalSec = durationMs / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
