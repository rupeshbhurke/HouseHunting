package com.rupeshbhurke.househunter.feature.map

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rupeshbhurke.househunter.R
import com.rupeshbhurke.househunter.core.data.db.ReferencePointEntity
import com.rupeshbhurke.househunter.core.data.model.EntryFilter
import com.rupeshbhurke.househunter.core.data.model.EntryFilterer
import com.rupeshbhurke.househunter.core.data.model.NearFilter
import com.rupeshbhurke.househunter.core.data.model.SortOption
import com.rupeshbhurke.househunter.core.data.repo.CollectionRepository
import com.rupeshbhurke.househunter.core.data.repo.EntryRepository
import com.rupeshbhurke.househunter.core.data.repo.EntrySummary
import com.rupeshbhurke.househunter.core.data.repo.FilterStore
import com.rupeshbhurke.househunter.core.data.repo.ReferencePointRepository
import com.rupeshbhurke.househunter.core.location.LocationClient
import com.rupeshbhurke.househunter.navigation.MapRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

const val DEFAULT_RADIUS_KM = 3.0

@HiltViewModel
class MapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    collections: CollectionRepository,
    entries: EntryRepository,
    private val filters: FilterStore,
    private val references: ReferencePointRepository,
    private val location: LocationClient,
) : ViewModel() {
    private val collectionId = savedStateHandle.toRoute<MapRoute>().collectionId

    val collectionName: StateFlow<String> = collections.observeCollection(collectionId)
        .map { it?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val filter: StateFlow<EntryFilter> = filters.observe(collectionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), filters.current(collectionId))

    /** The flats that pass the current search, filters and near-search and have a position to show. */
    val flats: StateFlow<List<EntrySummary>> =
        combine(entries.observeSummaries(collectionId), filters.observe(collectionId)) { rows, f ->
            EntryFilterer.apply(rows, f).filter { it.entry.lat != null && it.entry.lng != null }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val referencePoints: StateFlow<List<ReferencePointEntity>> = references.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _locating = MutableStateFlow(false)
    val locating: StateFlow<Boolean> = _locating

    private val messages = Channel<Int>(Channel.BUFFERED)

    /** String resources for things worth telling the user (e.g. no location fix). */
    val message: Flow<Int> = messages.receiveAsFlow()

    /** Sets the centre of the near-search. The first time, flats are also sorted nearest first. */
    fun setCenter(lat: Double, lng: Double, label: String) {
        filters.update(collectionId) { f ->
            val previous = f.near
            f.copy(
                near = NearFilter(lat, lng, if (previous == null) DEFAULT_RADIUS_KM else previous.radiusKm, label),
                sort = if (previous == null) SortOption.DISTANCE else f.sort,
            )
        }
    }

    /** A null radius keeps every flat and only uses the centre for sorting and distances. */
    fun setRadius(km: Double?) {
        filters.update(collectionId) { f -> f.copy(near = f.near?.copy(radiusKm = km)) }
    }

    fun clearNear() {
        filters.update(collectionId) { f ->
            f.copy(near = null, sort = if (f.sort == SortOption.DISTANCE) SortOption.RECENT else f.sort)
        }
    }

    fun useMyLocation(label: String) {
        if (_locating.value) return
        _locating.value = true
        viewModelScope.launch {
            val fix = location.currentFix()
            _locating.value = false
            if (fix == null) {
                messages.send(R.string.location_error_no_fix)
            } else {
                setCenter(fix.lat, fix.lng, label)
            }
        }
    }

    fun permissionDenied() = show(R.string.location_error_permission)

    fun show(@StringRes messageRes: Int) {
        viewModelScope.launch { messages.send(messageRes) }
    }

    fun selectPoint(point: ReferencePointEntity) = setCenter(point.lat, point.lng, point.name)

    fun savePoint(name: String) {
        val center = filters.current(collectionId).near ?: return
        viewModelScope.launch { references.add(name, center.lat, center.lng) }
    }

    fun deletePoint(point: ReferencePointEntity) {
        viewModelScope.launch { references.delete(point.id) }
    }
}
