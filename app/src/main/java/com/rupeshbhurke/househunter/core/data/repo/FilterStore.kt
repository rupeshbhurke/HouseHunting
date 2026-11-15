package com.rupeshbhurke.househunter.core.data.repo

import com.rupeshbhurke.househunter.core.data.model.EntryFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Remembers each collection's current search and filters while the app is open, shared by the list and map. */
@Singleton
class FilterStore @Inject constructor() {
    private val filters = MutableStateFlow<Map<Long, EntryFilter>>(emptyMap())

    fun observe(collectionId: Long): Flow<EntryFilter> =
        filters.map { it[collectionId] ?: EntryFilter() }.distinctUntilChanged()

    fun current(collectionId: Long): EntryFilter = filters.value[collectionId] ?: EntryFilter()

    fun set(collectionId: Long, filter: EntryFilter) {
        filters.update { it + (collectionId to filter) }
    }

    fun update(collectionId: Long, transform: (EntryFilter) -> EntryFilter) {
        filters.update { it + (collectionId to transform(it[collectionId] ?: EntryFilter())) }
    }
}
