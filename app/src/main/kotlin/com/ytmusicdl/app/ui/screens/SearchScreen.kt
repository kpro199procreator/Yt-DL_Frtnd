package com.ytmusicdl.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onDownload: (Track) -> Unit,
    onBack: () -> Unit,
    initialQuery: String = "",
    onGlobalBackendError: (String) -> Unit = {},
) {
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun doSearch(q: String) {
        if (q.isBlank()) return
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(250)
            loading = true
            error = null
            try {
                results = ExtractorBackendProvider.backend.searchSongs(q, 20)
                if (results.isEmpty()) error = "Sin resultados para \"$q\""
            } catch (e: Exception) {
                val backendMessage = e.message ?: "Error de búsqueda"
                error = backendMessage
                onGlobalBackendError(backendMessage)
            } finally {
                loading = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
        Text("Buscar música", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        SearchBar(
            query = query,
            onQueryChange = { query = it },
            onSearch = ::doSearch,
            active = false,
            onActiveChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar en YouTube Music…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton({ query = ""; results = emptyList(); error = null }) { Icon(Icons.Default.Close, null) }
            },
        ) {}
        Spacer(Modifier.height(8.dp))

        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            error != null -> Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
            }
            results.isEmpty() && query.isNotEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                Text("Sin resultados", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
            else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(results, key = { it.videoId }, contentType = { "track" }) { track ->
                    ListItem(
                        headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium) },
                        supportingContent = {
                            Text(buildString {
                                append(track.artist)
                                if (track.album.isNotEmpty()) append(" · ${track.album}")
                                if (track.duration.isNotEmpty()) append(" · ${track.duration}")
                            }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        },
                        leadingContent = {
                            AsyncImage(model = track.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp).clip(MaterialTheme.shapes.small))
                        },
                        trailingContent = { IconButton(onClick = { onDownload(track) }) { Icon(Icons.Default.Download, null) } },
                        modifier = Modifier.clickable { onDownload(track) },
                    )
                }
            }
        }
    }
}
