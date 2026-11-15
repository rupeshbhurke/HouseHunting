package com.rupeshbhurke.househunter.feature.entries

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rupeshbhurke.househunter.core.data.repo.CollectionRepository
import com.rupeshbhurke.househunter.core.data.repo.EntryRepository
import com.rupeshbhurke.househunter.core.data.repo.EntrySummary
import com.rupeshbhurke.househunter.navigation.EntryListRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EntryListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val collections: CollectionRepository,
    private val entries: EntryRepository,
) : ViewModel() {
    private val collectionId = savedStateHandle.toRoute<EntryListRoute>().collectionId

    val collectionName: StateFlow<String> = collections.observeCollection(collectionId)
        .map { it?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Null until the first database emission, so the empty state doesn't flash. */
    val entryList: StateFlow<List<EntrySummary>?> = entries.observeSummaries(collectionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addEntry(onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(entries.createEntry(collectionId)) }
    }
}
