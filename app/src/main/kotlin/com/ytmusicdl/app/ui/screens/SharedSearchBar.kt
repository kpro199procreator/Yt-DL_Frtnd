package com.ytmusicdl.app.ui.screens

import com.ytmusicdl.app.R

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YtmusicSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.search_placeholder),
    onPaste: (() -> Unit)? = null,
    showClear: Boolean = false,
    onSearch: (String) -> Unit = {},
) {
    SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = { submitted ->
            if (submitted.isNotBlank()) onSearch(submitted) else onSearch(query)
        },
        active = false,
        onActiveChange = {},
        modifier = modifier,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            when {
                onPaste != null -> IconButton(onClick = onPaste) { Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.paste)) }
                showClear && query.isNotEmpty() -> IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear)) }
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {}
}
