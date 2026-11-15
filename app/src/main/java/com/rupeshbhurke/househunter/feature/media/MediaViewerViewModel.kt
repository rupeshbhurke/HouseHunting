package com.rupeshbhurke.househunter.feature.media

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rupeshbhurke.househunter.core.data.db.AttachmentEntity
import com.rupeshbhurke.househunter.core.data.repo.MediaRepository
import com.rupeshbhurke.househunter.navigation.MediaViewerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val media: MediaRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<MediaViewerRoute>()
    val startIndex = route.startIndex

    val items: StateFlow<List<AttachmentEntity>?> = media.observeMedia(route.entryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setCaption(id: Long, caption: String) {
        viewModelScope.launch { media.setCaption(id, caption) }
    }

    fun delete(attachment: AttachmentEntity) {
        viewModelScope.launch { media.delete(attachment) }
    }
}
