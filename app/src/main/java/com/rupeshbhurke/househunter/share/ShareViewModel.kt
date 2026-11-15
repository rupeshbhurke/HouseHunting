package com.rupeshbhurke.househunter.share

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rupeshbhurke.househunter.core.data.db.CollectionWithCount
import com.rupeshbhurke.househunter.core.data.repo.CollectionRepository
import com.rupeshbhurke.househunter.core.data.repo.EntryRepository
import com.rupeshbhurke.househunter.core.data.repo.EntrySummary
import com.rupeshbhurke.househunter.core.media.ImportResult
import com.rupeshbhurke.househunter.core.media.MediaImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ShareStep {
    data object ChooseCollection : ShareStep
    data class ChooseFlat(val collectionId: Long, val collectionName: String) : ShareStep
    data object Importing : ShareStep
    data class Done(val entryId: Long, val flatTitle: String, val result: ImportResult) : ShareStep
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShareViewModel @Inject constructor(
    private val collectionsRepo: CollectionRepository,
    private val entriesRepo: EntryRepository,
    private val importer: MediaImporter,
) : ViewModel() {
    var uris: List<Uri> = emptyList()
        private set

    private val _step = MutableStateFlow<ShareStep>(ShareStep.ChooseCollection)
    val step: StateFlow<ShareStep> = _step

    val collections: StateFlow<List<CollectionWithCount>?> = collectionsRepo.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val selectedCollection = MutableStateFlow<Long?>(null)

    /** The flats of the chosen collection; null while none is chosen. */
    val flats: StateFlow<List<EntrySummary>?> = selectedCollection
        .flatMapLatest { id -> if (id == null) flowOf(null) else entriesRepo.observeSummaries(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setUris(list: List<Uri>) {
        if (uris.isEmpty()) uris = list
    }

    fun chooseCollection(collection: CollectionWithCount) {
        selectedCollection.value = collection.collection.id
        _step.value = ShareStep.ChooseFlat(collection.collection.id, collection.collection.name)
    }

    fun createCollection(name: String) {
        viewModelScope.launch {
            val id = collectionsRepo.create(name)
            selectedCollection.value = id
            _step.value = ShareStep.ChooseFlat(id, name.trim())
        }
    }

    fun back() {
        selectedCollection.value = null
        _step.value = ShareStep.ChooseCollection
    }

    fun importIntoFlat(entryId: Long, title: String) {
        _step.value = ShareStep.Importing
        viewModelScope.launch {
            val result = importer.import(entryId, uris)
            _step.value = ShareStep.Done(entryId, title, result)
        }
    }

    fun importIntoNewFlat(collectionId: Long) {
        _step.value = ShareStep.Importing
        viewModelScope.launch {
            val entryId = entriesRepo.createEntry(collectionId)
            val title = entriesRepo.getDetail(entryId).entry.title
            val result = importer.import(entryId, uris)
            _step.value = ShareStep.Done(entryId, title, result)
        }
    }
}
