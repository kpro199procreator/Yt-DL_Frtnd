package com.ytmusicdl.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ytmusicdl.app.data.api.PythonBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun YtDlpCliScreen() {
    val scope = rememberCoroutineScope()
    var argsLine by remember { mutableStateOf("--help") }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var exitCode by remember { mutableStateOf<Int?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("CLI yt-dlp", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Ejecuta comandos de yt-dlp dentro de la app (runtime Python embebido).",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = argsLine,
            onValueChange = { argsLine = it },
            label = { Text("Argumentos") },
            supportingText = { Text("Ej: --help o -F https://youtube.com/watch?v=...") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (!PythonBridge.isAvailable()) {
                        error = PythonBridge.getInitError() ?: "Python no disponible"
                        return@Button
                    }
                    loading = true
                    output = ""
                    error = ""
                    exitCode = null
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                PythonBridge.call("run_ytdlp_cli", argsLine)
                            }
                        }.onSuccess { raw ->
                            val obj = JSONObject(raw)
                            output = obj.optString("stdout")
                            error = obj.optString("stderr")
                            exitCode = obj.optInt("exitCode", -1)
                        }.onFailure {
                            error = it.message ?: "Error ejecutando comando"
                        }
                        loading = false
                    }
                }
            ) { Text("Ejecutar") }
        }

        if (loading) CircularProgressIndicator()
        exitCode?.let { Text("Exit code: $it") }

        if (output.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(output, modifier = Modifier.padding(12.dp))
            }
        }

        if (error.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text("stderr", color = MaterialTheme.colorScheme.error)
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(error, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
