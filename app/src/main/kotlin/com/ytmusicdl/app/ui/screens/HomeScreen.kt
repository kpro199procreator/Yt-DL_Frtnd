package com.ytmusicdl.app.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytmusicdl.app.data.api.PythonBridge
import com.ytmusicdl.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREFS_NAME = "ytmusicdl_prefs"
private const val KEY_COUNTRY = "selected_country"

@Composable
fun HomeScreen(onQuickSearch: (String) -> Unit) {
    val context = LocalContext.current
    var selectedCountry by remember { mutableStateOf(loadCountry(context)) }
    var showCountryPicker by remember { mutableStateOf(selectedCountry.isBlank()) }

    if (showCountryPicker) {
        CountryPickerDialog(
            onSelect = { country ->
                selectedCountry = country
                saveCountry(context, country)
                showCountryPicker = false
            }
        )
    }

    var globalTop10 by remember { mutableStateOf<List<Track>>(emptyList()) }
    var countryTop20 by remember(selectedCountry) { mutableStateOf<List<Track>>(emptyList()) }
    LaunchedEffect(selectedCountry) {
        withContext(Dispatchers.IO) {
            runCatching { PythonBridge.parseTrackList(PythonBridge.call("get_top_world", 10)) }.getOrNull()?.let { globalTop10 = it }
            runCatching { PythonBridge.parseTrackList(PythonBridge.call("get_top_region", selectedCountry, 20)) }.getOrNull()?.let { countryTop20 = it }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Top 10 más escuchadas", style = MaterialTheme.typography.headlineMedium)
            Text("Listado global", style = MaterialTheme.typography.bodyMedium)
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(globalTop10, key = { it.videoId }) { track ->
            ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { onQuickSearch("${track.title} ${track.artist}") }) {
                Row(Modifier.padding(12.dp)) {
                    AsyncImage(
                        model = "https://picsum.photos/seed/${track.title}/160/160",
                        contentDescription = null,
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                    Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text("Top 20 en $selectedCountry", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { showCountryPicker = true }) { Text("Cambiar país") }
        }

        items(countryTop20) { track ->
            ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { onQuickSearch("${track.title} ${track.artist}") }) {
                Row(Modifier.padding(12.dp)) {
                    AsyncImage(
                        model = "https://picsum.photos/seed/${track.artist}/160/160",
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(track.title.ifBlank { "Cargando..." }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist.ifBlank { "Artista" }, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryPickerDialog(onSelect: (String) -> Unit) {
    val countries = listOf("México", "Argentina", "España", "Colombia", "Chile", "Estados Unidos")
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text("Selecciona tu país") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                countries.forEach { country ->
                    OutlinedButton(onClick = { onSelect(country) }, modifier = Modifier.fillMaxWidth()) {
                        Text(country)
                    }
                }
            }
        }
    )
}


private fun loadCountry(context: Context): String {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_COUNTRY, "") ?: ""
}

private fun saveCountry(context: Context, country: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_COUNTRY, country).apply()
}
