package com.ytmusicdl.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytmusicdl.app.data.model.DownloadState
import com.ytmusicdl.app.data.model.Track
import com.ytmusicdl.app.service.DownloadService

@Composable
fun SongDownloadScreen(track: Track, onBack: () -> Unit) {
    val context = LocalContext.current
    val state by DownloadService.downloadState.collectAsState()
    val queue by DownloadService.queueState.collectAsState()
    val task = queue.firstOrNull { it.videoId == track.videoId }

    Column(Modifier.fillMaxSize().padding(20.dp).animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
            Text("Song Download", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(12.dp))
        if (track.coverUrl.isBlank()) {
            Surface(modifier = Modifier.size(220.dp), shape = MaterialTheme.shapes.large, tonalElevation = 4.dp) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(56.dp)) }
            }
        } else {
            AsyncImage(model = track.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(220.dp).clip(MaterialTheme.shapes.large))
        }
        Spacer(Modifier.height(14.dp))
        Text(track.title.ifBlank { "Título desconocido" }, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(track.artist.ifBlank { "Unknown artist" }, style = MaterialTheme.typography.bodyLarge)
        Text("${track.album.ifBlank { "Álbum desconocido" }} · ${track.duration.ifBlank { "--:--" }}", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(12.dp))
        Crossfade(targetState = state, label = "download-state") { current ->
            when (current) {
                DownloadState.FetchingStream -> AssistChip(onClick = {}, label = { Text("Analizando fuente…") })
                DownloadState.Converting -> AssistChip(onClick = {}, label = { Text("Convirtiendo audio…") })
                DownloadState.WritingTags -> AssistChip(onClick = {}, label = { Text("Escribiendo metadata…") })
                is DownloadState.Downloading -> {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Progreso ${current.progress}%")
                            LinearProgressIndicator(progress = { current.progress / 100f }, modifier = Modifier.fillMaxWidth())
                            Text("${"%.2f".format(current.mbDone)}MB / ${"%.2f".format(current.mbTotal)}MB")
                            Text("Velocidad: -- MB/s · ETA: --:--", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                is DownloadState.Error -> Text(current.message, color = MaterialTheme.colorScheme.error)
                is DownloadState.Done -> Text("Completado", color = MaterialTheme.colorScheme.primary)
                DownloadState.Idle -> {}
            }
        }

        AnimatedVisibility(visible = task != null) {
            Text("Estado cola: ${task?.status ?: "-"} · ${task?.progress ?: 0}%", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        Button(onClick = { DownloadService.downloadState.value = DownloadState.FetchingStream; DownloadService.start(context, track) }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Icon(Icons.Default.Download, null)
            Spacer(Modifier.width(8.dp))
            Text("Descargar")
        }
    }
}
