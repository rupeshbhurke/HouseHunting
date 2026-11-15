package com.rupeshbhurke.househunter.core.data.model

import com.rupeshbhurke.househunter.core.data.db.EntryEntity
import com.rupeshbhurke.househunter.core.data.db.FieldValueEntity
import com.rupeshbhurke.househunter.core.data.repo.EntrySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryFiltererTest {
    private fun row(
        id: Long,
        title: String = "Flat $id",
        status: EntryStatus = EntryStatus.NEW,
        rating: Int? = null,
        price: Double? = null,
        bhk: String? = null,
        extra: Map<String, FieldValueEntity> = emptyMap(),
        search: String = title.lowercase(),
    ): EntrySummary {
        val values = buildMap {
            price?.let { put("price", FieldValueEntity(id, "price", numValue = it)) }
            bhk?.let { put("bhk", FieldValueEntity(id, "bhk", textValue = it)) }
            putAll(extra)
        }
        return EntrySummary(
            entry = EntryEntity(id = id, collectionId = 1, title = title, status = status, rating = rating, createdAt = 0, updatedAt = 0),
            price = price,
            bhk = bhk,
            values = values,
            searchText = search,
        )
    }

    private val rows = listOf(
        row(1, status = EntryStatus.SHORTLISTED, rating = 4, price = 30_000.0, bhk = "2 BHK", search = "flat 1\nsociety: sunrise apts\nsharma 98765"),
        row(2, status = EntryStatus.REJECTED, rating = 2, price = 45_000.0, bhk = "3 BHK", search = "flat 2\nnote: water pressure is low"),
        row(3, status = EntryStatus.SHORTLISTED, price = 25_000.0, bhk = "2 BHK", search = "flat 3 covered parking"),
        row(4),
    )

    private fun ids(filter: EntryFilter) = EntryFilterer.apply(rows, filter).map { it.entry.id }

    @Test
    fun noFilter_keepsEveryFlatInOrder() {
        assertEquals(listOf(1L, 2L, 3L, 4L), ids(EntryFilter()))
        assertFalse(EntryFilter().isActive)
    }

    @Test
    fun text_mustMatchEveryWordAnywhereInTheFlat() {
        assertEquals(listOf(1L), ids(EntryFilter(text = "sunrise sharma")))
        assertEquals(listOf(2L), ids(EntryFilter(text = "WATER")))
        assertEquals(emptyList<Long>(), ids(EntryFilter(text = "sunrise parking")))
    }

    @Test
    fun statusAndRating_combine() {
        assertEquals(listOf(1L, 3L), ids(EntryFilter(statuses = setOf(EntryStatus.SHORTLISTED))))
        assertEquals(listOf(1L), ids(EntryFilter(statuses = setOf(EntryStatus.SHORTLISTED), minRating = 3)))
    }

    @Test
    fun rangeFilter_usesNumbersAndSkipsFlatsWithoutAValue() {
        val under35k = EntryFilter().withField("price", FieldFilter.Range(null, 35_000.0))
        assertEquals(listOf(1L, 3L), ids(under35k))
        val between = EntryFilter().withField("price", FieldFilter.Range(28_000.0, 50_000.0))
        assertEquals(listOf(1L, 2L), ids(between))
    }

    @Test
    fun choiceFilter_matchesAnyOfTheChoices() {
        assertEquals(listOf(1L, 3L), ids(EntryFilter().withField("bhk", FieldFilter.Choice(setOf("2 BHK")))))
        assertEquals(listOf(1L, 2L, 3L), ids(EntryFilter().withField("bhk", FieldFilter.Choice(setOf("2 BHK", "3 BHK")))))
    }

    @Test
    fun customFieldFilters_workLikeBuiltInOnes() {
        val parking = mapOf("custom_parking" to FieldValueEntity(3, "custom_parking", textValue = "Covered, 2 slots"))
        val withParking = rows.map { if (it.entry.id == 3L) it.copy(values = it.values + parking) else it }

        val result = EntryFilterer.apply(withParking, EntryFilter().withField("custom_parking", FieldFilter.Contains("covered")))
        assertEquals(listOf(3L), result.map { it.entry.id })

        val stars = mapOf("custom_view" to FieldValueEntity(1, "custom_view", numValue = 5.0))
        val rated = rows.map { if (it.entry.id == 1L) it.copy(values = it.values + stars) else it }
        assertEquals(
            listOf(1L),
            EntryFilterer.apply(rated, EntryFilter().withField("custom_view", FieldFilter.MinRating(4))).map { it.entry.id },
        )
    }

    @Test
    fun withField_dropsBlankFiltersAndCountsActiveOnes() {
        val f = EntryFilter(text = "x", minRating = 2)
            .withField("price", FieldFilter.Range(null, 1.0))
            .withField("bhk", FieldFilter.Choice(emptySet()))
            .withField("floor", FieldFilter.Contains("  "))
        assertEquals(setOf("price"), f.fields.keys)
        assertEquals(3, f.activeCount)
        assertTrue(f.withField("price", null).fields.isEmpty())
    }

    @Test
    fun cleared_keepsTheSortOrder() {
        val f = EntryFilter(text = "x", sort = SortOption.RATING).cleared()
        assertFalse(f.isActive)
        assertEquals(SortOption.RATING, f.sort)
    }

    @Test
    fun sorting_putsFlatsWithoutAValueLast() {
        assertEquals(listOf(3L, 1L, 2L, 4L), ids(EntryFilter(sort = SortOption.PRICE_LOW)))
        assertEquals(listOf(2L, 1L, 3L, 4L), ids(EntryFilter(sort = SortOption.PRICE_HIGH)))
        assertEquals(listOf(1L, 2L, 3L, 4L), ids(EntryFilter(sort = SortOption.RATING)))
    }
}
