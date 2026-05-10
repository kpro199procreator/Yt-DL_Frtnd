package com.ytmusicdl.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ytmusicdl.app.data.api.PythonBridge
import com.ytmusicdl.app.data.model.Track
import com.ytmusicdl.app.ui.screens.DownloadScreen
import com.ytmusicdl.app.ui.screens.DownloadsHistoryScreen
import com.ytmusicdl.app.ui.screens.HomeScreen
import com.ytmusicdl.app.ui.screens.SearchScreen
import com.ytmusicdl.app.ui.theme.YtmusicdlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        PythonBridge.initialize(this)
        setContent { YtmusicdlTheme { App(PythonBridge.getInitError()) } }
    }
}

private enum class AppTab { HOME, SEARCH, DOWNLOADS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(pythonError: String? = null) {
    RequestStartupPermissions()
    var showPythonError by remember(pythonError) { mutableStateOf(!pythonError.isNullOrBlank()) }
    var globalBackendError by remember { mutableStateOf<String?>(null) }
    var selectedTrack by remember { mutableStateOf<Track?>(null) }
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var seedQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ytmusicdl") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == AppTab.HOME, onClick = { tab = AppTab.HOME; selectedTrack = null }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Inicio") })
                NavigationBarItem(selected = tab == AppTab.SEARCH, onClick = { tab = AppTab.SEARCH; selectedTrack = null }, icon = { Icon(Icons.Default.Search, null) }, label = { Text("Buscar") })
                NavigationBarItem(selected = tab == AppTab.DOWNLOADS, onClick = { tab = AppTab.DOWNLOADS; selectedTrack = null }, icon = { Icon(Icons.Default.Download, null) }, label = { Text("Descargas") })
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Crossfade(targetState = Pair(tab, selectedTrack), label = "tab") { (currentTab, currentTrack) ->
                if (currentTrack != null) {
                    DownloadScreen(track = currentTrack, onBack = { selectedTrack = null })
                } else {
                    when (currentTab) {
                        AppTab.HOME -> HomeScreen { seedQuery = it; tab = AppTab.SEARCH }
                        AppTab.SEARCH -> SearchScreen(
                            onDownload = { selectedTrack = it },
                            onBack = { tab = AppTab.HOME },
                            initialQuery = seedQuery,
                            onGlobalBackendError = { globalBackendError = it },
                        )
                        AppTab.DOWNLOADS -> DownloadsHistoryScreen(onBack = { tab = AppTab.HOME })
                    }
                }
            }
        }
    }

    globalBackendError?.let { backendErr ->
        AlertDialog(
            onDismissRequest = { globalBackendError = null },
            confirmButton = { TextButton(onClick = { globalBackendError = null }) { Text("Cerrar") } },
            title = { Text("Error global de backend") },
            text = { Text(backendErr) },
        )
    }

    if (showPythonError && !pythonError.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showPythonError = false },
            confirmButton = { TextButton(onClick = { showPythonError = false }) { Text("OK") } },
            title = { Text("Motor de extracción no disponible") },
            text = { Text("$pythonError") },
        )
    }
}

@Composable
private fun RequestStartupPermissions() {
    val permissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }.toTypedArray()
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    var launched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!launched) {
            launched = true
            launcher.launch(permissions)
        }
    }
}
