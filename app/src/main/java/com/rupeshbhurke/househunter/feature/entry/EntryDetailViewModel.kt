package com.rupeshbhurke.househunter.feature.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rupeshbhurke.househunter.core.data.model.NoteKind
import com.rupeshbhurke.househunter.core.data.repo.EntryDetail
import com.rupeshbhurke.househunter.core.data.repo.EntryRepository
import com.rupeshbhurke.househunter.core.data.repo.NoteItem
import com.rupeshbhurke.househunter.core.data.repo.NoteRepository
import com.rupeshbhurke.househunter.core.location.LocationClient
import com.rupeshbhurke.househunter.core.media.MediaFiles
import com.rupeshbhurke.househunter.navigation.EntryDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EntryRepository,
    private val noteRepository: NoteRepository,
    private val mediaFiles: MediaFiles,
    private val location: LocationClient,
) : ViewModel() {
    val entryId = savedStateHandle.toRoute<EntryDetailRoute>().entryId

    val detail: StateFlow<EntryDetail?> = repository.observeDetail(entryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val notes: StateFlow<List<NoteItem>> = noteRepository.observeNotes(entryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
