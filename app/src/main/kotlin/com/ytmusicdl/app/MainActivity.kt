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
import androidx.compose.ui.unit.dp
import com.ytmusicdl.app.data.api.NewPipeService
import com.ytmusicdl.app.data.api.PythonBridge
import com.ytmusicdl.app.data.model.Track
import com.ytmusicdl.app.ui.screens.DownloadSheet
import com.ytmusicdl.app.ui.screens.HomeScreen
import com.ytmusicdl.app.ui.screens.SearchScreen
import com.ytmusicdl.app.ui.theme.YtmusicdlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        NewPipeService.init()
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

    var selectedTrack by remember { mutableStateOf<Track?>(null) }
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var seedQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ytmusicdl") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == AppTab.HOME, { tab = AppTab.HOME }, { Icon(Icons.Default.Home, null) }, label = { Text("Inicio") })
                NavigationBarItem(tab == AppTab.SEARCH, { tab = AppTab.SEARCH }, { Icon(Icons.Default.Search, null) }, label = { Text("Buscar") })
                NavigationBarItem(tab == AppTab.DOWNLOADS, { tab = AppTab.DOWNLOADS }, { Icon(Icons.Default.Download, null) }, label = { Text("Descargas") })
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Crossfade(targetState = tab, label = "tab") { current ->
                when (current) {
                    AppTab.HOME -> HomeScreen(onQuickSearch = { seedQuery = it; tab = AppTab.SEARCH })
                    AppTab.SEARCH -> SearchScreen(onDownload = { selectedTrack = it }, initialQuery = seedQuery)
                    AppTab.DOWNLOADS -> Box(Modifier.fillMaxSize()) { Text("Historial próximamente", modifier = Modifier.padding(24.dp)) }
                }
            }
        }

        selectedTrack?.let { track ->
            DownloadSheet(track = track, onDismiss = { selectedTrack = null })
        }
    }

    if (showPythonError && !pythonError.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showPythonError = false },
            confirmButton = { TextButton(onClick = { showPythonError = false }) { Text("OK") } },
            title = { Text("Python no disponible") },
            text = { Text("$pythonError\nSe usará NewPipe como respaldo.") },
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
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    var launched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!launched) {
            launched = true
            launcher.launch(permissions)
        }
    }
}
