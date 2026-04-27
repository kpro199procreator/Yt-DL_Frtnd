package com.ytmusicdl.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytmusicdl.app.data.api.Track
import com.ytmusicdl.app.data.repository.MusicRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    repository: MusicRepository,
    onDownload: (Track) -> Unit,
) {
    var query       by remember { mutableStateOf("") }
    var searchType  by remember { mutableStateOf("song") }   // song | album
    var results     by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {

        // ── SearchBar M3 ──────────────────────────────────────────────────────
        SearchBar(
            query            = query,
            onQueryChange    = { query = it },
            onSearch         = { q ->
                if (q.isNotBlank()) {
                    isLoading = true
                    errorMsg  = null
                    scope.launch {
    try {
        val data = if (searchType == "album")
            repository.searchAlbums(q)
        else
            repository.searchSongs(q)

        results = data
    } catch (e: Exception) {
        errorMsg = "Error: ${e.message}"
    } finally {
        isLoading = false
    }
}
                }
            },
            active           = false,
            onActiveChange   = {},
            modifier         = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder      = { Text("Buscar canciones o álbumes…") },
            leadingIcon      = { Icon(Icons.Default.Search, null) },
            trailingIcon     = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; results = emptyList() }) {
                        Icon(Icons.Default.Close, "Limpiar")
                    }
                }
            },
        ) {}

        // ── Tipo de búsqueda ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = searchType == "song",
                onClick  = { searchType = "song" },
                label    = { Text("Canciones") },
                leadingIcon = if (searchType == "song") {
                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
               } else null,
            )
            FilterChip(
                selected = searchType == "album",
                onClick  = { searchType = "album" },
                label    = { Text("Álbumes") },
                leadingIcon = if (searchType == "album") {{
                    Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                }} else null,
            )
        }

        // ── Resultados ────────────────────────────────────────────────────────
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMsg != null -> {
                Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                }
            }
            results.isEmpty() && query.isNotEmpty() && !isLoading -> {
                Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                    Text("Sin resultados", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            }
            else -> {
                LazyColumn(
                    contentPadding        = PaddingValues(vertical = 8.dp),
                    verticalArrangement   = Arrangement.spacedBy(2.dp),
                ) {
                    items(results, key = { it.video_id.ifEmpty { it.title } }) { track ->
                        TrackRow(
                            track       = track,
                            repository  = repository,
                            onDownload  = { onDownload(track) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackRow(
    track: Track,
    repository: MusicRepository,
    onDownload: () -> Unit,
) {
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurface.copy(0.6f),
            )
        },
        leadingContent = {
            // Carátula con Coil
            AsyncImage(
                model             = repository.coverUrl(track.cover_url),
                contentDescription= track.title,
                contentScale      = ContentScale.Crop,
                modifier          = Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.small),
            )
        },
        trailingContent = {
            IconButton(onClick = onDownload) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Descargar",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        modifier = Modifier.clickable { onDownload() },
    )
}
