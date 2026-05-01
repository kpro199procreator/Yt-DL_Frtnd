package com.ytmusicdl.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onQuickSearch: (String) -> Unit) {
    val quickSearches = listOf(
        "Rara Vez", "Kali Uchis", "Tame Impala", "The Weeknd", "Arctic Monkeys",
    )
    var showTips by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Inicio", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Descubre música y comienza una búsqueda rápida.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(10.dp))
        }

        item {
            AnimatedVisibility(visible = showTips, enter = fadeIn(), exit = fadeOut()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth().clickable { showTips = false },
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Tip", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Toca una sugerencia para ir directo a Buscar.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        items(quickSearches) { q ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onQuickSearch(q) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(q, style = MaterialTheme.typography.titleMedium)
                    Text("Búsqueda rápida", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
