package com.ytmusicdl.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.ytmusicdl.app.data.model.Track
import com.ytmusicdl.app.data.model.DownloadState
import com.ytmusicdl.app.service.DownloadService

@Composable
fun SongDownloadScreen(track: Track, onBack: () -> Unit) {
    val context = LocalContext.current
    val state by DownloadService.downloadState.collectAsState()
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
            Text("Song", style = MaterialTheme.typography.headlineLarge)
        }
        Spacer(Modifier.height(16.dp))
        AsyncImage(model = track.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(280.dp).clip(MaterialTheme.shapes.large))
        Spacer(Modifier.height(14.dp))
        Text(track.title, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(track.artist, style = MaterialTheme.typography.headlineSmall)
        if (track.album.isNotBlank()) Text(track.album, style = MaterialTheme.typography.headlineSmall)
        if (track.year.isNotBlank()) Text(track.year, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        if (state is DownloadState.Downloading) {
            val s = state as DownloadState.Downloading
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Descargando ${s.progress}%")
                    LinearProgressIndicator(progress = { s.progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("${"%.2f".format(s.mbDone)}MB / ${"%.2f".format(s.mbTotal)}MB")
                }
            }
        }
        Spacer(Modifier.height(26.dp))
        Button(
            onClick = { DownloadService.downloadState.value = DownloadState.FetchingStream; DownloadService.start(context, track) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("Descargar cancion", style = MaterialTheme.typography.headlineSmall) }
    }
}
