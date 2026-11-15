package com.rupeshbhurke.househunter.feature.entry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rupeshbhurke.househunter.R
import com.rupeshbhurke.househunter.core.data.db.AttachmentEntity
import com.rupeshbhurke.househunter.core.data.db.ContactEntity
import com.rupeshbhurke.househunter.core.data.db.EntryEntity
import com.rupeshbhurke.househunter.core.data.model.FieldResolver
import com.rupeshbhurke.househunter.core.data.repo.EntryDetail
import com.rupeshbhurke.househunter.core.data.repo.FieldEntry
import com.rupeshbhurke.househunter.core.data.repo.NoteItem
import com.rupeshbhurke.househunter.core.util.ContactActions
import com.rupeshbhurke.househunter.core.util.FieldFormat
import com.rupeshbhurke.househunter.core.util.MapActions
import com.rupeshbhurke.househunter.ui.components.MediaStrip
import com.rupeshbhurke.househunter.ui.components.RatingStars
import com.rupeshbhurke.househunter.ui.components.locationSummary
import com.rupeshbhurke.househunter.ui.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onCapture: (Long) -> Unit,
    onVideoNote: (Long) -> Unit,
    onOpenMedia: (entryId: Long, index: Int) -> Unit,
    viewModel: EntryDetailViewModel = hiltViewModel(),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        viewModel.importMedia(uris)
    }
    LaunchedEffect(viewModel) {
        viewModel.importResult.collect { result ->
            val message = buildString {
                append(context.resources.getQuantityString(R.plurals.import_done, result.imported, result.imported))
                if (result.failed > 0) {
                    append(" ")
                    append(context.resources.getQuantityString(R.plurals.import_failed, result.failed, result.failed))
                }
            }
            snackbar.showSnackbar(message)
        }
    }
    val notesActions = remember(viewModel) {
        NotesActions(
            newAudioFile = viewModel::newAudioFile,
            onAddText = viewModel::addTextNote,
            onSaveAudio = viewModel::addAudioNote,
            onVideoNote = { onVideoNote(viewModel.entryId) },
            onEditText = viewModel::updateTextNote,
            onDelete = viewModel::deleteNote,
        )
    }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(detail?.entry?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(viewModel.entryId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                },
            )
        },
    ) { padding ->
        detail?.let {
            EntryDetailContent(
                detail = it,
                notes = notes,
                importing = importing,
                onImport = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                notesActions = notesActions,
                onCapture = { onCapture(viewModel.entryId) },
                onOpenMedia = { index -> onOpenMedia(viewModel.entryId, index) },
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.entry_delete_title)) },
            text = { Text(stringResource(R.string.entry_delete_body, detail?.entry?.title.orEmpty())) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete(onBack) }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun EntryDetailContent(
    detail: EntryDetail,
    notes: List<NoteItem>,
    importing: Boolean,
    onImport: () -> Unit,
    notesActions: NotesActions,
    onCapture: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filled = detail.fields.filterNot { it.isBlank }
    val groups = FieldResolver.groupInOrder(filled) { it.def.group }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusChip(detail.entry.status)
                RatingStars(detail.entry.rating)
            }
        }
        if (detail.entry.lat != null && detail.entry.lng != null) {
            item { LocationCard(detail.entry) }
        }
        item { MediaCard(detail.attachments, importing, onCapture, onImport, onOpenMedia) }
        item { NotesCard(notes, notesActions) }
        if (groups.isEmpty() && detail.contacts.isEmpty() && detail.entry.lat == null && detail.attachments.isEmpty() && notes.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.entry_detail_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(groups, key = { it.first }) { (group, fields) -> FieldGroupCard(group, fields) }
        if (detail.contacts.isNotEmpty()) {
            item { ContactsCard(detail.contacts) }
        }
    }
}

@Composable
private fun FieldGroupCard(group: String, fields: List<FieldEntry>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(group, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            fields.forEach { field ->
                val value = FieldFormat.display(field.def.type, field.text, field.number) ?: return@forEach
                Column {
                    Text(
                        field.def.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(value, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun ContactsCard(contacts: List<ContactEntity>) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.entry_contacts),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            contacts.forEach { contact ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${contact.name} · ${stringResource(contact.role.labelRes)}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            contact.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { ContactActions.dial(context, contact.phone) }) {
                        Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.entry_call))
                    }
                    IconButton(onClick = { ContactActions.whatsApp(context, contact.phone) }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = stringResource(R.string.entry_whatsapp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationCard(entry: EntryEntity) {
    val context = LocalContext.current
    val lat = entry.lat ?: return
    val lng = entry.lng ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.location_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(locationSummary(entry), style = MaterialTheme.typography.bodyLarge)
            }
            TextButton(onClick = { MapActions.open(context, lat, lng, entry.title) }) {
                Text(stringResource(R.string.location_open_maps))
            }
        }
    }
}

@Composable
private fun MediaCard(
    media: List<AttachmentEntity>,
    importing: Boolean,
    onCapture: () -> Unit,
    onImport: () -> Unit,
    onOpen: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (media.isEmpty()) stringResource(R.string.media_title)
                else stringResource(R.string.media_title_count, media.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (media.isNotEmpty()) MediaStrip(media, onOpen)
            if (importing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onCapture) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Text(stringResource(R.string.media_capture), modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = onImport, enabled = !importing) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Text(stringResource(R.string.media_import), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
