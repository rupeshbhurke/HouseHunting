package com.rupeshbhurke.househunter.core.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class FieldResolverTest {
    private fun def(key: String, group: String = FieldDef.GROUP_OTHER) =
        FieldDef(key, key.uppercase(), FieldType.TEXT, group)

    @Test
    fun effectiveFields_ordersTemplateThenCollectionThenEntry() {
        val result = FieldResolver.effectiveFields(
            template = listOf(def("a"), def("b")),
            collectionExtras = listOf(def("c")),
            entryOwn = listOf(def("d")),
        )
        assertEquals(listOf("a", "b", "c", "d"), result.map { it.def.key })
        assertEquals(
            listOf(FieldScope.TEMPLATE, FieldScope.TEMPLATE, FieldScope.COLLECTION, FieldScope.ENTRY),
            result.map { it.scope },
        )
    }

    @Test
    fun effectiveFields_firstDefinitionOfAKeyWins() {
        val result = FieldResolver.effectiveFields(
            template = listOf(def("a")),
            collectionExtras = listOf(def("a")),
            entryOwn = listOf(def("a")),
        )
        assertEquals(1, result.size)
        assertEquals(FieldScope.TEMPLATE, result.single().scope)
    }

    @Test
    fun uniqueKey_slugifiesLabel() {
        assertEquals("custom_water_supply", FieldResolver.uniqueKey("Water supply!", emptySet()))
    }

    @Test
    fun uniqueKey_fallsBackWhenLabelHasNoLettersOrDigits() {
        assertEquals("custom_field", FieldResolver.uniqueKey("???", emptySet()))
    }

    @Test
    fun uniqueKey_avoidsClashes() {
        val taken = setOf("custom_parking", "custom_parking_2")
        assertEquals("custom_parking_3", FieldResolver.uniqueKey("Parking", taken))
    }

    @Test
    fun groupInOrder_putsKnownGroupsFirstAndOtherLast() {
        val items = listOf(
            def("x", FieldDef.GROUP_OTHER),
            def("y", "Neighbourhood"),
            def("z", FieldDef.GROUP_EVALUATION),
            def("w", FieldDef.GROUP_BASICS),
        )
        val groups = FieldResolver.groupInOrder(items) { it.group }
        assertEquals(
            listOf(FieldDef.GROUP_BASICS, FieldDef.GROUP_EVALUATION, "Neighbourhood", FieldDef.GROUP_OTHER),
            groups.map { it.first },
        )
    }

    @Test
    fun defaultTitle_usesSequenceAndShortDate() {
        assertEquals("Flat 3 – 15 Nov", EntryTitles.default(3, LocalDate.of(2026, 11, 15), Locale.ENGLISH))
    }

    @Test
    fun houseHuntTemplate_hasUniqueKeysAndUsableChoices() {
        val keys = HouseHuntTemplate.fields.map { it.key }
        assertEquals(keys.distinct(), keys)
        HouseHuntTemplate.fields.filter { it.type == FieldType.ENUM }.forEach {
            assertTrue("${it.key} needs options", it.options.size >= 2)
        }
    }
}
