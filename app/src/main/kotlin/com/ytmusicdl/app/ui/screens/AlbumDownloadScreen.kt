package com.ytmusicdl.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
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
    val queue by DownloadService.queueState.collectAsState()
    var albumTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var meta by remember { mutableStateOf("") }
    var exact by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(track.videoId) {
        loading = true
        val result = ExtractorBackendProvider.backend.getAlbumTracks(track.album.ifBlank { track.title }, track.artist)
        albumTracks = result.tracks
        exact = result.exactMatch
        meta = "${result.album.title.ifBlank { "Álbum desconocido" }} · ${result.album.year} · ${result.album.trackCount}"
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(20.dp).animateContentSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
            Text("Album Download", style = MaterialTheme.typography.headlineMedium)
        }
        Row(Modifier.fillMaxWidth()) {
            if (track.coverUrl.isBlank()) {
                Surface(modifier = Modifier.size(120.dp), shape = MaterialTheme.shapes.medium, tonalElevation = 4.dp) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null) }
                }
            } else {
                AsyncImage(model = track.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(120.dp).clip(MaterialTheme.shapes.medium))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(track.album.ifBlank { track.title.ifBlank { "Álbum desconocido" } }, style = MaterialTheme.typography.titleLarge)
                Text(track.artist.ifBlank { "Unknown" }, style = MaterialTheme.typography.bodyLarge)
                Text(track.year.ifBlank { "----" }, style = MaterialTheme.typography.bodySmall)
                Text(if (meta.isBlank()) "Cargando metadata…" else meta, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))

        Button(onClick = { albumTracks.forEach { item -> scope.launch { DownloadService.downloadState.value = DownloadState.FetchingStream; DownloadService.start(context, item) } } }, enabled = exact && albumTracks.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Descargar álbum") }
        AnimatedVisibility(visible = !exact && !loading) { Text("No exact match: revisa tracks antes de descargar", color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(10.dp))

        if (loading) {
            repeat(4) {
                ElevatedCard(Modifier.fillMaxWidth().height(58.dp)) {}
                Spacer(Modifier.height(6.dp))
            }
        } else if (albumTracks.isEmpty()) {
            Text("No se pudieron cargar tracks del álbum", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(albumTracks, key = { it.videoId }) { item ->
                    val q = queue.firstOrNull { it.videoId == item.videoId }
                    ElevatedCard(Modifier.fillMaxWidth().clickable { DownloadService.start(context, item) }) {
                        ListItem(
                            headlineContent = { Text(item.title.ifBlank { "Track sin título" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text("${q?.status ?: "waiting"} · ${q?.progress ?: 0}% · ${item.duration.ifBlank { "--:--" }}") },
                            trailingContent = {
                                IconButton(onClick = { DownloadService.downloadState.value = DownloadState.FetchingStream; DownloadService.start(context, item) }) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
