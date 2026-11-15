package com.rupeshbhurke.househunter.feature.entries

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rupeshbhurke.househunter.R
import com.rupeshbhurke.househunter.core.data.repo.EntrySummary
import com.rupeshbhurke.househunter.core.util.FieldFormat
import com.rupeshbhurke.househunter.ui.components.MediaThumbnail
import com.rupeshbhurke.househunter.ui.components.RatingStars
import com.rupeshbhurke.househunter.ui.components.StatusChip

private const val MAX_COMPARE = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryListScreen(
    onBack: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onEntryCreated: (Long) -> Unit,
    onCompare: (List<Long>) -> Unit,
    viewModel: EntryListViewModel = hiltViewModel(),
) {
    val name by viewModel.collectionName.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val fields by viewModel.fields.collectAsStateWithLifecycle()

    var searching by remember { mutableStateOf(filter.text.isNotEmpty()) }
    var showFilters by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptyList<Long>()) }
    val selecting = selected.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when {
                        selecting -> Text(pluralStringResource(R.plurals.list_selected, selected.size, selected.size))
                        searching -> SearchField(
                            value = filter.text,
                            onChange = { viewModel.setFilter(filter.copy(text = it)) },
                        )
                        else -> Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            selecting -> selected = emptyList()
                            searching -> {
                                searching = false
                                viewModel.setFilter(filter.copy(text = ""))
                            }
                            else -> onBack()
                        }
                    }) {
                        Icon(
                            if (selecting) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(if (selecting) R.string.list_cancel_selection else R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (selecting) {
                        IconButton(onClick = { onCompare(selected); selected = emptyList() }, enabled = selected.size >= 2) {
                            Icon(Icons.Filled.CompareArrows, contentDescription = stringResource(R.string.list_compare))
                        }
                    } else {
                        if (!searching) {
                            IconButton(onClick = { searching = true }) {
                                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.list_search))
                            }
                        }
                        IconButton(onClick = { showFilters = true }) {
                            BadgedBox(badge = {
                                // The search text has its own field, so only the sheet's conditions are counted here.
                                val count = filter.activeCount - if (filter.text.isNotBlank()) 1 else 0
                                if (count > 0) Badge { Text(count.toString()) }
                            }) {
                                Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.list_filter))
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!selecting) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.addEntry(onEntryCreated) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.entries_add)) },
                )
            }
        },
    ) { padding ->
        val current = state
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (filter.isActive && !selecting) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        pluralStringResource(R.plurals.list_filters_active, filter.activeCount, filter.activeCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.setFilter(filter.cleared()); searching = false }) {
                        Text(stringResource(R.string.list_clear_filters))
                    }
                }
            }
            when {
                current == null -> Unit
                current.total == 0 -> EmptyEntries(R.string.entries_empty_title, R.string.entries_empty_body)
                current.visible.isEmpty() -> EmptyEntries(R.string.list_no_match_title, R.string.list_no_match_body)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(current.visible, key = { it.entry.id }) { summary ->
                        val id = summary.entry.id
                        EntryCard(
                            summary = summary,
                            selecting = selecting,
                            selected = id in selected,
                            onClick = {
                                when {
                                    !selecting -> onOpenEntry(id)
                                    id in selected -> selected = selected - id
                                    selected.size < MAX_COMPARE -> selected = selected + id
                                }
                            },
                            onLongClick = { if (!selecting) selected = listOf(id) },
                        )
                    }
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            filter = filter,
            fields = fields,
            onChange = viewModel::setFilter,
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(stringResource(R.string.list_search_hint)) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryCard(
    summary: EntrySummary,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val entry = summary.entry
    val subtitle = listOfNotNull(summary.bhk, summary.price?.let(FieldFormat::money)).joinToString("  •  ")
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selecting) Checkbox(checked = selected, onCheckedChange = null)
            val cover = summary.cover
            if (cover != null) {
                MediaThumbnail(cover, size = 64.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(entry.status)
                    RatingStars(entry.rating, size = 16.dp)
                }
            }
        }
    }
}

@Composable
private fun EmptyEntries(title: Int, body: Int) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
