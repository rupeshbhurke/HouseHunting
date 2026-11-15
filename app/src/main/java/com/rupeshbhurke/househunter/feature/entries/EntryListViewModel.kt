package com.rupeshbhurke.househunter.feature.entries

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rupeshbhurke.househunter.core.data.db.ReferencePointEntity
import com.rupeshbhurke.househunter.core.data.model.EntryFilter
import com.rupeshbhurke.househunter.core.data.model.EntryFilterer
import com.rupeshbhurke.househunter.core.data.model.FieldDef
import com.rupeshbhurke.househunter.core.data.repo.CollectionRepository
import com.rupeshbhurke.househunter.core.data.repo.EntryRepository
import com.rupeshbhurke.househunter.core.data.repo.EntrySummary
import com.rupeshbhurke.househunter.core.data.repo.FilterStore
import com.rupeshbhurke.househunter.core.data.repo.ReferencePointRepository
import com.rupeshbhurke.househunter.navigation.EntryListRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The flats to show, plus how many there are before filtering (to tell "empty" from "no match"). */
data class ListState(val total: Int, val visible: List<EntrySummary>)

@HiltViewModel
class EntryListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    collections: CollectionRepository,
    private val entries: EntryRepository,
    private val filters: FilterStore,
    references: ReferencePointRepository,
) : ViewModel() {
    val collectionId = savedStateHandle.toRoute<EntryListRoute>().collectionId

    val collectionName: StateFlow<String> = collections.observeCollection(collectionId)
        .map { it?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val filter: StateFlow<EntryFilter> = filters.observe(collectionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), filters.current(collectionId))

    /** Every field in the collection, for the filter sheet. */
    val fields: StateFlow<List<FieldDef>> = entries.observeFields(collectionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val referencePoints: StateFlow<List<ReferencePointEntity>> = references.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Null until the first database emission, so the empty state doesn't flash. */
    val state: StateFlow<ListState?> = combine(entries.observeSummaries(collectionId), filters.observe(collectionId)) { rows, f ->
        ListState(total = rows.size, visible = EntryFilterer.apply(rows, f))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setFilter(filter: EntryFilter) = filters.set(collectionId, filter)

    fun addEntry(onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(entries.createEntry(collectionId)) }
    }
}
