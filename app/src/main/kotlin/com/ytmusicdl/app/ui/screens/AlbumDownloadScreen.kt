package com.ytmusicdl.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytmusicdl.app.data.api.ExtractorBackendProvider
import com.ytmusicdl.app.data.model.DownloadState
import com.ytmusicdl.app.data.model.Track
import com.ytmusicdl.app.service.DownloadService
import kotlinx.coroutines.launch

@Composable
fun AlbumDownloadScreen(track: Track, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var albumTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var meta by remember { mutableStateOf("") }
    var exact by remember { mutableStateOf(false) }

    LaunchedEffect(track.videoId) {
        val result = ExtractorBackendProvider.backend.getAlbumTracks(track.album.ifBlank { track.title }, track.artist)
        albumTracks = result.tracks
        exact = result.exactMatch
        meta = "${result.album.title} · ${result.album.year} · ${result.album.trackCount}"
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
            Text("Album", style = MaterialTheme.typography.headlineLarge)
        }
        Row(Modifier.fillMaxWidth()) {
            AsyncImage(model = track.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(170.dp).clip(MaterialTheme.shapes.medium))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(track.album.ifBlank { track.title }, style = MaterialTheme.typography.headlineSmall)
                Text(track.artist, style = MaterialTheme.typography.titleLarge)
                Text(track.year, style = MaterialTheme.typography.titleLarge)
                Text(meta, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                albumTracks.forEach { item -> scope.launch { DownloadService.downloadState.value = DownloadState.FetchingStream; DownloadService.start(context, item) } }
            },
            enabled = exact,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Descargar Album") }
        Spacer(Modifier.height(10.dp))
        Text("Track list", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(albumTracks, key = { it.videoId }) { item ->
                ListItem(
                    headlineContent = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingContent = {
                        IconButton(onClick = { DownloadService.downloadState.value = DownloadState.FetchingStream; DownloadService.start(context, item) }) {
                            Icon(Icons.Default.Download, contentDescription = null)
                        }
                    }
                )
            }
        }
    }
}

