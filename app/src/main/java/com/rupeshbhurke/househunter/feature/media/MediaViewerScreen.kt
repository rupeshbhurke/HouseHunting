package com.rupeshbhurke.househunter.feature.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.rupeshbhurke.househunter.R
import com.rupeshbhurke.househunter.core.data.db.AttachmentEntity
import com.rupeshbhurke.househunter.core.data.model.AttachmentKind
import com.rupeshbhurke.househunter.core.media.mediaFile
import com.rupeshbhurke.househunter.core.util.Coordinates
import com.rupeshbhurke.househunter.core.util.FieldFormat
import com.rupeshbhurke.househunter.ui.components.VideoPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    onBack: () -> Unit,
    viewModel: MediaViewerViewModel = hiltViewModel(),
) {
    val loaded by viewModel.items.collectAsStateWithLifecycle()
    val items = loaded ?: return
    if (items.isEmpty()) {
        // The last item was deleted.
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val pager = rememberPagerState(initialPage = viewModel.startIndex.coerceIn(0, items.lastIndex)) { items.size }
    val current = items[pager.currentPage.coerceIn(0, items.lastIndex)]
    var editingCaption by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${pager.currentPage + 1} / ${items.size}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth(), key = { items[it].id }) { page ->
                MediaPage(items[page], active = pager.currentPage == page)
            }
            MediaDetails(current, onEditCaption = { editingCaption = true })
        }
    }

    if (editingCaption) {
        CaptionDialog(
            initial = current.caption.orEmpty(),
            onConfirm = { viewModel.setCaption(current.id, it); editingCaption = false },
            onDismiss = { editingCaption = false },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.media_delete_title)) },
            text = { Text(stringResource(R.string.media_delete_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete(current) }) {
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
private fun MediaPage(item: AttachmentEntity, active: Boolean) {
    val context = LocalContext.current
    val file = remember(item.filePath) { context.mediaFile(item.filePath) }
    when (item.kind) {
        AttachmentKind.PHOTO -> AsyncImage(
            model = file,
            contentDescription = item.caption,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        AttachmentKind.VIDEO -> VideoPlayer(file, active, Modifier.fillMaxSize())
        AttachmentKind.AUDIO -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.media_audio_unsupported))
        }
    }
}

@Composable
private fun MediaDetails(item: AttachmentEntity, onEditCaption: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                item.caption ?: stringResource(R.string.media_add_caption),
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.caption == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 48.dp).align(Alignment.CenterStart),
            )
            IconButton(onClick = onEditCaption, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.media_edit_caption))
            }
        }
        val place = if (item.lat != null && item.lng != null) " · ${Coordinates.format(item.lat, item.lng)}" else ""
        Text(
            FieldFormat.dateTime(item.capturedAt) + place,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CaptionDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.media_edit_caption)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.media_caption_label)) },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
