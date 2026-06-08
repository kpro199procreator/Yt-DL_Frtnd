package com.ytmusicdl.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
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
    initialQuery: String = "",
    onGlobalBackendError: (String) -> Unit = {},
) {
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var mode by remember { mutableStateOf(SearchMode.SONGS) }
    var showFabMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                if (results.isEmpty()) error = "Sin resultados para \"$q\""
            } catch (e: Exception) {
                val backendMessage = e.message ?: "Error de búsqueda"
                error = backendMessage
                onGlobalBackendError(backendMessage)
            } finally { loading = false }
        }
    }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && initialQuery != query) query = initialQuery
    }

    LaunchedEffect(query, mode) { doSearch(query.trim()) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
            Text("Search", style = MaterialTheme.typography.headlineMedium)
            Text("Modo: ${mode.name.lowercase()}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            SearchBar(query = query, onQueryChange = { query = it }, onSearch = ::doSearch, active = false, onActiveChange = {}, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Escribe para buscar…") }, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Default.Close, null) } }) {}
            Spacer(Modifier.height(8.dp))
            when {
                loading -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(6) { ElevatedCard(Modifier.fillMaxWidth().height(68.dp)) {} } }
                error != null -> Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(error ?: "", color = MaterialTheme.colorScheme.error); TextButton(onClick = { doSearch(query) }) { Text("Retry") } } }
                results.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) { Text(if (query.isBlank()) "Busca o pega una URL para comenzar" else "Sin resultados") }
                else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(results, key = { it.videoId }) { track ->
                        ElevatedCard(Modifier.fillMaxWidth().clickable { if (mode == SearchMode.SONGS) onOpenSong(track) else onOpenAlbum(track) }) {
                            ListItem(
                                headlineContent = { Text(track.title.ifBlank { "Título desconocido" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = { Text("${track.artist.ifBlank { "Unknown" }} · ${track.duration.ifBlank { "--:--" }} · #${track.trackNumber}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingContent = {
                                    if (track.coverUrl.isBlank()) Icon(Icons.Default.MusicNote, contentDescription = null)
                                    else AsyncImage(model = track.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp).clip(MaterialTheme.shapes.small))
                                },
                                trailingContent = { IconButton(onClick = { onOpenSong(track) }) { Icon(Icons.Default.Download, null) } },
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(visible = results.isNotEmpty()) { Text("${results.size} resultados", style = MaterialTheme.typography.labelMedium) }
        }

        Column(Modifier.align(Alignment.BottomEnd).padding(16.dp), horizontalAlignment = Alignment.End) {
            AnimatedVisibility(visible = showFabMenu) {
                Column(horizontalAlignment = Alignment.End) {
                    SmallFloatingActionButton(onClick = { mode = SearchMode.SONGS; showFabMenu = false }) { Text("Songs") }
                    Spacer(Modifier.height(6.dp))
                    SmallFloatingActionButton(onClick = { mode = SearchMode.ALBUMS; showFabMenu = false }) { Text("Albums") }
                    Spacer(Modifier.height(6.dp))
                    SmallFloatingActionButton(onClick = { mode = SearchMode.PLAYLISTS; showFabMenu = false }) { Text("Playlists") }
                    Spacer(Modifier.height(10.dp))
                }
            }
            FloatingActionButton(onClick = { showFabMenu = !showFabMenu }) { Icon(Icons.Default.Search, null) }
        }
    }
}
