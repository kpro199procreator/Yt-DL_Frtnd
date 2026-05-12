package com.ytmusicdl.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytmusicdl.app.data.api.ExtractorBackendProvider
import com.ytmusicdl.app.data.model.DownloadState
import com.ytmusicdl.app.data.model.Track
import com.ytmusicdl.app.service.DownloadService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed class UnifiedDownloadUiState {
    data object Idle : UnifiedDownloadUiState()
    data object Fetching : UnifiedDownloadUiState()
    data class Downloading(val progress: Int, val mbDone: Float, val mbTotal: Float) : UnifiedDownloadUiState()
    data object Converting : UnifiedDownloadUiState()
    data object Tagging : UnifiedDownloadUiState()
    data class Done(val filepath: String) : UnifiedDownloadUiState()
    data class Error(val message: String, val technicalDetail: String? = null) : UnifiedDownloadUiState()
}

private fun DownloadState.toUnifiedUiState(): UnifiedDownloadUiState = when (this) {
    DownloadState.Idle -> UnifiedDownloadUiState.Idle
    DownloadState.FetchingStream -> UnifiedDownloadUiState.Fetching
    is DownloadState.Downloading -> UnifiedDownloadUiState.Downloading(progress, mbDone, mbTotal)
    DownloadState.Converting -> UnifiedDownloadUiState.Converting
    DownloadState.WritingTags -> UnifiedDownloadUiState.Tagging
    is DownloadState.Done -> UnifiedDownloadUiState.Done(filepath)
    is DownloadState.Error -> UnifiedDownloadUiState.Error(message = message, technicalDetail = message)
}

@Composable
fun DownloadScreen(track: Track, onBack: () -> Unit) {
    val context = LocalContext.current
    val state by DownloadService.downloadState.collectAsState()
    val uiState = state.toUnifiedUiState()
    val scope = rememberCoroutineScope()
    var albumTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var albumMeta by remember { mutableStateOf("") }
    var albumExactMatch by remember { mutableStateOf(false) }
    var playlistInput by remember { mutableStateOf("") }
    var playlistTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var playlistMeta by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is UnifiedDownloadUiState.Error) {
            delay(10_000)
            if (DownloadService.downloadState.value is DownloadState.Error) {
                DownloadService.downloadState.value = DownloadState.Idle
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AssistChip(onClick = {}, label = { Text("Descarga avanzada") })
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
        }
        AsyncImage(model = track.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(180.dp).clip(MaterialTheme.shapes.large))
        Spacer(Modifier.height(12.dp))
        Text(track.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(track.artist, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        if (track.album.isNotBlank()) Text(track.album, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        when (val s = uiState) {
            UnifiedDownloadUiState.Idle -> Button(onClick = { DownloadService.downloadState.value = DownloadState.FetchingStream; DownloadService.start(context, track) }, modifier = Modifier.fillMaxWidth()) { Text("Descargar canción") }
            UnifiedDownloadUiState.Fetching -> Text("Obteniendo info…", style = MaterialTheme.typography.bodyLarge)
            is UnifiedDownloadUiState.Downloading -> ExtendedProgressView(s)
            UnifiedDownloadUiState.Converting -> Text("Convirtiendo audio…", style = MaterialTheme.typography.bodyLarge)
            UnifiedDownloadUiState.Tagging -> Text("Escribiendo metadata…", style = MaterialTheme.typography.bodyLarge)
            is UnifiedDownloadUiState.Done -> DownloadSuccessView(s.filepath)
            is UnifiedDownloadUiState.Error -> DownloadErrorView(s, onRetry = {
                DownloadService.downloadState.value = DownloadState.FetchingStream
                DownloadService.start(context, track)
            }, onBack = onBack)
        }

        if (track.album.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = {
                scope.launch {
                    albumTracks = ExtractorBackendProvider.backend.searchSongs("${track.artist} ${track.album}", 20)
                    val result = ExtractorBackendProvider.backend.getAlbumTracks(track.album, track.artist)
                    albumTracks = result.tracks
                    albumExactMatch = result.exactMatch
                    albumMeta = "${result.album.title} · ${result.album.year} · ${result.album.trackCount} pistas"
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Download, null)
                Spacer(Modifier.width(8.dp))
                Text("Cargar canciones del álbum")
            }
        }

        if (albumTracks.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            if (albumMeta.isNotBlank()) Text(albumMeta, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = {
                    albumTracks.forEach { item ->
                        scope.launch { DownloadService.downloadState.value = DownloadState.FetchingStream; DownloadService.start(context, item) }
                    }
                },
                enabled = albumExactMatch,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Descargar álbum completo") }
            Spacer(Modifier.height(8.dp))
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

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = playlistInput,
            onValueChange = { playlistInput = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Playlist ID o URL") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                scope.launch {
                    val result = ExtractorBackendProvider.backend.getPlaylistTracks(playlistInput, 200)
                    playlistTracks = result.tracks
                    playlistMeta = "${result.playlist.title} · ${result.playlist.author} · ${result.playlist.trackCount} pistas"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = playlistInput.isNotBlank(),
        ) {
            Icon(Icons.Default.Download, null)
            Spacer(Modifier.width(8.dp))
            Text("Cargar playlist")
        }

        if (playlistMeta.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(playlistMeta, style = MaterialTheme.typography.bodyMedium)
        }
        if (playlistTracks.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("Descargar pistas de playlist", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(playlistTracks, key = { it.videoId }) { item ->
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

@Composable
private fun ExtendedProgressView(state: UnifiedDownloadUiState.Downloading) {
    val etaSeconds = ((state.mbTotal - state.mbDone).coerceAtLeast(0f) / 0.2f).toInt()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Descargando ${state.progress}%", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth())
            Text("Ext: m4a · Bitrate: variable", style = MaterialTheme.typography.bodyMedium)
            Text("${"%.2f".format(state.mbDone)}MB / ${"%.2f".format(state.mbTotal)}MB", style = MaterialTheme.typography.bodyMedium)
            Text("ETA: ${if (etaSeconds > 0) "~${etaSeconds}s" else "N/A"}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DownloadErrorView(state: UnifiedDownloadUiState.Error, onRetry: () -> Unit, onBack: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(state.message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.titleSmall)
            }
            state.technicalDetail?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRetry) { Text("Reintentar") }
                OutlinedButton(onClick = onBack) { Text("Cerrar") }
                IconButton(onClick = { clipboardManager.setText(AnnotatedString(state.technicalDetail ?: state.message)) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar detalle")
                }
            }
        }
    }
}

@Composable
private fun DownloadSuccessView(filePath: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("¡Descargado!", style = MaterialTheme.typography.titleMedium)
                Text(filePath, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
