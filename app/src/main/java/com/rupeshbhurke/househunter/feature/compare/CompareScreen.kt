package com.rupeshbhurke.househunter.feature.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rupeshbhurke.househunter.R
import com.rupeshbhurke.househunter.core.data.model.FieldDef
import com.rupeshbhurke.househunter.core.data.model.FieldResolver
import com.rupeshbhurke.househunter.core.data.repo.EntryDetail
import com.rupeshbhurke.househunter.core.util.FieldFormat
import com.rupeshbhurke.househunter.ui.components.MediaThumbnail
import com.rupeshbhurke.househunter.ui.components.RatingStars
import com.rupeshbhurke.househunter.ui.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    onBack: () -> Unit,
    viewModel: CompareViewModel = hiltViewModel(),
) {
    val flats by viewModel.flats.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compare_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        flats?.let { CompareTable(it, Modifier.padding(padding)) }
    }
}

@Composable
private fun CompareTable(flats: List<EntryDetail>, modifier: Modifier) {
    // Fields in order of first appearance, limited to the ones at least one flat has filled in.
    val defs = LinkedHashMap<String, FieldDef>()
    flats.forEach { flat -> flat.fields.filterNot { it.isBlank }.forEach { defs.putIfAbsent(it.def.key, it.def) } }
    val byFlat = flats.map { flat -> flat.fields.associateBy { it.def.key } }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            flats.forEach { flat -> Header(flat, Modifier.weight(1f)) }
        }
        HorizontalDivider()
        FieldResolver.groupInOrder(defs.values.toList()) { it.group }.forEach { (group, groupDefs) ->
            Text(group, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            groupDefs.forEach { def ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(def.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        byFlat.forEach { fields ->
                            val entry = fields[def.key]
                            val shown = entry?.let { FieldFormat.display(def.type, it.text, it.number) }
                            Text(
                                shown ?: stringResource(R.string.compare_no_value),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (shown == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(flat: EntryDetail, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val cover = flat.attachments.firstOrNull()
        if (cover != null) {
            MediaThumbnail(cover, size = 96.dp)
        } else {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Text(flat.entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        StatusChip(flat.entry.status)
        RatingStars(flat.entry.rating, size = 16.dp)
    }
}
