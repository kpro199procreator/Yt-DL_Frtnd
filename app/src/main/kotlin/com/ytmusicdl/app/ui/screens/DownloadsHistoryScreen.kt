package com.ytmusicdl.app.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ytmusicdl.app.data.db.AppDatabase
import com.ytmusicdl.app.data.db.DownloadHistoryCacheEntity
import com.ytmusicdl.app.service.DownloadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private val supportedAudioExtensions = setOf("mp3", "m4a", "wav", "webm")

private data class DownloadHistoryItem(
    val filePath: String,
    val title: String,
    val album: String,
    val duration: String,
    val coverBytes: ByteArray?,
)

@Composable
fun DownloadsHistoryScreen(onBack: () -> Unit, showQueueOnly: Boolean = false) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<DownloadHistoryItem>>(emptyList()) }
    val queue by DownloadService.queueState.collectAsState()

    LaunchedEffect(Unit) {
        val dao = AppDatabase.get(context).historyCacheDao()
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "ytmusicdl")
        val files = dir.takeIf { it.exists() }
            ?.listFiles()
            ?.filter { it.isFile && it.extension.lowercase(Locale.ROOT) in supportedAudioExtensions }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        withContext(Dispatchers.IO) {
            val cached = dao.getAll().associateBy { it.filePath }
            val toUpsert = mutableListOf<DownloadHistoryCacheEntity>()
            val result = files.map { file ->
                val cache = cached[file.absolutePath]
                if (cache != null && cache.lastModified == file.lastModified()) cache.toUiItem() else {
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
            Text(if (showQueueOnly) "Queue" else "Offline Library", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.size(48.dp))
        }

        Spacer(Modifier.height(10.dp))

        if (showQueueOnly) {
            QueueContent(queue = queue)
        } else {
            LibraryContent(items = items, onOpenFile = { item ->
                val file = File(item.filePath)
                if (!file.exists()) {
                    Toast.makeText(context, "Archivo no encontrado", Toast.LENGTH_SHORT).show()
                    return@LibraryContent
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeTypeFor(file))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Abrir con reproductor de música")
                runCatching { context.startActivity(chooser) }
                    .onFailure { Toast.makeText(context, "No hay reproductor disponible", Toast.LENGTH_SHORT).show() }
            })
        }
    }
}

@Composable
private fun QueueContent(queue: List<DownloadService.QueueItem>) {
    ElevatedCard(Modifier.fillMaxWidth().animateContentSize(), shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Estado de descargas", style = MaterialTheme.typography.titleLarge)
            Text(
                if (queue.isEmpty()) "No hay descargas activas." else "${queue.size} elemento(s) con progreso incremental en tiempo real.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("Queue") }, leadingIcon = { Icon(Icons.Default.HourglassTop, null) })
                AssistChip(onClick = {}, label = { Text("Material 3") })
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    Crossfade(targetState = queue.isEmpty(), label = "queue-crossfade") { emptyQueue ->
        if (emptyQueue) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Queue vacía", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(queue, key = { it.videoId }) { q ->
                    QueueItemCard(q)
                }
            }
        }
    }
}

@Composable
private fun QueueItemCard(item: DownloadService.QueueItem) {
    val progress = (item.progress.coerceIn(0, 100)) / 100f
    ElevatedCard(Modifier.fillMaxWidth().animateContentSize(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(statusIcon(item.status), contentDescription = null, tint = statusColor(item.status))
                Column(Modifier.weight(1f)) {
                    Text(item.title.ifBlank { "Descarga sin título" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    Text(statusLabel(item.status), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilterChip(selected = true, onClick = {}, label = { Text("${item.progress}%") })
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.extraLarge),
                color = statusColor(item.status),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("ETA ${if (item.etaSec >= 0) "${item.etaSec}s" else "--"}") }, leadingIcon = { Icon(Icons.Default.HourglassTop, null) })
                AssistChip(onClick = {}, label = { Text("${"%.2f".format(item.speedMbps)} MB/s") }, leadingIcon = { Icon(Icons.Default.Speed, null) })
                AssistChip(onClick = {}, label = { Text("m4a auto") }, leadingIcon = { Icon(Icons.Default.AudioFile, null) })
            }

            if (item.cliOutput.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 92.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        item.cliOutput.takeLast(360),
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryContent(items: List<DownloadHistoryItem>, onOpenFile: (DownloadHistoryItem) -> Unit) {
    Text("Archivos offline", style = MaterialTheme.typography.titleMedium)
    Text(
        "Solo audio: .mp3, .m4a, .wav y .webm",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))

    Crossfade(targetState = items.isEmpty(), label = "library-crossfade") { emptyLibrary ->
        if (emptyLibrary) {
            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                Text("Biblioteca vacía: aún no hay archivos de audio descargados")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(items, key = { it.filePath }) { item ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenFile(item) },
                        shape = MaterialTheme.shapes.large,
                    ) {
                        ListItem(
                            headlineContent = { Text(item.title.ifBlank { "Archivo sin título" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = {
                                Text(
                                    "${item.album.ifBlank { "Álbum desconocido" }} · ${item.duration.ifBlank { "--:--" }}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingContent = { Artwork(item.coverBytes) },
                            trailingContent = { Icon(Icons.Default.PlayArrow, contentDescription = "Abrir") },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Artwork(coverBytes: ByteArray?) {
    if (coverBytes != null) {
        val bitmap = remember(coverBytes) { BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size) }
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Carátula",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.medium),
            )
        } ?: Icon(Icons.Default.MusicNote, contentDescription = null)
    } else {
        Icon(Icons.Default.MusicNote, contentDescription = null)
    }
}

private fun readMetadata(file: File): DownloadHistoryCacheEntity {
    val mmr = MediaMetadataRetriever()
    return try {
        mmr.setDataSource(file.absolutePath)
        val durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty().ifBlank { "Álbum desconocido" }
        val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty().ifBlank { file.nameWithoutExtension }
        DownloadHistoryCacheEntity(file.absolutePath, title, album, formatDuration(durationMs), mmr.embeddedPicture, file.lastModified())
    } catch (_: Exception) {
        DownloadHistoryCacheEntity(file.absolutePath, file.nameWithoutExtension, "Álbum desconocido", "--:--", null, file.lastModified())
    } finally { mmr.release() }
}

private fun DownloadHistoryCacheEntity.toUiItem() = DownloadHistoryItem(filePath, title, album, duration, coverBytes)

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "--:--"
    val totalSec = durationMs / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

private fun mimeTypeFor(file: File): String = when (file.extension.lowercase(Locale.ROOT)) {
    "mp3" -> "audio/mpeg"
    "m4a" -> "audio/mp4"
    "wav" -> "audio/wav"
    "webm" -> "audio/webm"
    else -> "audio/*"
}

private fun statusLabel(status: String): String = when (status.lowercase(Locale.ROOT)) {
    "downloading" -> "Descargando: recibiendo datos y actualizando progreso"
    "queued" -> "En espera: listo para iniciar cuando haya un slot libre"
    "completed" -> "Completada: archivo guardado en la biblioteca"
    "failed" -> "Fallida: revisa el detalle del backend"
    else -> status.ifBlank { "Procesando" }
}

private fun statusIcon(status: String) = when (status.lowercase(Locale.ROOT)) {
    "completed" -> Icons.Default.CheckCircle
    "failed" -> Icons.Default.Error
    "downloading" -> Icons.Default.Speed
    else -> Icons.Default.HourglassTop
}

@Composable
private fun statusColor(status: String) = when (status.lowercase(Locale.ROOT)) {
    "completed" -> MaterialTheme.colorScheme.primary
    "failed" -> MaterialTheme.colorScheme.error
    "downloading" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.secondary
}
