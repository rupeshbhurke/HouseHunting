package com.rupeshbhurke.househunter.feature.entries

import com.rupeshbhurke.househunter.core.data.db.EntryEntity
import com.rupeshbhurke.househunter.core.data.db.ReferencePointEntity
import com.rupeshbhurke.househunter.core.data.model.EntryStatus
import com.rupeshbhurke.househunter.core.data.repo.EntrySummary
import com.rupeshbhurke.househunter.ui.SnapshotTest
import org.junit.Test

/** Goldens for the flat card, the densest thing on the list screen. */
class EntryCardSnapshotTest : SnapshotTest() {
    private fun summary(
        title: String = "Flat 3 - 15 Nov",
        status: EntryStatus = EntryStatus.SHORTLISTED,
        rating: Int? = 4,
        price: Double? = 28_000.0,
        bhk: String? = "2 BHK",
    ) = EntrySummary(
        entry = EntryEntity(
            id = 1,
            collectionId = 1,
            title = title,
            lat = 18.5204,
            lng = 73.8567,
            status = status,
            rating = rating,
            createdAt = 0,
            updatedAt = 0,
        ),
        price = price,
        bhk = bhk,
    )

    private val office = ReferencePointEntity(id = 1, name = "Office", lat = 18.53, lng = 73.85)

    @Test
    fun entryCard_filledIn() = snapshot {
        EntryCard(
            summary = summary(),
            distances = listOf(office to 2.4),
            selecting = false,
            selected = false,
            onClick = {},
            onLongClick = {},
        )
    }

    /** Every field is optional, so the card has to hold up with almost nothing in it. */
    @Test
    fun entryCard_empty() = snapshot {
        EntryCard(
            summary = summary(status = EntryStatus.NEW, rating = null, price = null, bhk = null),
            distances = emptyList(),
            selecting = false,
            selected = false,
            onClick = {},
            onLongClick = {},
        )
    }

    @Test
    fun entryCard_selected() = snapshot {
        EntryCard(
            summary = summary(),
            distances = listOf(office to 2.4),
            selecting = true,
            selected = true,
            onClick = {},
            onLongClick = {},
        )
    }

    @Test
    fun entryCard_filledIn_dark() = snapshot(dark = true) {
        EntryCard(
            summary = summary(),
            distances = listOf(office to 2.4),
            selecting = false,
            selected = false,
            onClick = {},
            onLongClick = {},
        )
    }
}
