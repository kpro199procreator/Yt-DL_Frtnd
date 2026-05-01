package com.ytmusicdl.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
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
import com.ytmusicdl.app.data.model.AudioFormatOption
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
    var showFormatSelector by remember { mutableStateOf(false) }
    var isLoadingFormats by remember { mutableStateOf(false) }
    var formatsError by remember { mutableStateOf<String?>(null) }
    var audioFormats by remember { mutableStateOf<List<AudioFormatOption>>(emptyList()) }

    LaunchedEffect(state, track.videoId) {
        val error = (state as? DownloadState.Error)?.message.orEmpty()
        val hasUnavailableFormatError = error.contains("Requested format is not available", ignoreCase = true) ||
            error.contains("format is not available", ignoreCase = true)
        if (!hasUnavailableFormatError || isLoadingFormats || showFormatSelector) return@LaunchedEffect

        isLoadingFormats = true
        showFormatSelector = true
        formatsError = null
        audioFormats = emptyList()
        runCatching {
            ExtractorBackendProvider.backend.listAudioFormats(track.videoId)
        }.onSuccess { formats ->
            audioFormats = formats
        }.onFailure { throwable ->
            formatsError = throwable.message ?: "No se pudieron listar formatos"
        }
        isLoadingFormats = false
    }

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
                is DownloadState.Idle -> Button(onClick = { DownloadService.downloadState.value = DownloadState.FetchingStream; DownloadService.start(context, track, null) }, modifier = Modifier.fillMaxWidth()) { Text("Descargar canción") }
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
                                IconButton(onClick = { DownloadService.downloadState.value = DownloadState.FetchingStream; DownloadService.start(context, item, null) }) {
                                    Icon(Icons.Default.Download, null)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFormatSelector) {
        AlertDialog(
            onDismissRequest = { showFormatSelector = false },
            title = { Text("Formato no disponible") },
            text = {
                when {
                    isLoadingFormats -> Text("Cargando formatos disponibles…")
                    formatsError != null -> Text("Fallo al listar formatos: ${formatsError}")
                    audioFormats.isEmpty() -> Text("No hay formatos alternativos para esta pista.")
                    else -> LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                        items(audioFormats, key = { it.formatId }) { format ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        "${format.formatId} • ${format.ext} • ${format.abr} kbps • ${
                                            format.note.ifBlank { "sin nota" }
                                        }"
                                    )
                                },
                                supportingContent = {
                                    Text("${format.acodec} • ${format.protocol}")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showFormatSelector = false
                                        DownloadService.downloadState.value = DownloadState.FetchingStream
                                        DownloadService.start(context, track, format.formatId)
                                    },
                                tonalElevation = 0.dp
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFormatSelector = false }) { Text("Cerrar") }
            },
            dismissButton = {
                if (audioFormats.isNotEmpty() && !isLoadingFormats && formatsError == null) {
                    TextButton(onClick = { showFormatSelector = false }) { Text("Cancelar") }
                }
            }
        )
    }
}
