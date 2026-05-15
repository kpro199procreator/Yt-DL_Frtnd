package com.ytmusicdl.app.ui.screens

import android.os.Environment
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
import java.io.File

@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onQuickSearch: (String) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "ytmusicdl")
    val files = downloadDir.takeIf { it.exists() }?.listFiles()?.filter { it.isFile }.orEmpty()
    val totalBytes = files.sumOf { it.length() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Hola", style = MaterialTheme.typography.headlineMedium)
            Text("Centro de descargas", style = MaterialTheme.typography.bodyMedium)
        }
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Buscar o pegar URL", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Canción, álbum, playlist o URL") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (query.isNotBlank()) onQuickSearch(query) else onOpenSearch()
                        }),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onOpenSearch() }) { Icon(Icons.Default.Search, null); Spacer(Modifier.width(6.dp)); Text("Abrir Search") }
                        TextButton(onClick = { if (query.isNotBlank()) onQuickSearch(query) }) { Icon(Icons.Default.Link, null); Spacer(Modifier.width(6.dp)); Text("Pegar URL") }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ElevatedCard(Modifier.weight(1f)) { TextButton(onClick = onOpenQueue) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("Queue") } }
                Spacer(Modifier.width(8.dp))
                ElevatedCard(Modifier.weight(1f)) { TextButton(onClick = onOpenLibrary) { Icon(Icons.Default.Folder, null); Spacer(Modifier.width(6.dp)); Text("Library") } }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Storage Status", style = MaterialTheme.typography.titleMedium)
                    Text("${files.size} archivos descargados")
                    Text("${"%.2f".format(totalBytes / (1024f * 1024f * 1024f))} GB usados")
                    TextButton(onClick = onOpenLibrary) { Icon(Icons.Default.History, null); Spacer(Modifier.width(6.dp)); Text("Ver descargados") }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
