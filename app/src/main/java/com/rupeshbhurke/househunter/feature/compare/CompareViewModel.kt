package com.rupeshbhurke.househunter.feature.compare

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rupeshbhurke.househunter.core.data.repo.EntryDetail
import com.rupeshbhurke.househunter.core.data.repo.EntryRepository
import com.rupeshbhurke.househunter.navigation.CompareRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompareViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    entries: EntryRepository,
) : ViewModel() {
    private val _flats = MutableStateFlow<List<EntryDetail>?>(null)

    /** The chosen flats in the order they were picked; null while loading. */
    val flats: StateFlow<List<EntryDetail>?> = _flats

    init {
        val ids = savedStateHandle.toRoute<CompareRoute>().entryIds
        viewModelScope.launch {
            // A flat deleted in the meantime is simply left out.
            _flats.value = ids.mapNotNull { id -> runCatching { entries.getDetail(id) }.getOrNull() }
        }
    }
}
