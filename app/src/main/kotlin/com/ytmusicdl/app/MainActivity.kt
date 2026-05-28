package com.ytmusicdl.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ytmusicdl.app.data.api.PythonBridge
import com.ytmusicdl.app.data.model.Track
import com.ytmusicdl.app.ui.screens.AlbumDownloadScreen
import com.ytmusicdl.app.ui.screens.DownloadsHistoryScreen
import com.ytmusicdl.app.ui.screens.HomeScreen
import com.ytmusicdl.app.ui.screens.SearchScreen
import com.ytmusicdl.app.ui.screens.SongDownloadScreen
import com.ytmusicdl.app.ui.theme.YtmusicdlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        PythonBridge.initialize(this)
        setContent { YtmusicdlTheme { App(PythonBridge.getInitError()) } }
    }
}

private enum class AppTab { HOME, SEARCH, QUEUE, LIBRARY }
private enum class DetailMode { SONG, ALBUM }

@Composable
fun App(pythonError: String? = null) {
    RequestStartupPermissions()
    var showPythonError by remember(pythonError) { mutableStateOf(!pythonError.isNullOrBlank()) }
    var globalBackendError by remember { mutableStateOf<String?>(null) }
    var selectedTrack by remember { mutableStateOf<Track?>(null) }
    var detailMode by remember { mutableStateOf(DetailMode.SONG) }
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var seedQuery by remember { mutableStateOf("") }

    BackHandler(enabled = selectedTrack != null || tab != AppTab.HOME) {
        when {
            selectedTrack != null -> selectedTrack = null
            tab != AppTab.HOME -> tab = AppTab.HOME
        }
    }

    Scaffold(bottomBar = {
        if (selectedTrack == null) {
            NavigationBar {
                NavigationBarItem(selected = tab == AppTab.HOME, onClick = { tab = AppTab.HOME }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = tab == AppTab.SEARCH, onClick = { tab = AppTab.SEARCH }, icon = { Icon(Icons.Default.Search, null) }, label = { Text("Search") })
                NavigationBarItem(selected = tab == AppTab.QUEUE, onClick = { tab = AppTab.QUEUE }, icon = { Icon(Icons.Default.Download, null) }, label = { Text("Queue") })
                NavigationBarItem(selected = tab == AppTab.LIBRARY, onClick = { tab = AppTab.LIBRARY }, icon = { Icon(Icons.Default.Folder, null) }, label = { Text("Library") })
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Crossfade(targetState = Pair(tab, selectedTrack), label = "tab") { (currentTab, currentTrack) ->
                if (currentTrack != null) {
                    when (detailMode) {
                        DetailMode.SONG -> SongDownloadScreen(track = currentTrack, onBack = { selectedTrack = null })
                        DetailMode.ALBUM -> AlbumDownloadScreen(track = currentTrack, onBack = { selectedTrack = null })
                    }
                } else {
                    when (currentTab) {
                        AppTab.HOME -> HomeScreen(
                            onOpenSearch = { tab = AppTab.SEARCH },
                            onQuickSearch = { seedQuery = it; tab = AppTab.SEARCH },
                            onOpenQueue = { tab = AppTab.QUEUE },
                            onOpenLibrary = { tab = AppTab.LIBRARY },
                        )
                        AppTab.SEARCH -> SearchScreen(
                            onOpenSong = { detailMode = DetailMode.SONG; selectedTrack = it },
                            onOpenAlbum = { detailMode = DetailMode.ALBUM; selectedTrack = it },
                            onBack = { tab = AppTab.HOME },
                            initialQuery = seedQuery,
                            onGlobalBackendError = { globalBackendError = it },
                        )
                        AppTab.QUEUE -> DownloadsHistoryScreen(onBack = { tab = AppTab.HOME }, showQueueOnly = true)
                        AppTab.LIBRARY -> DownloadsHistoryScreen(onBack = { tab = AppTab.HOME }, showQueueOnly = false)
                    }
                }
            }
        }
    }

    globalBackendError?.let { backendErr ->
        AlertDialog(onDismissRequest = { globalBackendError = null }, confirmButton = { TextButton(onClick = { globalBackendError = null }) { Text("Cerrar") } }, title = { Text("Error global de backend") }, text = { Text(backendErr) })
    }

    if (showPythonError && !pythonError.isNullOrBlank()) {
        AlertDialog(onDismissRequest = { showPythonError = false }, confirmButton = { TextButton(onClick = { showPythonError = false }) { Text("OK") } }, title = { Text("Motor de extracción no disponible") }, text = { Text("$pythonError") })
    }

    LaunchedEffect(seedQuery, tab) {
        if (tab != AppTab.SEARCH) return@LaunchedEffect
        if (seedQuery.isNotBlank()) seedQuery = ""
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
