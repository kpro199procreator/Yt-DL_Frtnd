package com.ytmusicdl.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ytmusicdl.app.data.SettingsPrefs

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var darkMode by remember { mutableStateOf(false) }
    var dynamicColor by remember { mutableStateOf(true) }
    var downloadPath by remember { mutableStateOf(SettingsPrefs.downloadPath(context)) }
    var filenameTemplate by remember { mutableStateOf(SettingsPrefs.filenameTemplate(context)) }
    var maxConcurrent by remember { mutableStateOf(SettingsPrefs.maxConcurrent(context).toFloat()) }
    var outputFormat by remember { mutableStateOf(SettingsPrefs.outputFormat(context)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Configuración", style = MaterialTheme.typography.headlineMedium)
            Text("Ajusta tu experiencia y preferencias.", style = MaterialTheme.typography.bodyMedium)
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Text("Ruta de descargas", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedTextField(
                        value = downloadPath,
                        onValueChange = { downloadPath = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("/Music/ytmusicdl") },
                    )
                    OutlinedTextField(
                        value = filenameTemplate,
                        onValueChange = { filenameTemplate = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Template de nombre") },
                        placeholder = { Text("{artist} - {track} ({year}) [{album}]") },
                    )
                    Text("Variables: {album} {track} {year} {artist}", style = MaterialTheme.typography.bodySmall)
                    Text("Descargas simultáneas: ${maxConcurrent.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = maxConcurrent,
                        onValueChange = { maxConcurrent = it },
                        valueRange = 1f..15f,
                        steps = 13,
                    )
                    Text("Formato final", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SettingsPrefs.supportedOutputFormats.forEach { format ->
                            FilterChip(
                                selected = outputFormat == format,
                                onClick = { outputFormat = format },
                                label = { Text(format.uppercase()) },
                                leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                            )
                        }
                    }
                    Text(
                        "El stream se descarga y luego FFmpegKit lo convierte dentro de la app, sin depender de un ejecutable externo.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = { SettingsPrefs.save(context, downloadPath, filenameTemplate, maxConcurrent.toInt(), outputFormat) }) {
                        Text("Guardar")
                    }
                }
            }
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ColorLens, contentDescription = null)
                        Text("Apariencia", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Modo oscuro")
                        Switch(checked = darkMode, onCheckedChange = { darkMode = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Color dinámico")
                        Switch(checked = dynamicColor, onCheckedChange = { dynamicColor = it })
                    }
                }
            }
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Text("Acerca de", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("ytmusicdl", style = MaterialTheme.typography.bodyMedium)
                    Text("Versión 0.1.0-alpha", style = MaterialTheme.typography.bodySmall)
                    Text("Aplicación de descarga y gestión local de música.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Text("Próximas opciones", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("• Selector avanzado de calidad por descarga\n• Perfiles de bitrate por formato\n• Auto-descarga de carátula\n• Limpieza automática de caché", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
