package com.rupeshbhurke.househunter.feature.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rupeshbhurke.househunter.core.data.db.ReferencePointEntity
import com.rupeshbhurke.househunter.core.data.model.NoteKind
import com.rupeshbhurke.househunter.core.data.repo.EntryDetail
import com.rupeshbhurke.househunter.core.data.repo.EntryRepository
import com.rupeshbhurke.househunter.core.data.repo.NoteItem
import com.rupeshbhurke.househunter.core.data.repo.NoteRepository
import com.rupeshbhurke.househunter.core.data.repo.ReferencePointRepository
import com.rupeshbhurke.househunter.core.location.LocationClient
import com.rupeshbhurke.househunter.core.media.ImportResult
import com.rupeshbhurke.househunter.core.media.MediaFiles
import com.rupeshbhurke.househunter.core.media.MediaImporter
import com.rupeshbhurke.househunter.navigation.EntryDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.net.Uri
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EntryRepository,
    private val noteRepository: NoteRepository,
    private val mediaFiles: MediaFiles,
    private val location: LocationClient,
    private val importer: MediaImporter,
    references: ReferencePointRepository,
) : ViewModel() {
    val entryId = savedStateHandle.toRoute<EntryDetailRoute>().entryId

    val detail: StateFlow<EntryDetail?> = repository.observeDetail(entryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val referencePoints: StateFlow<List<ReferencePointEntity>> = references.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notes: StateFlow<List<NoteItem>> = noteRepository.observeNotes(entryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing

    private val importResults = Channel<ImportResult>(Channel.BUFFERED)

    /** One result per finished import, for the screen to announce. */
    val importResult: Flow<ImportResult> = importResults.receiveAsFlow()

    fun importMedia(uris: List<Uri>) {
        if (uris.isEmpty() || _importing.value) return
        _importing.value = true
        viewModelScope.launch {
            try {
                importResults.send(importer.import(entryId, uris))
            } finally {
                _importing.value = false
            }
        }
    }

    fun newAudioFile(): File = mediaFiles.newFile(entryId, "m4a")

    fun addTextNote(text: String) {
        viewModelScope.launch { noteRepository.addText(entryId, text, location.lastKnownFix()) }
    }

    fun addAudioNote(file: File, durationMs: Long) {
        viewModelScope.launch {
            noteRepository.addRecording(entryId, file, NoteKind.AUDIO, durationMs, location.lastKnownFix())
        }
    }

    fun updateTextNote(noteId: Long, text: String) {
        viewModelScope.launch { noteRepository.updateText(noteId, text) }
    }

    fun deleteNote(item: NoteItem) {
        viewModelScope.launch { noteRepository.delete(item) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteEntry(entryId)
            onDeleted()
        }
    }
}
