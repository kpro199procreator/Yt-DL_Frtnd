package com.ytmusicdl.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    var darkMode by remember { mutableStateOf(false) }
    var dynamicColor by remember { mutableStateOf(true) }
    var downloadPath by remember { mutableStateOf("/Music/ytmusicdl") }

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
                    Text("(Placeholder) Persistencia de ruta en próxima versión", style = MaterialTheme.typography.bodySmall)
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
                    Text("• Calidad preferida\n• Formato predeterminado\n• Auto-descarga de carátula\n• Limpieza automática de caché", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
