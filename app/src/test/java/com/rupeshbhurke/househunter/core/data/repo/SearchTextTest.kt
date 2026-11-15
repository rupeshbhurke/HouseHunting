package com.rupeshbhurke.househunter.core.data.repo

import com.rupeshbhurke.househunter.core.data.db.ContactEntity
import com.rupeshbhurke.househunter.core.data.db.EntryEntity
import com.rupeshbhurke.househunter.core.data.db.FieldValueEntity
import com.rupeshbhurke.househunter.core.data.db.NoteEntity
import com.rupeshbhurke.househunter.core.data.model.ContactRole
import com.rupeshbhurke.househunter.core.data.model.EntryStatus
import com.rupeshbhurke.househunter.core.data.model.FieldDef
import com.rupeshbhurke.househunter.core.data.model.FieldType
import com.rupeshbhurke.househunter.core.data.model.NoteKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTextTest {
    private val entry = EntryEntity(id = 1, collectionId = 1, title = "Sunrise Apartments", status = EntryStatus.VISIT_AGAIN, createdAt = 0, updatedAt = 0)
    private val defs = mapOf(
        "price" to FieldDef("price", "Rent / price", FieldType.MONEY),
        "custom_parking" to FieldDef("custom_parking", "Parking", FieldType.TEXT),
    )

    private fun text() = SearchText.build(
        entry = entry,
        defs = defs,
        values = listOf(
            FieldValueEntity(1, "price", numValue = 28_000.0),
            FieldValueEntity(1, "custom_parking", textValue = "Covered"),
        ),
        contacts = listOf(ContactEntity(entryId = 1, role = ContactRole.AGENT, name = "Sharma", phone = "98765 43210")),
        notes = listOf(NoteEntity(entryId = 1, kind = NoteKind.TEXT, text = "Water pressure is low", createdAt = 0)),
        captions = listOf("Balcony view"),
    )

    @Test
    fun everythingAboutTheFlat_isSearchable() {
        val t = text()
        listOf("sunrise", "visit again", "rent / price", "parking: covered", "sharma", "98765", "water pressure", "balcony").forEach {
            assertTrue("missing '$it' in:\n$t", it in t)
        }
    }

    @Test
    fun numbers_matchAsShownAndAsTyped() {
        val t = text()
        assertTrue("28,000" in t)
        assertTrue("28000" in t)
    }

    @Test
    fun textIsLowerCase() {
        assertFalse(text().any { it.isUpperCase() })
    }

    @Test
    fun valuesOfUnknownFieldsStillCount() {
        val t = SearchText.build(
            entry = entry,
            defs = emptyMap(),
            values = listOf(FieldValueEntity(1, "legacy_field", textValue = "Old Value")),
            contacts = emptyList(),
            notes = emptyList(),
            captions = emptyList(),
        )
        assertTrue("legacy_field: " in t)
    }
}
