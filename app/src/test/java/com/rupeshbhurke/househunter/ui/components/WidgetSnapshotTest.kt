package com.rupeshbhurke.househunter.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import com.rupeshbhurke.househunter.core.data.model.EntryStatus
import com.rupeshbhurke.househunter.ui.SnapshotTest
import org.junit.Test

/** Goldens for the shared widgets every screen reuses. */
class WidgetSnapshotTest : SnapshotTest() {
    @Test
    fun statusChip_everyStatus() = snapshot {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EntryStatus.entries.forEach { StatusChip(it) }
        }
    }

    @Test
    fun statusChip_everyStatus_dark() = snapshot(dark = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EntryStatus.entries.forEach { StatusChip(it) }
        }
    }

    /** Null means "not rated", which must look different from zero stars. */
    @Test
    fun ratingStars_everyValue() = snapshot {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null, 1, 2, 3, 4, 5).forEach { rating ->
                Text(rating?.toString() ?: "not rated")
                RatingStars(rating = rating)
            }
        }
    }

    @Test
    fun ratingStars_everyValue_dark() = snapshot(dark = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null, 1, 3, 5).forEach { RatingStars(rating = it) }
        }
    }
}
