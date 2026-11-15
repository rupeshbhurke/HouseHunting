package com.rupeshbhurke.househunter.core.data.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object FieldResolver {
    /** Template fields, then collection-wide extras, then the flat's own fields; first definition of a key wins. */
    fun effectiveFields(
        template: List<FieldDef>,
        collectionExtras: List<FieldDef>,
        entryOwn: List<FieldDef>,
    ): List<ScopedField> {
        val seen = HashSet<String>()
        val result = ArrayList<ScopedField>()
        fun add(defs: List<FieldDef>, scope: FieldScope) {
            for (def in defs) if (seen.add(def.key)) result += ScopedField(def, scope)
        }
        add(template, FieldScope.TEMPLATE)
        add(collectionExtras, FieldScope.COLLECTION)
        add(entryOwn, FieldScope.ENTRY)
        return result
    }

    /** Builds a stable key from a user-typed label that does not clash with any key in [taken]. */
    fun uniqueKey(label: String, taken: Set<String>): String {
        val slug = label.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "field" }
        val base = "custom_$slug"
        if (base !in taken) return base
        var n = 2
        while ("${base}_$n" in taken) n++
        return "${base}_$n"
    }

    /** Fields grouped for display, keeping the template's group order and putting "Other" last. */
    fun <T> groupInOrder(items: List<T>, groupOf: (T) -> String): List<Pair<String, List<T>>> {
        val grouped = items.groupBy(groupOf)
        val order = listOf(FieldDef.GROUP_BASICS, FieldDef.GROUP_MONEY, FieldDef.GROUP_EVALUATION)
        val known = order.filter { it in grouped }
        val custom = grouped.keys.filter { it !in order && it != FieldDef.GROUP_OTHER }
        val other = if (FieldDef.GROUP_OTHER in grouped) listOf(FieldDef.GROUP_OTHER) else emptyList()
        return (known + custom + other).map { it to grouped.getValue(it) }
    }
}

object EntryTitles {
    fun default(sequence: Int, date: LocalDate, locale: Locale = Locale.getDefault()): String =
        "Flat $sequence – ${DateTimeFormatter.ofPattern("d MMM", locale).format(date)}"
}
