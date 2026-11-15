package com.rupeshbhurke.househunter.feature.capture

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rupeshbhurke.househunter.core.data.model.AttachmentKind
import com.rupeshbhurke.househunter.core.data.model.MediaSource
import com.rupeshbhurke.househunter.core.data.model.NoteKind
import com.rupeshbhurke.househunter.core.data.repo.MediaRepository
import com.rupeshbhurke.househunter.core.data.repo.NoteRepository
import com.rupeshbhurke.househunter.core.location.Fix
import com.rupeshbhurke.househunter.core.location.LocationClient
import com.rupeshbhurke.househunter.core.media.MediaFiles
import com.rupeshbhurke.househunter.navigation.CaptureRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val media: MediaRepository,
    private val notes: NoteRepository,
    private val mediaFiles: MediaFiles,
    private val location: LocationClient,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<CaptureRoute>()
    val entryId = route.entryId

    /** True when the screen records a video note instead of adding media to the flat. */
    val videoNote = route.videoNote

    private var fix: Fix? = null

    private val _saved = MutableStateFlow(0)
    val saved: StateFlow<Int> = _saved

    init {
        // Tag captures with where they were taken: take what the device knows now, refine in the background.
        viewModelScope.launch {
            fix = location.lastKnownFix()
            location.currentFix()?.let { fix = it }
        }
    }

    fun newFile(extension: String): File = mediaFiles.newFile(entryId, extension)

    fun onPhotoSaved(file: File) = save(file, AttachmentKind.PHOTO, "image/jpeg", durationMs = null)

    fun onVideoSaved(file: File, durationMs: Long) = save(file, AttachmentKind.VIDEO, "video/mp4", durationMs)

    private fun save(file: File, kind: AttachmentKind, mime: String, durationMs: Long?) {
        val where = fix
        // Not tied to the screen's lifetime: a recording finalised as the screen closes must still be kept.
        viewModelScope.launch(NonCancellable) {
            if (videoNote && kind == AttachmentKind.VIDEO) {
                notes.addRecording(entryId, file, NoteKind.VIDEO, durationMs ?: 0, where)
            } else media.addFile(
                entryId = entryId,
                file = file,
                kind = kind,
                mime = mime,
                source = MediaSource.CAPTURED,
                fix = where,
                durationMs = durationMs,
            )
            _saved.update { it + 1 }
        }
    }
}
