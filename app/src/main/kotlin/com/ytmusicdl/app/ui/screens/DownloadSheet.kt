package com.ytmusicdl.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.ytmusicdl.app.data.model.DownloadState
import com.ytmusicdl.app.data.model.Track
import com.ytmusicdl.app.service.DownloadService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSheet(track: Track, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val state   by DownloadService.downloadState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier            = Modifier.fillMaxWidth()
                .padding(horizontal = 24.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Carátula grande
            AsyncImage(
                model             = track.coverUrl,
                contentDescription = null,
                contentScale      = ContentScale.Crop,
                modifier          = Modifier.size(180.dp)
                    .clip(MaterialTheme.shapes.large),
            )
            Spacer(Modifier.height(16.dp))

            Text(track.title, style = MaterialTheme.typography.titleLarge,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary)
            if (track.album.isNotEmpty())
                Text(track.album, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f))

            Spacer(Modifier.height(24.dp))

            when (val s = state) {
                is DownloadState.Idle -> {
                    Button(
                        onClick  = {
                            DownloadService.downloadState.value = DownloadState.FetchingStream
                            DownloadService.start(context, track)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Descargar") }
                }
                is DownloadState.FetchingStream -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("Obteniendo stream de audio…",
                        style = MaterialTheme.typography.bodySmall)
                }
                is DownloadState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { s.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${s.progress}%", style = MaterialTheme.typography.bodySmall)
                        Text("${s.mbDone} / ${s.mbTotal} MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                }
                is DownloadState.Converting -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("Convirtiendo a m4a…", style = MaterialTheme.typography.bodySmall)
                }
                is DownloadState.WritingTags -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("Escribiendo metadata y letras…",
                        style = MaterialTheme.typography.bodySmall)
                }
                is DownloadState.Done -> {
                    Icon(Icons.Default.CheckCircle, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("¡Descargado!", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = {
                        DownloadService.downloadState.value = DownloadState.Idle
                        onDismiss()
                    }, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
                }
                is DownloadState.Error -> {
                    Icon(Icons.Default.Error, null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = {
                        DownloadService.downloadState.value = DownloadState.Idle
                    }, modifier = Modifier.fillMaxWidth()) { Text("Reintentar") }
                }
            }
        }
    }
}
