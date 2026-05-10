package com.ytmusicdl.app.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val PREFS_NAME = "ytmusicdl_prefs"
private const val KEY_COUNTRY = "selected_country"

private data class ChartTrack(val title: String, val artist: String)

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

    val globalTop10 = remember {
        listOf(
            ChartTrack("Blinding Lights", "The Weeknd"),
            ChartTrack("Shape of You", "Ed Sheeran"),
            ChartTrack("As It Was", "Harry Styles"),
            ChartTrack("bad guy", "Billie Eilish"),
            ChartTrack("Levitating", "Dua Lipa"),
            ChartTrack("One Dance", "Drake"),
            ChartTrack("Stay", "The Kid LAROI"),
            ChartTrack("Someone You Loved", "Lewis Capaldi"),
            ChartTrack("Sunflower", "Post Malone"),
            ChartTrack("Señorita", "Shawn Mendes & Camila Cabello"),
        )
    }

    val countryTop20 = remember(selectedCountry) { buildCountryTop20(selectedCountry) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Top 10 más escuchadas", style = MaterialTheme.typography.headlineMedium)
            Text("Listado global", style = MaterialTheme.typography.bodyMedium)
        }

        items(globalTop10) { track ->
            ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { onQuickSearch("${track.title} ${track.artist}") }) {
                Column(Modifier.padding(12.dp)) {
                    Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text("Top 20 en $selectedCountry", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { showCountryPicker = true }) { Text("Cambiar país") }
        }

        items(countryTop20) { track ->
            ListItem(
                headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(track.artist) },
                modifier = Modifier.clickable { onQuickSearch("${track.title} ${track.artist}") }
            )
            HorizontalDivider()
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

private fun buildCountryTop20(country: String): List<ChartTrack> {
    val suffix = if (country.isBlank()) "Global" else country
    return (1..20).map { idx -> ChartTrack("Top $idx - $suffix", "Artista $idx") }
}

private fun loadCountry(context: Context): String {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_COUNTRY, "") ?: ""
}

private fun saveCountry(context: Context, country: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_COUNTRY, country).apply()
}
