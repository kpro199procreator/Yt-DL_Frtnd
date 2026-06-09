package com.ytmusicdl.app.ui.screens

import com.ytmusicdl.app.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytmusicdl.app.data.api.ExtractorBackendProvider
import com.ytmusicdl.app.data.model.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SearchMode { SONGS, ALBUMS, PLAYLISTS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenSong: (Track) -> Unit,
    onOpenAlbum: (Track) -> Unit,
    onBack: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onGlobalBackendError: (String) -> Unit = {},
) {
    var results by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var mode by remember { mutableStateOf(SearchMode.SONGS) }
    var showFabMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val searchErrorFallback = stringResource(R.string.search_error_fallback)
    val noResultsFor = stringResource(R.string.search_no_results_for, query)

    fun doSearch(q: String) {
        if (q.isBlank()) {
            loading = false; results = emptyList(); error = null; return
        }
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(250)
            loading = true
            error = null
            try {
                val bundle = ExtractorBackendProvider.backend.searchAll(q, 20)
                results = when (mode) {
                    SearchMode.SONGS -> bundle.songs
                    SearchMode.ALBUMS -> bundle.albums
                    SearchMode.PLAYLISTS -> bundle.playlists
                }
                if (results.isEmpty()) error = noResultsFor.replace(query, q)
            } catch (e: Exception) {
                val backendMessage = e.message ?: searchErrorFallback
                error = backendMessage
                onGlobalBackendError(backendMessage)
            } finally { loading = false }
        }
    }

    LaunchedEffect(query, mode) { doSearch(query.trim()) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back)) }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.nav_search), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.search_mode, modeLabel(mode)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            YtmusicSearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { submitted ->
                    onQueryChange(submitted)
                    doSearch(submitted)
                },
                modifier = Modifier.fillMaxWidth(),
                onPaste = { onQueryChange(clipboardManager.getText()?.text.orEmpty()) },
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == SearchMode.SONGS, onClick = { mode = SearchMode.SONGS }, label = { Text(stringResource(R.string.songs)) }, leadingIcon = { Icon(Icons.Default.MusicNote, null) })
                FilterChip(selected = mode == SearchMode.ALBUMS, onClick = { mode = SearchMode.ALBUMS }, label = { Text(stringResource(R.string.albums)) }, leadingIcon = { Icon(Icons.Default.Album, null) })
                FilterChip(selected = mode == SearchMode.PLAYLISTS, onClick = { mode = SearchMode.PLAYLISTS }, label = { Text(stringResource(R.string.playlists)) }, leadingIcon = { Icon(Icons.Default.PlaylistPlay, null) })
            }
            Spacer(Modifier.height(8.dp))
            when {
                loading -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(6) { ElevatedCard(Modifier.fillMaxWidth().height(68.dp)) {} } }
                error != null -> Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(error ?: "", color = MaterialTheme.colorScheme.error); TextButton(onClick = { doSearch(query) }) { Text(stringResource(R.string.retry)) } } }
                results.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) { Text(if (query.isBlank()) stringResource(R.string.search_empty_hint) else stringResource(R.string.no_results)) }
                else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(results, key = { it.videoId }) { track ->
                        ElevatedCard(Modifier.fillMaxWidth().clickable { if (mode == SearchMode.SONGS) onOpenSong(track) else onOpenAlbum(track) }) {
                            ListItem(
                                headlineContent = { Text(track.title.ifBlank { stringResource(R.string.unknown_title) }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = { Text(stringResource(R.string.search_track_subtitle, track.artist.ifBlank { stringResource(R.string.unknown_artist) }, track.duration.ifBlank { stringResource(R.string.no_duration) }, track.trackNumber), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingContent = {
                                    if (track.coverUrl.isBlank()) Icon(Icons.Default.MusicNote, contentDescription = null)
                                    else AsyncImage(model = track.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.medium))
                                },
                                trailingContent = { IconButton(onClick = { onOpenSong(track) }) { Icon(Icons.Default.Download, null) } },
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(visible = results.isNotEmpty()) { Text(stringResource(R.string.results_count, results.size), style = MaterialTheme.typography.labelMedium) }
        }

        Column(Modifier.align(Alignment.BottomEnd).padding(16.dp), horizontalAlignment = Alignment.End) {
            AnimatedVisibility(visible = showFabMenu) {
                Column(horizontalAlignment = Alignment.End) {
                    ExtendedFloatingActionButton(
                        onClick = { mode = SearchMode.SONGS; showFabMenu = false },
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                        text = { Text(stringResource(R.string.songs)) },
                    )
                    Spacer(Modifier.height(6.dp))
                    ExtendedFloatingActionButton(
                        onClick = { mode = SearchMode.ALBUMS; showFabMenu = false },
                        icon = { Icon(Icons.Default.Album, contentDescription = null) },
                        text = { Text(stringResource(R.string.albums)) },
                    )
                    Spacer(Modifier.height(6.dp))
                    ExtendedFloatingActionButton(
                        onClick = { mode = SearchMode.PLAYLISTS; showFabMenu = false },
                        icon = { Icon(Icons.Default.PlaylistPlay, contentDescription = null) },
                        text = { Text(stringResource(R.string.playlists)) },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            FloatingActionButton(onClick = { showFabMenu = !showFabMenu }) { Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.change_mode)) }
        }
    }
}


@Composable
private fun modeLabel(mode: SearchMode): String = when (mode) {
    SearchMode.SONGS -> stringResource(R.string.mode_songs)
    SearchMode.ALBUMS -> stringResource(R.string.mode_albums)
    SearchMode.PLAYLISTS -> stringResource(R.string.mode_playlists)
}
