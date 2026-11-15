package com.rupeshbhurke.househunter.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rupeshbhurke.househunter.R
import com.rupeshbhurke.househunter.core.data.model.EntryStatus

@Composable
fun StatusChip(status: EntryStatus, modifier: Modifier = Modifier) {
    AssistChip(onClick = {}, label = { Text(stringResource(status.labelRes)) }, modifier = modifier)
}

/** Five stars; pass [onRate] to make them tappable (tapping the current rating clears it). */
@Composable
fun RatingStars(
    rating: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    onRate: ((Int?) -> Unit)? = null,
) {
    Row(modifier = modifier) {
        for (star in 1..5) {
            val filled = rating != null && star <= rating
            val base = Modifier.let { if (onRate != null) it.clickable { onRate(if (rating == star) null else star) } else it }
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (onRate != null) stringResource(R.string.edit_rate_stars, star) else null,
                tint = if (filled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                modifier = base.then(Modifier.padding(if (onRate != null) 4.dp else 0.dp)).size(size),
            )
        }
    }
}
