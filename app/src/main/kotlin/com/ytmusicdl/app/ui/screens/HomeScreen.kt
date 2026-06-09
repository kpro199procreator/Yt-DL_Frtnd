package com.ytmusicdl.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    onQuickSearch: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var isLaunchingSearch by rememberSaveable { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(query) {
        val cleanQuery = query.trim()
        if (cleanQuery.isNotEmpty()) {
            isLaunchingSearch = true
            delay(320)
            onQuickSearch(cleanQuery)
        } else {
            isLaunchingSearch = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp),
        contentAlignment = if (isLaunchingSearch) Alignment.TopCenter else Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = !isLaunchingSearch,
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 300),
                targetOffsetY = { fullHeight -> -fullHeight },
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Ytmusicdl",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                HomeSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onPaste = { query = clipboardManager.getText()?.text.orEmpty() },
                )
            }
        }

        if (isLaunchingSearch) {
            HomeSearchBar(
                query = query,
                onQueryChange = { query = it },
                onPaste = { query = clipboardManager.getText()?.text.orEmpty() },
                modifier = Modifier.padding(top = 12.dp),
                placeholder = "Abriendo Search…",
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Song, album, playlist o URL",
) {
    SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = { submitted -> if (submitted.isNotBlank()) onQueryChange(submitted) },
        active = false,
        onActiveChange = {},
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onPaste) {
                Icon(Icons.Default.ContentPaste, contentDescription = "Pegar")
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {}
}
