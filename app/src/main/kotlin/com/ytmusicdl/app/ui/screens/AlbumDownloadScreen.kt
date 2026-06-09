package com.ytmusicdl.app.ui.screens

import com.ytmusicdl.app.R

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
import androidx.compose.ui.res.stringResource
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
    val unknownAlbum = stringResource(R.string.unknown_album)
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
        if (track.album.equals("Playlist", ignoreCase = true)) {
            val result = ExtractorBackendProvider.backend.getPlaylistTracks(track.videoId, 200)
            playlistTracks = result.tracks
            playlistMeta = context.getString(R.string.playlist_meta_format, result.playlist.title.ifBlank { track.title }, result.playlist.author, result.playlist.trackCount)
            albumTracks = emptyList()
            exact = false
            meta = playlistMeta
        } else {
            val result = ExtractorBackendProvider.backend.getAlbumTracks(track.album.ifBlank { track.title }, track.artist)
            albumTracks = result.tracks
            exact = result.exactMatch
            meta = context.getString(R.string.album_meta_format, result.album.title.ifBlank { unknownAlbum }, result.album.year, result.album.trackCount)
            playlistTracks = emptyList()
            playlistMeta = ""
        }
        loading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).animateContentSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back)) }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.album_download_title), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.album_download_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AlbumArtwork(track.coverUrl)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(track.album.ifBlank { track.title.ifBlank { stringResource(R.string.unknown_album) } }, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(track.artist.ifBlank { stringResource(R.string.unknown_artist) }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = {}, label = { Text(track.year.ifBlank { stringResource(R.string.unknown_year) }) }, leadingIcon = { Icon(Icons.Default.Album, null) })
                            AssistChip(onClick = {}, label = { Text(if (meta.isBlank()) stringResource(R.string.metadata_loading) else meta, maxLines = 1, overflow = TextOverflow.Ellipsis) })
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
                Text(stringResource(R.string.download_album))
            }
            AnimatedVisibility(visible = !exact && !loading, enter = fadeIn()) {
                Text(stringResource(R.string.no_exact_match), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
        }

        if (loading) {
            items(3) { ElevatedCard(Modifier.fillMaxWidth().height(66.dp), shape = MaterialTheme.shapes.large) {} }
        } else {
            items(albumTracks, key = { it.videoId }) { item ->
                val q = queue.firstOrNull { it.videoId == item.videoId }
                ElevatedCard(Modifier.fillMaxWidth().clickable { DownloadService.start(context, item) }, shape = MaterialTheme.shapes.large) {
                    ListItem(
                        headlineContent = { Text(item.title.ifBlank { stringResource(R.string.track_without_title) }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(stringResource(R.string.status_progress, q?.status ?: stringResource(R.string.waiting), q?.progress ?: 0), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                        trailingContent = { IconButton(onClick = { DownloadService.start(context, item) }) { Icon(Icons.Default.Download, contentDescription = null) } },
                    )
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.playlist_title), style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = playlistUrl,
                        onValueChange = { playlistUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.playlist_url_label)) },
                        shape = MaterialTheme.shapes.extraLarge,
                        leadingIcon = { Icon(Icons.Default.PlaylistPlay, contentDescription = null) },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                val r = ExtractorBackendProvider.backend.getPlaylistTracks(playlistUrl, 200)
                                playlistTracks = r.tracks
                                playlistMeta = context.getString(R.string.playlist_meta_format, r.playlist.title, r.playlist.author, r.playlist.trackCount)
                            }
                        }, enabled = playlistUrl.isNotBlank(), shape = MaterialTheme.shapes.extraLarge) { Text(stringResource(R.string.load_playlist)) }
                        Button(onClick = { playlistTracks.forEach { item -> scope.launch { DownloadService.start(context, item) } } }, enabled = playlistTracks.isNotEmpty(), shape = MaterialTheme.shapes.extraLarge) { Text(stringResource(R.string.download_playlist)) }
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
