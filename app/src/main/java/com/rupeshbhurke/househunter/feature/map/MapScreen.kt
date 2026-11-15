package com.rupeshbhurke.househunter.feature.map

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.rupeshbhurke.househunter.BuildConfig
import com.rupeshbhurke.househunter.R
import com.rupeshbhurke.househunter.core.data.db.ReferencePointEntity
import com.rupeshbhurke.househunter.core.data.model.EntryStatus
import com.rupeshbhurke.househunter.core.data.model.NearFilter
import com.rupeshbhurke.househunter.core.data.repo.EntrySummary
import com.rupeshbhurke.househunter.core.util.FieldFormat
import com.rupeshbhurke.househunter.core.util.Geo
import com.rupeshbhurke.househunter.ui.components.StatusChip

private const val PLACEHOLDER_KEY = "DEFAULT_API_KEY"
private const val MIN_RADIUS_KM = 0.5f
private const val MAX_RADIUS_KM = 20f

private fun statusHue(status: EntryStatus) = when (status) {
    EntryStatus.NEW -> BitmapDescriptorFactory.HUE_AZURE
    EntryStatus.SHORTLISTED -> BitmapDescriptorFactory.HUE_GREEN
    EntryStatus.VISIT_AGAIN -> BitmapDescriptorFactory.HUE_ORANGE
    EntryStatus.REJECTED -> BitmapDescriptorFactory.HUE_RED
}

private fun zoomFor(radiusKm: Double?) = when {
    radiusKm == null -> 12f
    radiusKm <= 1.0 -> 15f
    radiusKm <= 3.0 -> 14f
    radiusKm <= 7.0 -> 13f
    radiusKm <= 15.0 -> 12f
    else -> 11f
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val name by viewModel.collectionName.collectAsStateWithLifecycle()
    val flats by viewModel.flats.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val points by viewModel.referencePoints.collectAsStateWithLifecycle()
    val locating by viewModel.locating.collectAsStateWithLifecycle()
    val near = filter.near
    val keyMissing = BuildConfig.MAPS_API_KEY == PLACEHOLDER_KEY

    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var mapLoaded by remember { mutableStateOf(false) }
    var fitted by rememberSaveable { mutableStateOf(false) }
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.5937, 78.9629), 4f)
    }
    val snackbar = remember { SnackbarHostState() }
    val myLocationLabel = stringResource(R.string.map_my_location)
    val droppedPinLabel = stringResource(R.string.map_dropped_pin)

    LaunchedEffect(viewModel) { viewModel.message.collect { snackbar.showSnackbar(context.getString(it)) } }

    // Show all the flats once, the first time the map and the list are both ready.
    LaunchedEffect(mapLoaded, flats.isNotEmpty()) {
        if (mapLoaded && !fitted && flats.isNotEmpty()) {
            fitted = true
            val points = flats.map { LatLng(it.entry.lat!!, it.entry.lng!!) }
            if (points.size == 1) {
                camera.animate(CameraUpdateFactory.newLatLngZoom(points.first(), 15f))
            } else {
                val bounds = LatLngBounds.builder().apply { points.forEach(::include) }.build()
                camera.animate(CameraUpdateFactory.newLatLngBounds(bounds, 120))
            }
        }
    }
    LaunchedEffect(near?.lat, near?.lng) {
        near?.let { camera.animate(CameraUpdateFactory.newLatLngZoom(LatLng(it.lat, it.lng), zoomFor(it.radiusKm))) }
    }

    val locationPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) viewModel.useMyLocation(myLocationLabel) else viewModel.permissionDenied()
    }
    val placeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val place = Autocomplete.getPlaceFromIntent(data)
            place.location?.let { viewModel.setCenter(it.latitude, it.longitude, place.displayName ?: place.formattedAddress ?: droppedPinLabel) }
        }
    }

    var savingPoint by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camera,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
                onMapLoaded = { mapLoaded = true },
                onMapLongClick = { viewModel.setCenter(it.latitude, it.longitude, droppedPinLabel) },
                onMapClick = { selectedId = null },
            ) {
                flats.forEach { flat ->
                    val position = LatLng(flat.entry.lat!!, flat.entry.lng!!)
                    key(flat.entry.id) {
                        Marker(
                            state = rememberMarkerState(key = "flat-${flat.entry.id}", position = position),
                            title = flat.entry.title,
                            icon = BitmapDescriptorFactory.defaultMarker(statusHue(flat.entry.status)),
                            onClick = { selectedId = flat.entry.id; false },
                        )
                    }
                }
                near?.let { center ->
                    val centerPoint = LatLng(center.lat, center.lng)
                    Marker(
                        state = rememberMarkerState(key = "near", position = centerPoint),
                        title = center.label,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET),
                    )
                    center.radiusKm?.let { km ->
                        Circle(
                            center = centerPoint,
                            radius = km * 1000,
                            strokeColor = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4f,
                            fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        )
                    }
                }
            }

            if (keyMissing) {
                Card(
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        stringResource(R.string.map_key_missing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                flats.firstOrNull { it.entry.id == selectedId }?.let { flat ->
                    SelectedFlatCard(flat, near, onOpen = { onOpenEntry(flat.entry.id) })
                }
                NearPanel(
                    near = near,
                    matching = flats.size,
                    points = points,
                    locating = locating,
                    onMyLocation = {
                        val granted = locationPermissions.any {
                            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                        }
                        if (granted) viewModel.useMyLocation(myLocationLabel) else permissionLauncher.launch(locationPermissions)
                    },
                    onSearchPlace = {
                        if (keyMissing) {
                            // Places needs a real key; say so instead of failing silently.
                            viewModel.show(R.string.map_key_needed_for_search)
                        } else {
                            if (!Places.isInitialized()) Places.initializeWithNewPlacesApiEnabled(context, BuildConfig.MAPS_API_KEY)
                            val intent = Autocomplete.IntentBuilder(
                                AutocompleteActivityMode.OVERLAY,
                                listOf(Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS, Place.Field.LOCATION),
                            ).build(context)
                            placeLauncher.launch(intent)
                        }
                    },
                    onSavePoint = { savingPoint = true },
                    onClear = viewModel::clearNear,
                    onRadius = viewModel::setRadius,
                    onSelectPoint = viewModel::selectPoint,
                    onDeletePoint = viewModel::deletePoint,
                )
            }
        }
    }

    if (savingPoint) {
        var pointName by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { savingPoint = false },
            title = { Text(stringResource(R.string.map_save_point_title)) },
            text = {
                OutlinedTextField(
                    value = pointName,
                    onValueChange = { pointName = it },
                    label = { Text(stringResource(R.string.map_save_point_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.savePoint(pointName); savingPoint = false }, enabled = pointName.isNotBlank()) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = { TextButton(onClick = { savingPoint = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun SelectedFlatCard(flat: EntrySummary, near: NearFilter?, onOpen: () -> Unit) {
    val subtitle = listOfNotNull(
        flat.bhk,
        flat.price?.let(FieldFormat::money),
        near?.distanceKm(flat)?.let { "${Geo.format(it)} · ${near.label}" },
    ).joinToString("  •  ")
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(flat.entry.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusChip(flat.entry.status)
            }
            Button(onClick = onOpen) { Text(stringResource(R.string.map_open_flat)) }
        }
    }
}

@Composable
private fun NearPanel(
    near: NearFilter?,
    matching: Int,
    points: List<ReferencePointEntity>,
    locating: Boolean,
    onMyLocation: () -> Unit,
    onSearchPlace: () -> Unit,
    onSavePoint: () -> Unit,
    onClear: () -> Unit,
    onRadius: (Double?) -> Unit,
    onSelectPoint: (ReferencePointEntity) -> Unit,
    onDeletePoint: (ReferencePointEntity) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    AssistChip(
                        onClick = onMyLocation,
                        enabled = !locating,
                        label = { Text(stringResource(if (locating) R.string.location_fetching else R.string.map_my_location)) },
                        leadingIcon = { Icon(Icons.Filled.MyLocation, contentDescription = null) },
                    )
                }
                item {
                    AssistChip(
                        onClick = onSearchPlace,
                        label = { Text(stringResource(R.string.map_search_place)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    )
                }
                if (near != null) {
                    item {
                        AssistChip(
                            onClick = onSavePoint,
                            label = { Text(stringResource(R.string.map_save_point)) },
                            leadingIcon = { Icon(Icons.Filled.BookmarkAdd, contentDescription = null) },
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onClear,
                            label = { Text(stringResource(R.string.action_clear)) },
                            leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) },
                        )
                    }
                }
            }
            if (points.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(points, key = { it.id }) { point ->
                        InputChip(
                            selected = near != null && near.label == point.name,
                            onClick = { onSelectPoint(point) },
                            label = { Text(point.name) },
                            trailingIcon = {
                                IconButton(onClick = { onDeletePoint(point) }, modifier = Modifier.padding(0.dp)) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.map_delete_point, point.name),
                                    )
                                }
                            },
                        )
                    }
                }
            }
            if (near == null) {
                Text(
                    stringResource(R.string.map_near_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                RadiusControls(near, matching, onRadius)
            }
        }
    }
}

@Composable
private fun RadiusControls(near: NearFilter, matching: Int, onRadius: (Double?) -> Unit) {
    val limited = near.radiusKm != null
    var slider by remember(near.radiusKm) { mutableFloatStateOf((near.radiusKm ?: 3.0).toFloat()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.map_near_label, near.label),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(stringResource(R.string.map_limit_radius), style = MaterialTheme.typography.bodySmall)
        Switch(
            checked = limited,
            onCheckedChange = { onRadius(if (it) slider.toDouble() else null) },
            modifier = Modifier.padding(start = 8.dp),
        )
    }
    if (limited) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = slider,
                onValueChange = { slider = it },
                onValueChangeFinished = { onRadius(slider.toDouble()) },
                valueRange = MIN_RADIUS_KM..MAX_RADIUS_KM,
                steps = 38,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.map_radius_km, slider), modifier = Modifier.padding(start = 8.dp))
        }
    }
    Text(
        pluralStringResource(R.plurals.map_matching, matching, matching),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
