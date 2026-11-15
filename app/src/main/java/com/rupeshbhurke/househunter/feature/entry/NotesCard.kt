package com.rupeshbhurke.househunter.feature.entry

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.rupeshbhurke.househunter.R
import com.rupeshbhurke.househunter.core.data.model.NoteKind
import com.rupeshbhurke.househunter.core.data.repo.NoteItem
import com.rupeshbhurke.househunter.core.media.AudioRecorder
import com.rupeshbhurke.househunter.core.media.mediaFile
import com.rupeshbhurke.househunter.core.util.FieldFormat
import com.rupeshbhurke.househunter.ui.components.MediaThumbnail
import com.rupeshbhurke.househunter.ui.components.VideoPlayer
import kotlinx.coroutines.delay
import java.io.File

/** What the notes card can ask the screen to do. */
class NotesActions(
    val newAudioFile: () -> File,
    val onAddText: (String) -> Unit,
    val onSaveAudio: (File, Long) -> Unit,
    val onVideoNote: () -> Unit,
    val onEditText: (noteId: Long, text: String) -> Unit,
    val onDelete: (NoteItem) -> Unit,
)

private sealed interface NoteDialog {
    data class Text(val existing: NoteItem?) : NoteDialog
    data object Audio : NoteDialog
    data class Video(val file: File) : NoteDialog
    data class ConfirmDelete(val item: NoteItem) : NoteDialog
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesCard(notes: List<NoteItem>, actions: NotesActions) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<NoteDialog?>(null) }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) dialog = NoteDialog.Audio
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.notes_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { dialog = NoteDialog.Text(null) }) {
                    Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null)
                    Text(stringResource(R.string.notes_add_text), modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) dialog = NoteDialog.Audio else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Icon(Icons.Filled.Mic, contentDescription = null)
                    Text(stringResource(R.string.notes_add_audio), modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = actions.onVideoNote) {
                    Icon(Icons.Filled.Videocam, contentDescription = null)
                    Text(stringResource(R.string.notes_add_video), modifier = Modifier.padding(start = 8.dp))
                }
            }
            if (notes.isEmpty()) {
                Text(
                    stringResource(R.string.notes_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            notes.forEach { item ->
                NoteRow(
                    item = item,
                    onOpen = {
                        when (item.note.kind) {
                            NoteKind.TEXT -> dialog = NoteDialog.Text(item)
                            NoteKind.VIDEO -> item.attachment?.let { dialog = NoteDialog.Video(context.mediaFile(it.filePath)) }
                            NoteKind.AUDIO -> Unit
                        }
                    },
                    onDelete = { dialog = NoteDialog.ConfirmDelete(item) },
                )
            }
        }
    }

    when (val d = dialog) {
        is NoteDialog.Text -> TextNoteDialog(
            initial = d.existing?.note?.text.orEmpty(),
            onSave = { text ->
                if (d.existing == null) actions.onAddText(text) else actions.onEditText(d.existing.note.id, text)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        NoteDialog.Audio -> AudioRecordDialog(
            newFile = actions.newAudioFile,
            onSaved = { file, durationMs -> actions.onSaveAudio(file, durationMs); dialog = null },
            onDismiss = { dialog = null },
        )
        is NoteDialog.Video -> Dialog(
            onDismissRequest = { dialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            VideoPlayer(d.file, active = true, modifier = Modifier.fillMaxSize())
        }
        is NoteDialog.ConfirmDelete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.notes_delete_title)) },
            text = { Text(stringResource(R.string.notes_delete_body)) },
            confirmButton = {
                TextButton(onClick = { actions.onDelete(d.item); dialog = null }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
        null -> Unit
    }
}

@Composable
private fun NoteRow(item: NoteItem, onOpen: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
            when (item.note.kind) {
                NoteKind.TEXT -> Text(
                    item.note.text.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
                NoteKind.AUDIO -> item.attachment?.let { att ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AudioPlayerButton(context.mediaFile(att.filePath))
                        Text(
                            stringResource(R.string.notes_audio_label, FieldFormat.duration(att.durationMs ?: 0)),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                NoteKind.VIDEO -> item.attachment?.let { MediaThumbnail(it, size = 80.dp) }
            }
            Text(
                FieldFormat.dateTime(item.note.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
        }
    }
}

@Composable
private fun AudioPlayerButton(file: File) {
    var playing by remember(file) { mutableStateOf(false) }
    var prepared by remember(file) { mutableStateOf(false) }
    val player = remember(file) { MediaPlayer() }
    DisposableEffect(player) { onDispose { player.release() } }

    IconButton(onClick = {
        if (playing) {
            player.pause()
            playing = false
        } else {
            try {
                if (!prepared) {
                    player.setDataSource(file.path)
                    player.prepare()
                    player.setOnCompletionListener {
                        playing = false
                        it.seekTo(0)
                    }
                    prepared = true
                }
                player.start()
                playing = true
            } catch (_: Exception) {
                playing = false
            }
        }
    }) {
        Icon(
            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = stringResource(if (playing) R.string.notes_pause else R.string.notes_play),
        )
    }
}

@Composable
private fun TextNoteDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notes_text_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.notes_text_label)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Records straight away and saves when stopped; cancelling throws the recording away. */
@Composable
private fun AudioRecordDialog(newFile: () -> File, onSaved: (File, Long) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val recorder = remember { AudioRecorder(context) }
    val file = remember { newFile() }
    var recording by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        recording = recorder.start(file)
        failed = !recording
    }
    LaunchedEffect(recording) {
        while (recording) {
            delay(1_000)
            elapsedSec++
        }
    }
    DisposableEffect(Unit) { onDispose { if (recorder.isRecording) recorder.cancel(file) } }

    AlertDialog(
        onDismissRequest = { recorder.cancel(file); onDismiss() },
        title = { Text(stringResource(R.string.notes_audio_title)) },
        text = {
            Text(
                if (failed) stringResource(R.string.notes_audio_failed) else "● ${FieldFormat.duration(elapsedSec * 1_000L)}",
                style = MaterialTheme.typography.headlineSmall,
                color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        },
        confirmButton = {
            TextButton(
                enabled = recording,
                onClick = {
                    val durationMs = recorder.stop()
                    recording = false
                    if (durationMs != null) onSaved(file, durationMs) else { file.delete(); onDismiss() }
                },
            ) { Text(stringResource(R.string.notes_audio_stop)) }
        },
        dismissButton = {
            TextButton(onClick = { recorder.cancel(file); onDismiss() }) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
