package com.ytmusicdl.app.ui.screens

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun DownloadsHistoryScreen(onBack: () -> Unit) {
    var files by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(Unit) {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "ytmusicdl")
        files = dir.takeIf { it.exists() }?.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
        Text("Descargas", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))

        if (files.isEmpty()) {
            Text("Sin descargas aún", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(files, key = { it.absolutePath }) { file ->
                    ListItem(
                        headlineContent = { Text(file.name) },
                        supportingContent = { Text(file.absolutePath, style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.clickable { }
                    )
                    Divider()
                }
            }
        }
    }
}
