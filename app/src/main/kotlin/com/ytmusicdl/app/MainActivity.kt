package com.ytmusicdl.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ytmusicdl.app.data.api.NewPipeService
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

        setContent {
            YtmusicdlTheme { App() }
        }
    }
}

private enum class AppTab(val label: String) { HOME("Inicio"), SEARCH("Buscar"), DOWNLOADS("Descargas") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var selectedTrack by remember { mutableStateOf<Track?>(null) }
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var seedQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ytmusicdl") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == AppTab.HOME,
                    onClick = { tab = AppTab.HOME },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Inicio") },
                )
                NavigationBarItem(
                    selected = tab == AppTab.SEARCH,
                    onClick = { tab = AppTab.SEARCH },
                    icon = { Icon(Icons.Default.Search, null) },
                    label = { Text("Buscar") },
                )
                NavigationBarItem(
                    selected = tab == AppTab.DOWNLOADS,
                    onClick = { tab = AppTab.DOWNLOADS },
                    icon = { Icon(Icons.Default.Download, null) },
                    label = { Text("Descargas") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Crossfade(targetState = tab, label = "tab") { current ->
                when (current) {
                    AppTab.HOME -> HomeScreen(
                        onQuickSearch = {
                            seedQuery = it
                            tab = AppTab.SEARCH
                        },
                    )
                    AppTab.SEARCH -> SearchScreen(
                        onDownload = { selectedTrack = it },
                        initialQuery = seedQuery,
                    )
                    AppTab.DOWNLOADS -> Box(Modifier.fillMaxSize()) {
                        Text("Historial próximamente", modifier = Modifier.padding(24.dp))
                    }
                }
            }
        }

        selectedTrack?.let { track ->
            DownloadSheet(track = track, onDismiss = { selectedTrack = null })
        }
    }
}
