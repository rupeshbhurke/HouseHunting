package com.rupeshbhurke.househunter.feature.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rupeshbhurke.househunter.R
import com.rupeshbhurke.househunter.core.data.model.EntryFilter
import com.rupeshbhurke.househunter.core.data.model.EntryStatus
import com.rupeshbhurke.househunter.core.data.model.FieldDef
import com.rupeshbhurke.househunter.core.data.model.FieldFilter
import com.rupeshbhurke.househunter.core.data.model.FieldResolver
import com.rupeshbhurke.househunter.core.data.model.FieldType
import com.rupeshbhurke.househunter.core.data.model.SortOption
import com.rupeshbhurke.househunter.core.util.FieldFormat
import com.rupeshbhurke.househunter.ui.components.RatingStars

/** Sort order, status, rating and a control for every field in the collection, custom fields included. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    filter: EntryFilter,
    fields: List<FieldDef>,
    onChange: (EntryFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var resetToken by remember { mutableIntStateOf(0) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.filters_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { onChange(filter.cleared()); resetToken++ },
                    enabled = filter.isActive,
                ) { Text(stringResource(R.string.filters_reset)) }
            }

            Section(R.string.filters_sort) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortOption.entries.filter { it != SortOption.DISTANCE || filter.near != null }.forEach { option ->
                        FilterChip(
                            selected = filter.sort == option,
                            onClick = { onChange(filter.copy(sort = option)) },
                            label = { Text(stringResource(option.labelRes())) },
                        )
                    }
                }
            }
            filter.near?.let { near ->
                Section(R.string.filters_near) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            near.radiusKm?.let { stringResource(R.string.filters_near_within, near.label, it) }
                                ?: near.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            onChange(filter.copy(near = null, sort = if (filter.sort == SortOption.DISTANCE) SortOption.RECENT else filter.sort))
                        }) { Text(stringResource(R.string.action_clear)) }
                    }
                }
            }
            Section(R.string.edit_status_label) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EntryStatus.entries.forEach { status ->
                        val on = status in filter.statuses
                        FilterChip(
                            selected = on,
                            onClick = { onChange(filter.copy(statuses = if (on) filter.statuses - status else filter.statuses + status)) },
                            label = { Text(stringResource(status.labelRes)) },
                        )
                    }
                }
            }
            Section(R.string.filters_min_rating) {
                RatingStars(filter.minRating, size = 32.dp, onRate = { onChange(filter.copy(minRating = it)) })
            }

            FieldResolver.groupInOrder(fields) { it.group }.forEach { (group, defs) ->
                Text(group, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                defs.forEach { def ->
                    FieldFilterEditor(
                        def = def,
                        current = filter.fields[def.key],
                        resetToken = resetToken,
                        onChange = { onChange(filter.withField(def.key, it)) },
                    )
                }
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.filters_done)) }
        }
    }
}

private fun SortOption.labelRes() = when (this) {
    SortOption.RECENT -> R.string.sort_recent
    SortOption.PRICE_LOW -> R.string.sort_price_low
    SortOption.PRICE_HIGH -> R.string.sort_price_high
    SortOption.RATING -> R.string.sort_rating
    SortOption.DISTANCE -> R.string.sort_distance
}

@Composable
private fun Section(title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.labelLarge)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FieldFilterEditor(
    def: FieldDef,
    current: FieldFilter?,
    resetToken: Int,
    onChange: (FieldFilter?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(def.label, style = MaterialTheme.typography.labelLarge)
        when (def.type) {
            FieldType.NUMBER, FieldType.MONEY -> {
                val range = current as? FieldFilter.Range
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberBox(stringResource(R.string.filters_min), range?.min, resetToken, Modifier.weight(1f)) {
                        onChange(FieldFilter.Range(it, range?.max))
                    }
                    NumberBox(stringResource(R.string.filters_max), range?.max, resetToken, Modifier.weight(1f)) {
                        onChange(FieldFilter.Range(range?.min, it))
                    }
                }
            }
            FieldType.DATE -> {
                val range = current as? FieldFilter.Range
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    DateBound(stringResource(R.string.filters_from), range?.min) { onChange(FieldFilter.Range(it, range?.max)) }
                    DateBound(stringResource(R.string.filters_to), range?.max) { onChange(FieldFilter.Range(range?.min, it)) }
                }
            }
            FieldType.ENUM -> {
                val chosen = (current as? FieldFilter.Choice)?.values.orEmpty()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    def.options.forEach { option ->
                        val on = option in chosen
                        FilterChip(
                            selected = on,
                            onClick = { onChange(FieldFilter.Choice(if (on) chosen - option else chosen + option)) },
                            label = { Text(option) },
                        )
                    }
                }
            }
            FieldType.RATING -> RatingStars(
                rating = (current as? FieldFilter.MinRating)?.stars,
                size = 32.dp,
                onRate = { onChange(it?.let(FieldFilter::MinRating)) },
            )
            FieldType.TEXT, FieldType.LONG_TEXT, FieldType.PHONE -> {
                var text by remember(resetToken, def.key) { mutableStateOf((current as? FieldFilter.Contains)?.text.orEmpty()) }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; onChange(FieldFilter.Contains(it)) },
                    label = { Text(stringResource(R.string.filters_contains)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun NumberBox(label: String, value: Double?, resetToken: Int, modifier: Modifier, onChange: (Double?) -> Unit) {
    var text by remember(resetToken) { mutableStateOf(value?.let { FieldFormat.editable(FieldType.NUMBER, null, it) }.orEmpty()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val clean = input.filter { it.isDigit() || it == '.' }
            text = clean
            onChange(clean.toDoubleOrNull())
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateBound(label: String, millis: Double?, onChange: (Double?) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { picking = true }) {
        Text(millis?.let { "$label ${FieldFormat.date(it.toLong())}" } ?: label)
    }
    if (millis != null) {
        IconButton(onClick = { onChange(null) }) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
        }
    }
    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = millis?.toLong())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(it.toDouble()) }
                    picking = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = state) }
    }
}
