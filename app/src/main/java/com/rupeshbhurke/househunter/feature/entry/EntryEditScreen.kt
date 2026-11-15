package com.rupeshbhurke.househunter.feature.entry

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rupeshbhurke.househunter.R
import com.rupeshbhurke.househunter.core.data.db.ContactEntity
import com.rupeshbhurke.househunter.core.data.db.EntryEntity
import com.rupeshbhurke.househunter.core.data.model.ContactRole
import com.rupeshbhurke.househunter.core.data.model.EntryStatus
import com.rupeshbhurke.househunter.core.data.model.FieldResolver
import com.rupeshbhurke.househunter.core.data.model.FieldType
import com.rupeshbhurke.househunter.core.data.repo.FieldEntry
import com.rupeshbhurke.househunter.core.util.Coordinates
import com.rupeshbhurke.househunter.core.util.FieldFormat
import com.rupeshbhurke.househunter.core.util.LatLng
import com.rupeshbhurke.househunter.core.util.MapActions
import com.rupeshbhurke.househunter.ui.components.RatingStars
import com.rupeshbhurke.househunter.ui.components.locationSummary

private sealed interface EditDialog {
    data object AddField : EditDialog
    data object EnterCoordinates : EditDialog
    data class Contact(val existing: ContactEntity?) : EditDialog
    data object Discard : EditDialog
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryEditScreen(
    onDone: () -> Unit,
    viewModel: EntryEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<EditDialog?>(null) }
    val locationStatus by viewModel.locationStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) viewModel.captureLocation() else viewModel.permissionDenied()
    }
    val onCapture = {
        val granted = locationPermissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) viewModel.captureLocation() else permissionLauncher.launch(locationPermissions)
    }
    val dirty = state?.dirty == true
    val requestBack = { if (dirty) dialog = EditDialog.Discard else onDone() }

    BackHandler(onBack = requestBack)

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_edit)) },
                navigationIcon = {
                    IconButton(onClick = requestBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save(onDone) }, enabled = state != null) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        val ui = state ?: return@Scaffold
        val groups = FieldResolver.groupInOrder(ui.detail.fields) { it.def.group }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = ui.draft.title,
                    onValueChange = viewModel::setTitle,
                    label = { Text(stringResource(R.string.edit_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.edit_status_label), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EntryStatus.entries.forEach { status ->
                            FilterChip(
                                selected = ui.draft.status == status,
                                onClick = { viewModel.setStatus(status) },
                                label = { Text(stringResource(status.labelRes)) },
                            )
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.edit_rating_label), style = MaterialTheme.typography.labelLarge)
                    RatingStars(ui.draft.rating, size = 32.dp, onRate = viewModel::setRating)
                }
            }
            item {
                LocationSection(
                    entry = ui.detail.entry,
                    status = locationStatus,
                    onCapture = onCapture,
                    onEnterCoordinates = { dialog = EditDialog.EnterCoordinates },
                    onClear = viewModel::clearLocation,
                )
            }
            items(groups, key = { it.first }) { (group, fields) ->
                FieldGroupSection(
                    group = group,
                    fields = fields,
                    values = ui.draft.values,
                    onValueChange = viewModel::setValue,
                )
            }
            item {
                OutlinedButton(onClick = { dialog = EditDialog.AddField }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.edit_add_field), modifier = Modifier.padding(start = 8.dp))
                }
            }
            item {
                Text(
                    stringResource(R.string.entry_contacts),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(ui.detail.contacts, key = { "contact-${it.id}" }) { contact ->
                ContactRow(
                    contact = contact,
                    onEdit = { dialog = EditDialog.Contact(contact) },
                    onDelete = { viewModel.deleteContact(contact) },
                )
            }
            item {
                OutlinedButton(onClick = { dialog = EditDialog.Contact(null) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.edit_add_contact), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    when (val d = dialog) {
        EditDialog.AddField -> AddFieldDialog(
            onConfirm = { label, type, options, addToAll ->
                viewModel.addField(label, type, options, addToAll)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        EditDialog.EnterCoordinates -> CoordinatesDialog(
            onConfirm = { viewModel.setManualLocation(it); dialog = null },
            onDismiss = { dialog = null },
        )
        is EditDialog.Contact -> ContactDialog(
            existing = d.existing,
            onConfirm = { role, name, phone ->
                if (d.existing == null) viewModel.addContact(role, name, phone)
                else viewModel.updateContact(d.existing, role, name, phone)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        EditDialog.Discard -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.edit_discard_title)) },
            text = { Text(stringResource(R.string.edit_discard_body)) },
            confirmButton = {
                TextButton(onClick = { dialog = null; onDone() }) { Text(stringResource(R.string.action_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
        null -> Unit
    }
}

/** Collapsible group; starts open only when something in it is already filled in. */
@Composable
private fun FieldGroupSection(
    group: String,
    fields: List<FieldEntry>,
    values: Map<String, String>,
    onValueChange: (String, String) -> Unit,
) {
    val hasValue = fields.any { it.def.key in values }
    var expanded by rememberSaveable(group) { mutableStateOf(hasValue) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    group,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    fields.forEach { field ->
                        FieldInput(
                            field = field,
                            value = values[field.def.key].orEmpty(),
                            onValueChange = { onValueChange(field.def.key, it) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FieldInput(field: FieldEntry, value: String, onValueChange: (String) -> Unit) {
    val def = field.def
    when (def.type) {
        FieldType.TEXT, FieldType.LONG_TEXT, FieldType.PHONE -> OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(def.label) },
            singleLine = def.type != FieldType.LONG_TEXT,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (def.type == FieldType.PHONE) KeyboardType.Phone else KeyboardType.Text,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        FieldType.NUMBER, FieldType.MONEY -> OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) },
            label = { Text(def.label) },
            prefix = if (def.type == FieldType.MONEY) ({ Text("₹") }) else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        FieldType.ENUM -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(def.label, style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                def.options.forEach { option ->
                    FilterChip(
                        selected = value == option,
                        onClick = { onValueChange(if (value == option) "" else option) },
                        label = { Text(option) },
                    )
                }
            }
        }
        FieldType.RATING -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(def.label, style = MaterialTheme.typography.labelLarge)
            RatingStars(
                rating = value.toIntOrNull(),
                size = 32.dp,
                onRate = { onValueChange(it?.toString().orEmpty()) },
            )
        }
        FieldType.DATE -> {
            var picking by rememberSaveable(def.key) { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(def.label, style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { picking = true }) {
                        Text(value.toLongOrNull()?.let(FieldFormat::date) ?: stringResource(R.string.edit_pick_date))
                    }
                    if (value.isNotEmpty()) {
                        IconButton(onClick = { onValueChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                        }
                    }
                }
            }
            if (picking) {
                val pickerState = rememberDatePickerState(initialSelectedDateMillis = value.toLongOrNull())
                DatePickerDialog(
                    onDismissRequest = { picking = false },
                    confirmButton = {
                        TextButton(onClick = {
                            pickerState.selectedDateMillis?.let { onValueChange(it.toString()) }
                            picking = false
                        }) { Text(stringResource(R.string.action_save)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { picking = false }) { Text(stringResource(R.string.action_cancel)) }
                    },
                ) { DatePicker(state = pickerState) }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: ContactEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${contact.name} · ${stringResource(contact.role.labelRes)}", style = MaterialTheme.typography.bodyLarge)
                Text(
                    contact.phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddFieldDialog(
    onConfirm: (label: String, type: FieldType, options: String, addToAll: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(FieldType.TEXT) }
    var options by rememberSaveable { mutableStateOf("") }
    var addToAll by rememberSaveable { mutableStateOf(false) }
    val optionCount = options.split(',').count { it.isNotBlank() }
    val valid = label.isNotBlank() && (type != FieldType.ENUM || optionCount >= 2)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_field_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.add_field_label)) },
                    singleLine = true,
                )
                Text(stringResource(R.string.add_field_type), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(stringResource(option.labelRes)) },
                        )
                    }
                }
                if (type == FieldType.ENUM) {
                    OutlinedTextField(
                        value = options,
                        onValueChange = { options = it },
                        label = { Text(stringResource(R.string.add_field_options)) },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.add_field_to_all), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.add_field_to_all_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = addToAll, onCheckedChange = { addToAll = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(label, type, options, addToAll) }, enabled = valid) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContactDialog(
    existing: ContactEntity?,
    onConfirm: (ContactRole, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var role by rememberSaveable { mutableStateOf(existing?.role ?: ContactRole.AGENT) }
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(existing?.phone.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.contact_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContactRole.entries.forEach { option ->
                        FilterChip(
                            selected = role == option,
                            onClick = { role = option },
                            label = { Text(stringResource(option.labelRes)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.contact_name_label)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.contact_phone_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(role, name, phone) }, enabled = phone.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocationSection(
    entry: EntryEntity,
    status: LocationStatus,
    onCapture: () -> Unit,
    onEnterCoordinates: () -> Unit,
    onClear: () -> Unit,
) {
    val context = LocalContext.current
    val lat = entry.lat
    val lng = entry.lng
    val fetching = status == LocationStatus.Fetching
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.location_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (lat != null && lng != null) {
                Text(locationSummary(entry), style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(
                    stringResource(R.string.location_not_set),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (status is LocationStatus.Failed) {
                Text(
                    stringResource(status.messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onCapture, enabled = !fetching) {
                    if (fetching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                    Text(
                        stringResource(
                            when {
                                fetching -> R.string.location_fetching
                                lat != null -> R.string.location_update
                                else -> R.string.location_capture
                            },
                        ),
                        modifier = Modifier.padding(start = if (fetching) 8.dp else 0.dp),
                    )
                }
                OutlinedButton(onClick = onEnterCoordinates) { Text(stringResource(R.string.location_enter)) }
                if (lat != null && lng != null) {
                    TextButton(onClick = { MapActions.open(context, lat, lng, entry.title) }) {
                        Text(stringResource(R.string.location_open_maps))
                    }
                    TextButton(onClick = onClear) { Text(stringResource(R.string.action_clear)) }
                }
            }
        }
    }
}

@Composable
private fun CoordinatesDialog(onConfirm: (LatLng) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val parsed = Coordinates.parse(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.location_enter_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.location_enter_label)) },
                    placeholder = { Text("18.5204, 73.8567") },
                    singleLine = true,
                    isError = text.isNotBlank() && parsed == null,
                )
                Text(
                    stringResource(R.string.location_enter_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = parsed != null) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
