package com.ytmusicdl.app.ui.screens

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
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
            Column(Modifier.weight(1f)) {
                Text("Song Download", style = MaterialTheme.typography.headlineMedium)
                Text("Material 3 audio package", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    track.title.ifBlank { "Título desconocido" },
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(track.artist.ifBlank { "Unknown" }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(track.duration.ifBlank { "--:--" }) })
                    AssistChip(onClick = {}, label = { Text("#${track.trackNumber}") })
                    AssistChip(onClick = {}, label = { Text(track.album.ifBlank { "Single" }, maxLines = 1, overflow = TextOverflow.Ellipsis) })
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
            Text(if (isComplete) "Descargar de nuevo" else "Descargar")
        }

        AnimatedVisibility(visible = showProgress && !isComplete, enter = fadeIn(), exit = fadeOut()) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.tertiary)
                        Text("Estado: ${task?.status ?: "preparing"}", style = MaterialTheme.typography.titleMedium)
                    }
                    LinearProgressIndicator(progress = { (task?.progress ?: 0) / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.extraLarge))
                    Text("${task?.format ?: "auto"} · ${if ((task?.bitrateKbps ?: 0) > 0) "${task?.bitrateKbps}kbps" else "bitrate N/A"}")
                    Text("Velocidad: ${"%.2f".format(task?.speedMbps ?: 0f)} MB/s · ETA: ${if ((task?.etaSec ?: -1) >= 0) "${task?.etaSec}s" else "--"}", style = MaterialTheme.typography.bodySmall)
                    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium).padding(10.dp)) {
                        Text(task?.cliOutput ?: "[idle]", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        AnimatedVisibility(visible = isComplete, enter = fadeIn()) {
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Completado y listo en Library", color = MaterialTheme.colorScheme.onPrimaryContainer)
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
