package com.rupeshbhurke.househunter.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rupeshbhurke.househunter.R
import com.rupeshbhurke.househunter.core.data.db.EntryEntity
import com.rupeshbhurke.househunter.core.data.model.EntryStatus
import com.rupeshbhurke.househunter.core.util.Coordinates

@Composable
fun StatusChip(status: EntryStatus, modifier: Modifier = Modifier) {
    AssistChip(onClick = {}, label = { Text(stringResource(status.labelRes)) }, modifier = modifier)
}

/**
 * Five stars; pass [onRate] to make them tappable (tapping the current rating clears it).
 *
 * Read-only stars are collapsed into one "Rated N of 5" node, so TalkBack reads the
 * rating once rather than stepping through five unlabeled icons. Tappable stars each
 * get a 48dp target around the icon, the minimum touch size, while the icon keeps
 * its caller's [size]; each star reports `selected` so TalkBack can say how many are lit.
 */
@Composable
fun RatingStars(
    rating: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    onRate: ((Int?) -> Unit)? = null,
) {
    val summary = if (rating != null) {
        stringResource(R.string.rating_stars_rated, rating)
    } else {
        stringResource(R.string.rating_stars_not_rated)
    }
    val rowModifier = if (onRate == null) modifier.clearAndSetSemantics { contentDescription = summary } else modifier
    Row(modifier = rowModifier) {
        for (star in 1..5) {
            val filled = rating != null && star <= rating
            val icon = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder
            val tint = if (filled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
            if (onRate == null) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(size))
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .selectable(selected = filled, onClick = { onRate(if (rating == star) null else star) })
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = stringResource(R.string.edit_rate_stars, star),
                        tint = tint,
                        modifier = Modifier.size(size),
                    )
                }
            }
        }
    }
}

/** "18.52040, 73.85670 · ±12 m", or "… · entered manually" for a hand-typed position. */
@Composable
fun locationSummary(entry: EntryEntity): String {
    val lat = entry.lat ?: return ""
    val lng = entry.lng ?: return ""
    val tail = entry.accuracyM?.let(Coordinates::accuracy) ?: stringResource(R.string.location_manual)
    return "${Coordinates.format(lat, lng)} · $tail"
}
