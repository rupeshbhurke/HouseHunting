package com.rupeshbhurke.househunter.core.data.model

import com.rupeshbhurke.househunter.core.data.db.FieldValueEntity
import com.rupeshbhurke.househunter.core.data.repo.EntrySummary
import com.rupeshbhurke.househunter.core.util.Geo

/** A condition on one field of a flat. Flats without a value for the field never match. */
sealed interface FieldFilter {
    /** True when the filter is blank and should be dropped. */
    val isEmpty: Boolean

    /** Numbers, amounts and dates (dates compare as epoch millis). */
    data class Range(val min: Double?, val max: Double?) : FieldFilter {
        override val isEmpty get() = min == null && max == null
    }

    /** One of the listed choices. */
    data class Choice(val values: Set<String>) : FieldFilter {
        override val isEmpty get() = values.isEmpty()
    }

    /** Text containing the given words, ignoring case. */
    data class Contains(val text: String) : FieldFilter {
        override val isEmpty get() = text.isBlank()
    }

    /** At least this many stars, for a custom rating field. */
    data class MinRating(val stars: Int) : FieldFilter {
        override val isEmpty get() = stars <= 0
    }

    fun accepts(value: FieldValueEntity): Boolean = when (this) {
        is Range -> value.numValue?.let { (min == null || it >= min) && (max == null || it <= max) } ?: false
        is Choice -> value.textValue in values
        is Contains -> value.textValue?.contains(text.trim(), ignoreCase = true) == true
        is MinRating -> (value.numValue ?: 0.0) >= stars
    }
}

enum class SortOption { RECENT, PRICE_LOW, PRICE_HIGH, RATING, DISTANCE }

/**
 * Flats near a point. With a [radiusKm], flats without a position (or farther away) are left out;
 * with null they are kept and the point is only used for the nearest-first sort and distances.
 */
data class NearFilter(
    val lat: Double,
    val lng: Double,
    val radiusKm: Double?,
    val label: String,
) {
    fun distanceKm(row: EntrySummary): Double? {
        val flatLat = row.entry.lat ?: return null
        val flatLng = row.entry.lng ?: return null
        return Geo.distanceKm(lat, lng, flatLat, flatLng)
    }
}

data class EntryFilter(
    val text: String = "",
    val statuses: Set<EntryStatus> = emptySet(),
    val minRating: Int? = null,
    val fields: Map<String, FieldFilter> = emptyMap(),
    val sort: SortOption = SortOption.RECENT,
    val near: NearFilter? = null,
) {
    /** How many separate conditions are set (sorting does not count). */
    val activeCount: Int
        get() = (if (text.isNotBlank()) 1 else 0) +
            (if (statuses.isNotEmpty()) 1 else 0) +
            (if (minRating != null) 1 else 0) +
            (if (near != null) 1 else 0) +
            fields.size

    val isActive: Boolean get() = activeCount > 0

    fun withField(key: String, filter: FieldFilter?): EntryFilter =
        copy(fields = if (filter == null || filter.isEmpty) fields - key else fields + (key to filter))

    /** Keeps the sort order but drops every condition. */
    fun cleared(): EntryFilter = EntryFilter(sort = if (sort == SortOption.DISTANCE) SortOption.RECENT else sort)
}

object EntryFilterer {
    fun apply(rows: List<EntrySummary>, filter: EntryFilter): List<EntrySummary> =
        sort(rows.filter { matches(it, filter) }, filter.sort, filter.near)

    fun matches(row: EntrySummary, filter: EntryFilter): Boolean {
        val terms = filter.text.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (terms.any { it !in row.searchText }) return false
        if (filter.statuses.isNotEmpty() && row.entry.status !in filter.statuses) return false
        filter.minRating?.let { if ((row.entry.rating ?: 0) < it) return false }
        filter.near?.let { near ->
            val radius = near.radiusKm
            if (radius != null) {
                val distance = near.distanceKm(row) ?: return false
                if (distance > radius) return false
            }
        }
        for ((key, condition) in filter.fields) {
            val value = row.values[key] ?: return false
            if (!condition.accepts(value)) return false
        }
        return true
    }

    private fun sort(rows: List<EntrySummary>, sort: SortOption, near: NearFilter?): List<EntrySummary> = when (sort) {
        SortOption.RECENT -> rows
        SortOption.PRICE_LOW -> rows.sortedWith(compareBy(nullsLast()) { it.price })
        SortOption.PRICE_HIGH -> rows.sortedWith(descendingNullsLast { it.price })
        SortOption.RATING -> rows.sortedWith(descendingNullsLast { it.entry.rating })
        SortOption.DISTANCE -> if (near == null) rows else rows.sortedWith(compareBy(nullsLast()) { near.distanceKm(it) })
    }

    /** Highest first, with flats that have no value at the end (reversing nullsLast would put them first). */
    private fun <T : Comparable<T>> descendingNullsLast(selector: (EntrySummary) -> T?) =
        Comparator<EntrySummary> { a, b ->
            val x = selector(a)
            val y = selector(b)
            when {
                x == null && y == null -> 0
                x == null -> 1
                y == null -> -1
                else -> y.compareTo(x)
            }
        }
}
