package com.ytmusicdl.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytmusicdl.app.data.api.ExtractorBackendProvider
import com.ytmusicdl.app.data.model.Track
import com.ytmusicdl.app.service.DownloadService
import kotlinx.coroutines.launch

@Composable
fun AlbumDownloadScreen(track: Track, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val queue by DownloadService.queueState.collectAsState()
    var albumTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var meta by remember { mutableStateOf("") }
    var exact by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var playlistUrl by remember { mutableStateOf("") }
    var playlistTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var playlistMeta by remember { mutableStateOf("") }

    LaunchedEffect(track.videoId) {
        loading = true
        val result = ExtractorBackendProvider.backend.getAlbumTracks(track.album.ifBlank { track.title }, track.artist)
        albumTracks = result.tracks
        exact = result.exactMatch
        meta = "${result.album.title.ifBlank { "Álbum desconocido" }} · ${result.album.year} · ${result.album.trackCount} tracks"
        loading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).animateContentSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
                Column(Modifier.weight(1f)) {
                    Text("Album Download", style = MaterialTheme.typography.headlineMedium)
                    Text("Álbumes y playlists con estilo Material 3", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AlbumArtwork(track.coverUrl)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(track.album.ifBlank { track.title.ifBlank { "Álbum desconocido" } }, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(track.artist.ifBlank { "Unknown" }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = {}, label = { Text(track.year.ifBlank { "----" }) }, leadingIcon = { Icon(Icons.Default.Album, null) })
                            AssistChip(onClick = {}, label = { Text(if (meta.isBlank()) "Metadata…" else meta, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { albumTracks.forEach { item -> scope.launch { DownloadService.start(context, item) } } },
                enabled = exact && albumTracks.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Icon(Icons.Default.LibraryMusic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Descargar álbum")
            }
            AnimatedVisibility(visible = !exact && !loading, enter = fadeIn()) {
                Text("No exact match: revisa tracks antes de descargar", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
        }

        if (loading) {
            items(3) { ElevatedCard(Modifier.fillMaxWidth().height(66.dp), shape = MaterialTheme.shapes.large) {} }
        } else {
            items(albumTracks, key = { it.videoId }) { item ->
                val q = queue.firstOrNull { it.videoId == item.videoId }
                ElevatedCard(Modifier.fillMaxWidth().clickable { DownloadService.start(context, item) }, shape = MaterialTheme.shapes.large) {
                    ListItem(
                        headlineContent = { Text(item.title.ifBlank { "Track sin título" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text("${q?.status ?: "waiting"} · ${q?.progress ?: 0}%", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                        trailingContent = { IconButton(onClick = { DownloadService.start(context, item) }) { Icon(Icons.Default.Download, contentDescription = null) } },
                    )
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Playlist", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = playlistUrl,
                        onValueChange = { playlistUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Playlist URL / ID") },
                        shape = MaterialTheme.shapes.extraLarge,
                        leadingIcon = { Icon(Icons.Default.PlaylistPlay, contentDescription = null) },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                val r = ExtractorBackendProvider.backend.getPlaylistTracks(playlistUrl, 200)
                                playlistTracks = r.tracks
                                playlistMeta = "${r.playlist.title} · ${r.playlist.author} · ${r.playlist.trackCount} tracks"
                            }
                        }, enabled = playlistUrl.isNotBlank(), shape = MaterialTheme.shapes.extraLarge) { Text("Cargar") }
                        Button(onClick = { playlistTracks.forEach { item -> scope.launch { DownloadService.start(context, item) } } }, enabled = playlistTracks.isNotEmpty(), shape = MaterialTheme.shapes.extraLarge) { Text("Descargar") }
                    }
                    if (playlistMeta.isNotBlank()) Text(playlistMeta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AlbumArtwork(coverUrl: String) {
    if (coverUrl.isBlank()) {
        Surface(modifier = Modifier.size(148.dp), shape = MaterialTheme.shapes.extraLarge, tonalElevation = 4.dp) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(52.dp)) }
        }
    } else {
        AsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(148.dp).clip(MaterialTheme.shapes.extraLarge),
        )
    }
}
