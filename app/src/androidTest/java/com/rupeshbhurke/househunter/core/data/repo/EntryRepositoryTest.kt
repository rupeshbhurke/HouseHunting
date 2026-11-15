package com.rupeshbhurke.househunter.core.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rupeshbhurke.househunter.core.data.db.AppDatabase
import com.rupeshbhurke.househunter.core.data.model.ContactRole
import com.rupeshbhurke.househunter.core.data.model.EntryStatus
import com.rupeshbhurke.househunter.core.data.model.FieldScope
import com.rupeshbhurke.househunter.core.data.model.FieldType
import com.rupeshbhurke.househunter.core.data.model.HouseHuntTemplate
import com.rupeshbhurke.househunter.core.data.model.HouseHuntTemplate.Keys
import com.rupeshbhurke.househunter.core.media.MediaFiles
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var collections: CollectionRepository
    private lateinit var entries: EntryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val mediaFiles = MediaFiles(context)
        collections = CollectionRepository(db.collectionDao(), db.templateDao(), db.entryDao(), mediaFiles)
        entries = EntryRepository(
            db, db.entryDao(), db.collectionDao(), db.templateDao(),
            db.entryFieldDefDao(), db.fieldValueDao(), db.contactDao(),
            db.attachmentDao(), mediaFiles,
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun newEntry_hasEveryTemplateFieldBlank() = runBlocking {
        val entryId = entries.createEntry(collections.create("Pune"))
        val detail = entries.getDetail(entryId)
        assertTrue(detail.fields.isNotEmpty())
        assertTrue(detail.fields.all { it.isBlank })
        assertEquals(EntryStatus.NEW, detail.entry.status)
    }

    @Test
    fun syncBuiltInTemplates_refreshesAStaleStoredTemplate() = runBlocking {
        val collectionId = collections.create("Pune")
        // Simulate a template stored by an older app version that lacked the area type field.
        val stale = db.templateDao().get(HouseHuntTemplate.ID)!!
        db.templateDao().upsert(stale.copy(fieldDefsJson = "[]"))
        val entryId = entries.createEntry(collectionId)
        assertTrue(entries.getDetail(entryId).fields.none { it.def.key == Keys.AREA_TYPE })

        collections.syncBuiltInTemplates()

        assertTrue(entries.getDetail(entryId).fields.any { it.def.key == Keys.AREA_TYPE })
    }

    @Test
    fun saveEntry_storesTypedValuesAndDropsBlankOnes() = runBlocking {
        val entryId = entries.createEntry(collections.create("Pune"))
        entries.saveEntry(
            entryId, "Sunrise Apts", EntryStatus.SHORTLISTED, 4,
            mapOf(Keys.PRICE to "25000", Keys.BHK to "2 BHK", Keys.SOCIETY to "  ", Keys.FLOOR to "abc-ignored"),
        )
        val detail = entries.getDetail(entryId)
        val byKey = detail.fields.associateBy { it.def.key }
        assertEquals("Sunrise Apts", detail.entry.title)
        assertEquals(4, detail.entry.rating)
        assertEquals(25000.0, byKey.getValue(Keys.PRICE).number!!, 0.0)
        assertEquals("2 BHK", byKey.getValue(Keys.BHK).text)
        assertTrue(byKey.getValue(Keys.SOCIETY).isBlank)
    }

    @Test
    fun location_isStoredWithAccuracyAndSurvivesSavingTheForm() = runBlocking {
        val entryId = entries.createEntry(collections.create("Pune"))
        entries.setLocation(entryId, 18.5204, 73.8567, 12.5f)

        entries.saveEntry(entryId, "Renamed", EntryStatus.NEW, null, mapOf(Keys.PRICE to "1"))

        val entry = entries.getDetail(entryId).entry
        assertEquals(18.5204, entry.lat!!, 0.0)
        assertEquals(73.8567, entry.lng!!, 0.0)
        assertEquals(12.5f, entry.accuracyM!!, 0.0f)
        assertEquals("Renamed", entry.title)
    }

    @Test
    fun location_canBeClearedAndEnteredManuallyWithoutAccuracy() = runBlocking {
        val entryId = entries.createEntry(collections.create("Pune"))
        entries.setLocation(entryId, 18.5, 73.8, 5f)
        entries.clearLocation(entryId)
        assertNull(entries.getDetail(entryId).entry.lat)

        entries.setLocation(entryId, 19.0, 72.8, null)
        val entry = entries.getDetail(entryId).entry
        assertEquals(19.0, entry.lat!!, 0.0)
        assertNull(entry.accuracyM)
    }

    @Test
    fun saveEntry_withUnparsableNumber_leavesFieldEmpty() = runBlocking {
        val entryId = entries.createEntry(collections.create("Pune"))
        entries.saveEntry(entryId, "x", EntryStatus.NEW, null, mapOf(Keys.PRICE to "1.2.3"))
        assertNull(entries.getDetail(entryId).fields.first { it.def.key == Keys.PRICE }.number)
    }

    @Test
    fun customField_forThisFlatOnly_isNotShownOnOtherFlats() = runBlocking {
        val collectionId = collections.create("Pune")
        val first = entries.createEntry(collectionId)
        val second = entries.createEntry(collectionId)

        val key = entries.addCustomField(first, "Parking", FieldType.TEXT, emptyList(), addToAll = false)
        entries.saveEntry(first, "A", EntryStatus.NEW, null, mapOf(key to "Covered"))

        val onFirst = entries.getDetail(first).fields.first { it.def.key == key }
        assertEquals(FieldScope.ENTRY, onFirst.scoped.scope)
        assertEquals("Covered", onFirst.text)
        assertTrue(entries.getDetail(second).fields.none { it.def.key == key })
    }

    @Test
    fun customField_addedToAll_appearsBlankOnOtherFlats() = runBlocking {
        val collectionId = collections.create("Pune")
        val first = entries.createEntry(collectionId)
        val second = entries.createEntry(collectionId)

        val key = entries.addCustomField(first, "Water supply", FieldType.ENUM, listOf("24x7", "Tanker"), addToAll = true)

        val onSecond = entries.getDetail(second).fields.first { it.def.key == key }
        assertEquals(FieldScope.COLLECTION, onSecond.scoped.scope)
        assertEquals(listOf("24x7", "Tanker"), onSecond.def.options)
        assertTrue(onSecond.isBlank)
    }

    @Test
    fun customField_withSameLabelGetsDistinctKey() = runBlocking {
        val entryId = entries.createEntry(collections.create("Pune"))
        val a = entries.addCustomField(entryId, "Parking", FieldType.TEXT, emptyList(), addToAll = false)
        val b = entries.addCustomField(entryId, "Parking", FieldType.TEXT, emptyList(), addToAll = false)
        assertTrue(a != b)
    }

    @Test
    fun contacts_canBeAddedUpdatedAndDeleted() = runBlocking {
        val entryId = entries.createEntry(collections.create("Pune"))
        entries.addContact(entryId, ContactRole.AGENT, " Sharma ", "98765 43210")

        val added = entries.getDetail(entryId).contacts.single()
        assertEquals("Sharma", added.name)

        entries.updateContact(added.copy(role = ContactRole.OWNER))
        assertEquals(ContactRole.OWNER, entries.getDetail(entryId).contacts.single().role)

        entries.deleteContact(added)
        assertTrue(entries.getDetail(entryId).contacts.isEmpty())
    }

    @Test
    fun summaries_exposePriceAndBhkForTheList() = runBlocking {
        val collectionId = collections.create("Pune")
        val entryId = entries.createEntry(collectionId)
        entries.saveEntry(entryId, "A", EntryStatus.NEW, null, mapOf(Keys.PRICE to "30000", Keys.BHK to "3 BHK"))

        val summary = entries.observeSummaries(collectionId).first().single()
        assertEquals(30000.0, summary.price!!, 0.0)
        assertEquals("3 BHK", summary.bhk)
    }

    @Test
    fun deletingACollection_removesItsFlatsAndTheirData() = runBlocking {
        val collectionId = collections.create("Pune")
        val entryId = entries.createEntry(collectionId)
        entries.saveEntry(entryId, "A", EntryStatus.NEW, null, mapOf(Keys.PRICE to "1"))
        entries.addContact(entryId, ContactRole.AGENT, "S", "1")

        collections.delete(collectionId)

        assertNull(db.entryDao().get(entryId))
        assertTrue(db.fieldValueDao().observe(entryId).first().isEmpty())
        assertTrue(db.contactDao().observe(entryId).first().isEmpty())
    }

    @Test
    fun newEntries_getSequentialDefaultTitles() = runBlocking {
        val collectionId = collections.create("Pune")
        val first = entries.getDetail(entries.createEntry(collectionId)).entry.title
        val second = entries.getDetail(entries.createEntry(collectionId)).entry.title
        assertTrue(first, first.startsWith("Flat 1 "))
        assertTrue(second, second.startsWith("Flat 2 "))
    }
}
