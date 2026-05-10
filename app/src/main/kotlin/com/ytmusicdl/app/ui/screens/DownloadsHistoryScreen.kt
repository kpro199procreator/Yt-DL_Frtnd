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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File

private data class DownloadHistoryItem(
    val file: File,
    val album: String,
    val duration: String,
    val coverBytes: ByteArray?,
)

@Composable
fun DownloadsHistoryScreen(onBack: () -> Unit) {
    var items by remember { mutableStateOf<List<DownloadHistoryItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "ytmusicdl")
        val files = dir.takeIf { it.exists() }?.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
        items = files.map { file ->
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(file.absolutePath)
                val durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty().ifBlank { "Álbum desconocido" }
                DownloadHistoryItem(
                    file = file,
                    album = album,
                    duration = formatDuration(durationMs),
                    coverBytes = mmr.embeddedPicture,
                )
            } catch (_: Exception) {
                DownloadHistoryItem(file = file, album = "Álbum desconocido", duration = "--:--", coverBytes = null)
            } finally {
                mmr.release()
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
        Text("Descargas", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))

        if (items.isEmpty()) {
            Text("Sin descargas aún", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.file.absolutePath }) { item ->
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
                                Text(item.file.nameWithoutExtension, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "--:--"
    val totalSec = durationMs / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
