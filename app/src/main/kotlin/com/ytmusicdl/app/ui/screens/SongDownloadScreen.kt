package com.ytmusicdl.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
        Spacer(Modifier.height(10.dp))
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(track.title.ifBlank { "Título desconocido" }, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Artista(s): ${track.artist.ifBlank { "Unknown" }}")
                Text("Duración: ${track.duration.ifBlank { "--:--" }} · BPM: N/A · #${track.trackNumber}")
            }
        }

        Spacer(Modifier.height(16.dp))
        val canStart = task?.status != "downloading" && task?.status != "queued"
        Button(onClick = { showProgress = true; DownloadService.start(context, track) }, enabled = canStart, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("Descargar")
        }

        AnimatedVisibility(visible = showProgress && (task == null || task.status != "done"), enter = fadeIn(), exit = fadeOut()) {
            ElevatedCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Estado: ${task?.status ?: "preparing"}")
                    LinearProgressIndicator(progress = { (task?.progress ?: 0) / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("${task?.format ?: "auto"} · ${if ((task?.bitrateKbps ?: 0) > 0) "${task?.bitrateKbps}kbps" else "bitrate N/A"}")
                    Text("Velocidad: ${"%.2f".format(task?.speedMbps ?: 0f)} MB/s · ETA: ${if ((task?.etaSec ?: -1) >= 0) "${task?.etaSec}s" else "--"}", style = MaterialTheme.typography.bodySmall)
                    Box(Modifier.fillMaxWidth().background(Color.Black, MaterialTheme.shapes.small).padding(8.dp)) {
                        Text(task?.cliOutput ?: "[idle]", color = Color(0xFF8CFF8C), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        AnimatedVisibility(visible = task?.status == "done", enter = fadeIn()) {
            ElevatedCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Completado")
                }
            }
        }
    }
}
