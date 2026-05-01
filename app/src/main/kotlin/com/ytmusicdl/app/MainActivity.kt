package com.ytmusicdl.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ytmusicdl.app.data.api.NewPipeService
import com.ytmusicdl.app.data.api.PythonBridge
import com.ytmusicdl.app.data.model.Track
import com.ytmusicdl.app.ui.screens.DownloadSheet
import com.ytmusicdl.app.ui.screens.SearchScreen
import com.ytmusicdl.app.ui.theme.YtmusicdlTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Inicializar NewPipe Extractor con el downloader HTTP
        NewPipeService.init()
        PythonBridge.initialize(this)

        setContent {
            YtmusicdlTheme {
                App()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val pythonReady = remember { PythonBridge.isReady() }
    val pythonError = remember { PythonBridge.getInitError() }
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("ytmusicdl") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column {
                if (!pythonReady) {
                    Text(
                        text = pythonError ?: "Runtime de Python no disponible; usando fallback NewPipe.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    )
                }
                SearchScreen(onDownload = { selectedTrack = it })
            }
        }

        selectedTrack?.let { track ->
            DownloadSheet(
                track     = track,
                onDismiss = { selectedTrack = null },
            )
        }
    }
}
