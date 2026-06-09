package com.ytmusicdl.app.ui.screens

import com.ytmusicdl.app.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytmusicdl.app.data.model.Track
import com.ytmusicdl.app.service.DownloadService

@Composable
fun SongDownloadScreen(track: Track, onBack: () -> Unit) {
    val context = LocalContext.current
    val queue by DownloadService.queueState.collectAsState()
    val task = queue.firstOrNull { it.videoId == track.videoId }
    var showProgress by remember { mutableStateOf(false) }
    val isComplete = task?.status == "done" || task?.status == "completed"
    val canStart = task?.status != "downloading" && task?.status != "queued"

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back)) }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.song_download_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.song_download_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TrackArtwork(track.coverUrl, size = 260)
                Text(
                    track.title.ifBlank { stringResource(R.string.unknown_title) },
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(track.artist.ifBlank { stringResource(R.string.unknown_artist) }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(track.duration.ifBlank { stringResource(R.string.no_duration) }) })
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.track_number, track.trackNumber)) })
                    AssistChip(onClick = {}, label = { Text(track.album.ifBlank { stringResource(R.string.single) }, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                }
            }
        }

        Button(
            onClick = { showProgress = true; DownloadService.start(context, track) },
            enabled = canStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Icon(Icons.Default.Download, null)
            Spacer(Modifier.width(8.dp))
            Text(if (isComplete) stringResource(R.string.download_again) else stringResource(R.string.download_action))
        }

        AnimatedVisibility(visible = showProgress && !isComplete, enter = fadeIn(), exit = fadeOut()) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.tertiary)
                        Text(stringResource(R.string.status_label, task?.status ?: stringResource(R.string.preparing)), style = MaterialTheme.typography.titleMedium)
                    }
                    LinearProgressIndicator(progress = { (task?.progress ?: 0) / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.extraLarge))
                    Text(stringResource(R.string.format_bitrate, task?.format ?: stringResource(R.string.format_auto), if ((task?.bitrateKbps ?: 0) > 0) stringResource(R.string.bitrate_kbps, task?.bitrateKbps ?: 0) else stringResource(R.string.bitrate_na)))
                    Text(stringResource(R.string.speed_eta, task?.speedMbps ?: 0f, if ((task?.etaSec ?: -1) >= 0) "${task?.etaSec}s" else stringResource(R.string.no_duration)), style = MaterialTheme.typography.bodySmall)
                    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium).padding(10.dp)) {
                        Text(task?.cliOutput ?: stringResource(R.string.idle_cli), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        AnimatedVisibility(visible = isComplete, enter = fadeIn()) {
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.completed_library), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
private fun TrackArtwork(coverUrl: String, size: Int) {
    if (coverUrl.isBlank()) {
        Surface(modifier = Modifier.size(size.dp), shape = MaterialTheme.shapes.extraLarge, tonalElevation = 4.dp) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(64.dp)) }
        }
    } else {
        AsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size.dp).clip(MaterialTheme.shapes.extraLarge),
        )
    }
}
