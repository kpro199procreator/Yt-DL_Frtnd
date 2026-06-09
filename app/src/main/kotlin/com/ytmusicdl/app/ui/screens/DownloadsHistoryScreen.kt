package com.ytmusicdl.app.ui.screens

import com.ytmusicdl.app.R

import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.res.stringResource
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

private val supportedAudioExtensions = setOf("mp3", "wav", "flac")

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
    val unknownAlbum = stringResource(R.string.unknown_album)
    val noDuration = stringResource(R.string.no_duration)

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
                    val refreshed = readMetadata(file, unknownAlbum, noDuration)
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
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back)) }
            Text(if (showQueueOnly) stringResource(R.string.queue_title) else stringResource(R.string.offline_library_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.size(48.dp))
        }

        Spacer(Modifier.height(10.dp))

        if (showQueueOnly) {
            QueueContent(queue = queue)
        } else {
            LibraryContent(items = items, onOpenFile = { item ->
                val file = File(item.filePath)
                if (!file.exists()) {
                    Toast.makeText(context, context.getString(R.string.file_not_found), Toast.LENGTH_SHORT).show()
                    return@LibraryContent
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeTypeFor(file))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, context.getString(R.string.open_with_music_player))
                runCatching { context.startActivity(chooser) }
                    .onFailure { Toast.makeText(context, context.getString(R.string.no_music_player), Toast.LENGTH_SHORT).show() }
            })
        }
    }
}

@Composable
private fun QueueContent(queue: List<DownloadService.QueueItem>) {
    ElevatedCard(Modifier.fillMaxWidth().animateContentSize(), shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.downloads_status_title), style = MaterialTheme.typography.titleLarge)
            Text(
                if (queue.isEmpty()) stringResource(R.string.no_active_downloads) else stringResource(R.string.queue_count_status, queue.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.queue_title)) }, leadingIcon = { Icon(Icons.Default.HourglassTop, null) })
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.material3)) })
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    Crossfade(targetState = queue.isEmpty(), label = "queue-crossfade") { emptyQueue ->
        if (emptyQueue) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.queue_empty), style = MaterialTheme.typography.titleMedium)
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
                    Text(item.title.ifBlank { stringResource(R.string.download_without_title) }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    Text(statusLabel(item.status), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilterChip(selected = true, onClick = {}, label = { Text(stringResource(R.string.progress_percent, item.progress)) })
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.extraLarge),
                color = statusColor(item.status),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            AnimatedVisibility(
                visible = item.status == "done" || item.status == "completed",
                enter = slideInHorizontally(initialOffsetX = { -it }) + expandVertically() + fadeIn(),
                exit = fadeOut(),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null)
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Text(stringResource(R.string.download_moved_to_library), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.eta_value, if (item.etaSec >= 0) "${item.etaSec}s" else stringResource(R.string.no_duration))) }, leadingIcon = { Icon(Icons.Default.HourglassTop, null) })
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.speed_value, item.speedMbps)) }, leadingIcon = { Icon(Icons.Default.Speed, null) })
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.format_m4a_auto)) }, leadingIcon = { Icon(Icons.Default.AudioFile, null) })
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
    Text(stringResource(R.string.offline_files), style = MaterialTheme.typography.titleMedium)
    Text(
        "Solo audio: .mp3, .wav y .flac",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))

    Crossfade(targetState = items.isEmpty(), label = "library-crossfade") { emptyLibrary ->
        if (emptyLibrary) {
            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.empty_audio_library))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(items, key = { it.filePath }) { item ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenFile(item) },
                        shape = MaterialTheme.shapes.large,
                    ) {
                        ListItem(
                            headlineContent = { Text(item.title.ifBlank { stringResource(R.string.unknown_file_title) }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = {
                                Text(
                                    stringResource(R.string.library_item_subtitle, item.album.ifBlank { stringResource(R.string.unknown_album) }, item.duration.ifBlank { stringResource(R.string.no_duration) }),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingContent = { Artwork(item.coverBytes) },
                            trailingContent = { Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.open)) },
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
                contentDescription = stringResource(R.string.cover_art),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.medium),
            )
        } ?: Icon(Icons.Default.MusicNote, contentDescription = null)
    } else {
        Icon(Icons.Default.MusicNote, contentDescription = null)
    }
}

private fun readMetadata(file: File, unknownAlbum: String, noDuration: String): DownloadHistoryCacheEntity {
    val mmr = MediaMetadataRetriever()
    return try {
        mmr.setDataSource(file.absolutePath)
        val durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty().ifBlank { unknownAlbum }
        val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty().ifBlank { file.nameWithoutExtension }
        DownloadHistoryCacheEntity(file.absolutePath, title, album, formatDuration(durationMs, noDuration), mmr.embeddedPicture, file.lastModified())
    } catch (_: Exception) {
        DownloadHistoryCacheEntity(file.absolutePath, file.nameWithoutExtension, unknownAlbum, noDuration, null, file.lastModified())
    } finally { mmr.release() }
}

private fun DownloadHistoryCacheEntity.toUiItem() = DownloadHistoryItem(filePath, title, album, duration, coverBytes)

private fun formatDuration(durationMs: Long, noDuration: String): String {
    if (durationMs <= 0L) return noDuration
    val totalSec = durationMs / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

private fun mimeTypeFor(file: File): String = when (file.extension.lowercase(Locale.ROOT)) {
    "mp3" -> "audio/mpeg"
    "wav" -> "audio/wav"
    "flac" -> "audio/flac"
    else -> "audio/*"
}

@Composable
private fun statusLabel(status: String): String = when (status.lowercase(Locale.ROOT)) {
    "downloading" -> stringResource(R.string.status_downloading_verbose)
    "queued" -> stringResource(R.string.status_queued_verbose)
    "done", "completed" -> stringResource(R.string.status_completed_verbose)
    "error", "failed" -> stringResource(R.string.status_failed_verbose)
    else -> status.ifBlank { stringResource(R.string.status_processing) }
}

private fun statusIcon(status: String) = when (status.lowercase(Locale.ROOT)) {
    "done", "completed" -> Icons.Default.CheckCircle
    "error", "failed" -> Icons.Default.Error
    "downloading" -> Icons.Default.Speed
    else -> Icons.Default.HourglassTop
}

@Composable
private fun statusColor(status: String) = when (status.lowercase(Locale.ROOT)) {
    "done", "completed" -> MaterialTheme.colorScheme.primary
    "error", "failed" -> MaterialTheme.colorScheme.error
    "downloading" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.secondary
}
