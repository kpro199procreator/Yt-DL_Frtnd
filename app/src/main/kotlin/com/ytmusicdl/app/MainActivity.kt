package com.ytmusicdl.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ytmusicdl.app.data.api.Track
import com.ytmusicdl.app.data.repository.MusicRepository
import com.ytmusicdl.app.ui.screens.*
import com.ytmusicdl.app.ui.theme.YtmusicdlTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = MusicRepository(applicationContext)

        setContent {
            YtmusicdlTheme {
                YtmusicdlApp(repository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YtmusicdlApp(repository: MusicRepository) {
    val scope         = rememberCoroutineScope()
    var connectState  by remember { mutableStateOf(ConnectState.CONNECTING) }
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    // Intentar conectar al arrancar
    LaunchedEffect(Unit) {
        if (repository.isServerRunning()) {
            connectState = ConnectState.CONNECTED
        } else {
            connectState = ConnectState.STARTING
            repository.startServer()
            val ok = repository.waitForServer()
            connectState = if (ok) ConnectState.CONNECTED else ConnectState.TIMEOUT
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ytmusicdl") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    ) { padding ->

        when (connectState) {
            ConnectState.CONNECTED -> {
                SearchScreen(
                    repository  = repository,
                    onDownload  = { track -> selectedTrack = track },
                )
            }
            else -> {
                ConnectScreen(
                    state   = connectState,
                    onRetry = {
                        scope.launch {
                            connectState = ConnectState.CONNECTING
                            if (repository.isServerRunning()) {
                                connectState = ConnectState.CONNECTED
                            } else {
                                connectState = ConnectState.STARTING
                                repository.startServer()
                                connectState = if (repository.waitForServer())
                                    ConnectState.CONNECTED else ConnectState.TIMEOUT
                            }
                        }
                    }
                )
            }
        }

        // Bottom sheet de descarga
        selectedTrack?.let { track ->
            DownloadSheet(
                track      = track,
                repository = repository,
                onDismiss  = { selectedTrack = null },
            )
        }
    }
}
