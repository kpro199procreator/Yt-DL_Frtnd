package com.ytmusicdl.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytmusicdl.app.data.api.ProgressResponse
import com.ytmusicdl.app.data.api.Track
import com.ytmusicdl.app.data.repository.MusicRepository
import kotlinx.coroutines.launch

/**
 * Bottom sheet de descarga — se abre al pulsar descargar en un track.
 * Muestra carátula grande + info + barra de progreso real.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSheet(
    track: Track,
    repository: MusicRepository,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope      = rememberCoroutineScope()

    var downloadState by remember { mutableStateOf<DownloadUiState>(DownloadUiState.Idle) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // Carátula grande
            AsyncImage(
                model             = repository.coverUrl(track.cover_url),
                contentDescription= track.title,
                contentScale      = ContentScale.Crop,
                modifier          = Modifier
                    .size(180.dp)
                    .clip(MaterialTheme.shapes.large),
            )

            Spacer(Modifier.height(16.dp))

            // Info
            Text(
                text     = track.title,
                style    = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text  = track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (track.album.isNotEmpty()) {
                Text(
                    text  = track.album,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                )
            }

            Spacer(Modifier.height(24.dp))

            // Estado de descarga
            when (val state = downloadState) {
                is DownloadUiState.Idle -> {
                    Button(
                        onClick = {
                            scope.launch {
                                downloadState = DownloadUiState.Starting
                                try {
                                    val resp = repository.download(track)
                                    when {
                                        resp.status == "skipped" ->
                                            downloadState = DownloadUiState.Done("Ya descargado")
                                        resp.job_id != null -> {
                                            repository.waitForDownload(resp.job_id) { prog ->
                                                downloadState = DownloadUiState.Downloading(prog)
                                            }
                                            downloadState = DownloadUiState.Done("Descarga completa ✓")
                                        }
                                        else ->
                                            downloadState = DownloadUiState.Error("Error desconocido")
                                    }
                                } catch (e: Exception) {
                                    downloadState = DownloadUiState.Error(e.message ?: "Error")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Descargar")
                    }
                }

                is DownloadUiState.Starting -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("Iniciando…", style = MaterialTheme.typography.bodySmall)
                }

                is DownloadUiState.Downloading -> {
                    val prog = state.progress
                    LinearProgressIndicator(
                        progress    = { prog.progress / 100f },
                        modifier    = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${prog.progress}%",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "${prog.mb_done} / ${prog.mb_total} MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                        )
                    }
                }

                is DownloadUiState.Done -> {
                    Icon(
                        imageVector        = androidx.compose.material.icons.Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(state.message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Cerrar")
                    }
                }

                is DownloadUiState.Error -> {
                    Text(
                        "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { downloadState = DownloadUiState.Idle },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}

sealed class DownloadUiState {
    object Idle                                    : DownloadUiState()
    object Starting                                : DownloadUiState()
    data class Downloading(val progress: ProgressResponse) : DownloadUiState()
    data class Done(val message: String)           : DownloadUiState()
    data class Error(val message: String)          : DownloadUiState()
}
