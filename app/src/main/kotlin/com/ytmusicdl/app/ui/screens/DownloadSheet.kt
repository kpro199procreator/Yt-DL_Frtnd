package com.ytmusicdl.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytmusicdl.app.data.api.ExtractorBackendProvider
import com.ytmusicdl.app.data.model.DownloadState
import com.ytmusicdl.app.data.model.Track
import com.ytmusicdl.app.service.DownloadService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSheet(track: Track, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val state by DownloadService.downloadState.collectAsState()
    val scope = rememberCoroutineScope()
    var albumTracks by remember { mutableStateOf<List<Track>>(emptyList()) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, null) }
            }
            AsyncImage(model = track.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(180.dp).clip(MaterialTheme.shapes.large))
            Spacer(Modifier.height(12.dp))
            Text(track.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = MaterialTheme.colorScheme.primary)
            if (track.album.isNotBlank()) Text(track.album, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            Spacer(Modifier.height(16.dp))

            when (val s = state) {
                is DownloadState.Idle -> Button(onClick = { DownloadService.downloadState.value = DownloadState.FetchingStream; DownloadService.start(context, track) }, modifier = Modifier.fillMaxWidth()) { Text("Descargar canción") }
                is DownloadState.FetchingStream -> Text("Obteniendo stream…")
                is DownloadState.Downloading -> Text("Descargando ${s.progress}%")
                is DownloadState.Converting -> Text("Convirtiendo audio…")
                is DownloadState.WritingTags -> Text("Escribiendo metadata…")
                is DownloadState.Done -> Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("¡Descargado!") }
                is DownloadState.Error -> Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(8.dp)); Text(s.message, color = MaterialTheme.colorScheme.error) }
            }

            if (track.album.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    scope.launch {
                        albumTracks = ExtractorBackendProvider.backend.searchSongs("${track.artist} ${track.album}", 30)
                            .filter { it.album.equals(track.album, ignoreCase = true) || it.artist.contains(track.artist, true) }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cargar canciones del álbum")
                }
            }

            if (albumTracks.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Descargar pistas del álbum", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(albumTracks, key = { it.videoId }) { item ->
                        ListItem(
                            headlineContent = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(item.artist) },
                            trailingContent = {
                                IconButton(onClick = { DownloadService.downloadState.value = DownloadState.FetchingStream; DownloadService.start(context, item) }) {
                                    Icon(Icons.Default.Download, null)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
