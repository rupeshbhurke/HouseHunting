package com.rupeshbhurke.househunter.share

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rupeshbhurke.househunter.R
import com.rupeshbhurke.househunter.core.data.repo.EntrySummary
import com.rupeshbhurke.househunter.core.util.FieldFormat
import com.rupeshbhurke.househunter.ui.components.MediaThumbnail
import com.rupeshbhurke.househunter.ui.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    viewModel: ShareViewModel,
    onClose: () -> Unit,
    onOpenFlat: (Long) -> Unit,
) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val count = viewModel.uris.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.share_title))
                        Text(
                            pluralStringResource(R.plurals.share_file_count, count, count),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    if (step is ShareStep.ChooseFlat) {
                        IconButton(onClick = viewModel::back) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (val s = step) {
            ShareStep.ChooseCollection -> ChooseCollection(viewModel, modifier)
            is ShareStep.ChooseFlat -> ChooseFlat(s, viewModel, modifier)
            ShareStep.Importing -> Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.share_importing))
            }
            is ShareStep.Done -> Done(s, onClose, onOpenFlat, modifier)
        }
    }
}

@Composable
private fun ChooseCollection(viewModel: ShareViewModel, modifier: Modifier) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(stringResource(R.string.share_choose_collection), style = MaterialTheme.typography.titleMedium) }
        items(collections.orEmpty(), key = { it.collection.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth().clickable { viewModel.chooseCollection(item) }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(item.collection.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        pluralStringResource(R.plurals.collection_flat_count, item.entryCount, item.entryCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            OutlinedButton(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.collections_new), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    if (creating) {
        var name by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text(stringResource(R.string.collections_create_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.collections_name_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.createCollection(name); creating = false },
                    enabled = name.isNotBlank(),
                ) { Text(stringResource(R.string.action_create)) }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun ChooseFlat(step: ShareStep.ChooseFlat, viewModel: ShareViewModel, modifier: Modifier) {
    val flats by viewModel.flats.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.share_choose_flat, step.collectionName),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        item {
            Button(onClick = { viewModel.importIntoNewFlat(step.collectionId) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.share_new_flat), modifier = Modifier.padding(start = 8.dp))
            }
        }
        items(flats.orEmpty(), key = { it.entry.id }) { summary ->
            FlatChoice(summary, onClick = { viewModel.importIntoFlat(summary.entry.id, summary.entry.title) })
        }
    }
}

@Composable
private fun FlatChoice(summary: EntrySummary, onClick: () -> Unit) {
    val subtitle = listOfNotNull(summary.bhk, summary.price?.let(FieldFormat::money)).joinToString("  •  ")
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            summary.cover?.let { MediaThumbnail(it, size = 48.dp) }
            Column(modifier = Modifier.weight(1f)) {
                Text(summary.entry.title, style = MaterialTheme.typography.titleMedium)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            StatusChip(summary.entry.status)
        }
    }
}

@Composable
private fun Done(step: ShareStep.Done, onClose: () -> Unit, onOpenFlat: (Long) -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            pluralStringResource(R.plurals.share_done, step.result.imported, step.result.imported, step.flatTitle),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (step.result.failed > 0) {
            Text(
                pluralStringResource(R.plurals.share_failed, step.result.failed, step.result.failed),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Button(onClick = { onOpenFlat(step.entryId) }) { Text(stringResource(R.string.share_open_flat)) }
        OutlinedButton(onClick = onClose) { Text(stringResource(R.string.share_close)) }
    }
}
