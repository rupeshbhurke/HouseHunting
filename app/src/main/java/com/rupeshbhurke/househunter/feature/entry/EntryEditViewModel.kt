package com.rupeshbhurke.househunter.feature.entry

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rupeshbhurke.househunter.R
import com.rupeshbhurke.househunter.core.data.db.ContactEntity
import com.rupeshbhurke.househunter.core.data.model.ContactRole
import com.rupeshbhurke.househunter.core.data.model.EntryStatus
import com.rupeshbhurke.househunter.core.data.model.FieldType
import com.rupeshbhurke.househunter.core.data.model.HouseHuntTemplate
import com.rupeshbhurke.househunter.core.data.repo.EntryDetail
import com.rupeshbhurke.househunter.core.data.repo.EntryRepository
import com.rupeshbhurke.househunter.core.location.AddressResolver
import com.rupeshbhurke.househunter.core.location.LocationClient
import com.rupeshbhurke.househunter.core.util.FieldFormat
import com.rupeshbhurke.househunter.core.util.LatLng
import com.rupeshbhurke.househunter.navigation.EntryEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the user has typed so far; [values] holds raw input by field key, blanks removed. */
data class EditDraft(
    val title: String,
    val status: EntryStatus,
    val rating: Int?,
    val values: Map<String, String>,
)

data class EditUiState(
    val detail: EntryDetail,
    val draft: EditDraft,
    val dirty: Boolean,
)

sealed interface LocationStatus {
    data object Idle : LocationStatus
    data object Fetching : LocationStatus
    data class Failed(@StringRes val messageRes: Int) : LocationStatus
}

@HiltViewModel
class EntryEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EntryRepository,
    private val locationClient: LocationClient,
    private val addressResolver: AddressResolver,
) : ViewModel() {
    private val entryId = savedStateHandle.toRoute<EntryEditRoute>().entryId

    private val initial = MutableStateFlow<EditDraft?>(null)
    private val draft = MutableStateFlow<EditDraft?>(null)

    val uiState: StateFlow<EditUiState?> = combine(
        repository.observeDetail(entryId),
        initial,
        draft,
    ) { detail, start, current ->
        if (start == null || current == null) null else EditUiState(detail, current, dirty = current != start)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _locationStatus = MutableStateFlow<LocationStatus>(LocationStatus.Idle)
    val locationStatus: StateFlow<LocationStatus> = _locationStatus

    init {
        viewModelScope.launch {
            val start = repository.getDetail(entryId).toDraft()
            initial.value = start
            draft.value = start
        }
    }

    fun setTitle(title: String) = draft.update { it?.copy(title = title) }

    fun setStatus(status: EntryStatus) = draft.update { it?.copy(status = status) }

    fun setRating(rating: Int?) = draft.update { it?.copy(rating = rating) }

    fun setValue(key: String, value: String) = draft.update { current ->
        current?.copy(values = if (value.isBlank()) current.values - key else current.values + (key to value))
    }

    fun save(onSaved: () -> Unit) {
        val current = draft.value ?: return
        viewModelScope.launch {
            repository.saveEntry(entryId, current.title, current.status, current.rating, current.values)
            onSaved()
        }
    }

    /** Takes a fix from the device (the caller has already secured the location permission). */
    fun captureLocation() {
        if (_locationStatus.value == LocationStatus.Fetching) return
        _locationStatus.value = LocationStatus.Fetching
        viewModelScope.launch {
            val fix = locationClient.currentFix()
            if (fix == null) {
                _locationStatus.value = LocationStatus.Failed(R.string.location_error_no_fix)
                return@launch
            }
            repository.setLocation(entryId, fix.lat, fix.lng, fix.accuracyM)
            _locationStatus.value = LocationStatus.Idle
            fillAddressIfBlank(fix.lat, fix.lng)
        }
    }

    fun setManualLocation(point: LatLng) {
        viewModelScope.launch {
            repository.setLocation(entryId, point.lat, point.lng, accuracyM = null)
            _locationStatus.value = LocationStatus.Idle
            fillAddressIfBlank(point.lat, point.lng)
        }
    }

    fun clearLocation() {
        viewModelScope.launch { repository.clearLocation(entryId) }
    }

    fun permissionDenied() {
        _locationStatus.value = LocationStatus.Failed(R.string.location_error_permission)
    }

    /** The street address goes into the draft, not the database, so saving the form keeps it. */
    private suspend fun fillAddressIfBlank(lat: Double, lng: Double) {
        val address = addressResolver.resolve(lat, lng) ?: return
        draft.update { current ->
            if (current == null || !current.values[HouseHuntTemplate.Keys.ADDRESS].isNullOrBlank()) current
            else current.copy(values = current.values + (HouseHuntTemplate.Keys.ADDRESS to address))
        }
    }

    fun addField(label: String, type: FieldType, options: String, addToAll: Boolean) {
        val parsed = options.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        viewModelScope.launch { repository.addCustomField(entryId, label, type, parsed, addToAll) }
    }

    fun addContact(role: ContactRole, name: String, phone: String) {
        viewModelScope.launch { repository.addContact(entryId, role, name, phone) }
    }

    fun updateContact(contact: ContactEntity, role: ContactRole, name: String, phone: String) {
        viewModelScope.launch {
            repository.updateContact(contact.copy(role = role, name = name.trim(), phone = phone.trim()))
        }
    }

    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch { repository.deleteContact(contact) }
    }

    private fun EntryDetail.toDraft() = EditDraft(
        title = entry.title,
        status = entry.status,
        rating = entry.rating,
        values = fields
            .associate { it.def.key to FieldFormat.editable(it.def.type, it.text, it.number) }
            .filterValues { it.isNotBlank() },
    )
}
