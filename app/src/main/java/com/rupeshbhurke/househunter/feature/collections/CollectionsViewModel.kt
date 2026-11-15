package com.rupeshbhurke.househunter.feature.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rupeshbhurke.househunter.core.data.db.CollectionWithCount
import com.rupeshbhurke.househunter.core.data.repo.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val repository: CollectionRepository,
) : ViewModel() {

    /** Null until the first database emission, so the empty state doesn't flash on launch. */
    val collections: StateFlow<List<CollectionWithCount>?> = repository.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun create(name: String) {
        viewModelScope.launch { repository.create(name) }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { repository.rename(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
