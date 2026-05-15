package com.ytmusicdl.app.ui.screens

import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ytmusicdl.app.service.DownloadService
import java.io.File

@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onQuickSearch: (String) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val queue = DownloadService.queueState.value
    val active = queue.count { it.status == "downloading" || it.status == "queued" }
    val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "ytmusicdl")
    val files = downloadDir.takeIf { it.exists() }?.listFiles()?.filter { it.isFile }.orEmpty()
    val totalBytes = files.sumOf { it.length() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Download Control Center", style = MaterialTheme.typography.titleLarge)
                    Text("Estado: ${if (active > 0) "Activo ($active)" else "Idle"}")
                    Text("${files.size} archivos · ${"%.2f".format(totalBytes / (1024f * 1024f * 1024f))} GB")
                }
            }
        }
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Buscar / Pegar URL", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Song, album, playlist o URL") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (query.isNotBlank()) onQuickSearch(query) else onOpenSearch()
                        }),
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ElevatedCard(Modifier.weight(1f)) { TextButton(onClick = { if (query.isNotBlank()) onQuickSearch(query) else onOpenSearch() }) { Icon(Icons.Default.Link, null); Spacer(Modifier.width(6.dp)); Text("Pegar URL") } }
                ElevatedCard(Modifier.weight(1f)) { TextButton(onClick = onOpenSearch) { Icon(Icons.Default.Search, null); Spacer(Modifier.width(6.dp)); Text("Buscar") } }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ElevatedCard(Modifier.weight(1f)) { TextButton(onClick = onOpenQueue) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("Queue") } }
                ElevatedCard(Modifier.weight(1f)) { TextButton(onClick = onOpenLibrary) { Icon(Icons.Default.Folder, null); Spacer(Modifier.width(6.dp)); Text("Library") } }
            }
        }
        item {
            AnimatedVisibility(visible = queue.isNotEmpty()) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Descargas activas", style = MaterialTheme.typography.titleMedium)
                        queue.take(3).forEach {
                            Text(it.title, maxLines = 1)
                            LinearProgressIndicator(progress = { it.progress / 100f }, modifier = Modifier.fillMaxWidth())
                            Text("${it.status} · ${it.progress}%", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Storage", style = MaterialTheme.typography.titleMedium)
                    Text("Directorio: ${downloadDir.path}")
                    TextButton(onClick = onOpenLibrary) { Icon(Icons.Default.History, null); Spacer(Modifier.width(6.dp)); Text("Gestionar offline") }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
