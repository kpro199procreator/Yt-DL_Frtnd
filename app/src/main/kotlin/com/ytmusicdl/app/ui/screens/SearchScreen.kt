package com.ytmusicdl.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytmusicdl.app.data.api.NewPipeService
import com.ytmusicdl.app.data.model.Track
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onDownload: (Track) -> Unit) {
    var query     by remember { mutableStateOf("") }
    var results   by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loading   by remember { mutableStateOf(false) }
    var error     by remember { mutableStateOf<String?>(null) }
    val scope     = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {

        SearchBar(
            query          = query,
            onQueryChange  = { query = it },
            onSearch       = { q ->
                if (q.isNotBlank()) {
                    loading = true; error = null
                    scope.launch {
                        try {
                            results = NewPipeService.searchSongs(q)
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            loading = false
                        }
                    }
                }
            },
            active         = false,
            onActiveChange = {},
            modifier       = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder    = { Text("Buscar en YouTube Music…") },
            leadingIcon    = { Icon(Icons.Default.Search, null) },
            trailingIcon   = {
                if (query.isNotEmpty()) IconButton({ query = ""; results = emptyList() }) {
                    Icon(Icons.Default.Close, null)
                }
            },
        ) {}

        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            results.isEmpty() && query.isNotEmpty() -> Box(
                Modifier.fillMaxSize().padding(32.dp), Alignment.Center
            ) {
                Text("Sin resultados", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(results, key = { it.videoId }) { track ->
                    ListItem(
                        headlineContent  = {
                            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = {
                            Text(
                                buildString {
                                    append(track.artist)
                                    if (track.album.isNotEmpty()) append(" · ${track.album}")
                                    if (track.duration.isNotEmpty()) append(" · ${track.duration}")
                                },
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                            )
                        },
                        leadingContent = {
                            AsyncImage(
                                model = track.coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(52.dp)
                                    .clip(MaterialTheme.shapes.small),
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { onDownload(track) }) {
                                Icon(Icons.Default.Download, null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.clickable { onDownload(track) },
                    )
                }
            }
        }
    }
}
