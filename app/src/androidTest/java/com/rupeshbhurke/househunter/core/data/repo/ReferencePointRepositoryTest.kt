package com.rupeshbhurke.househunter.core.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rupeshbhurke.househunter.core.data.db.AppDatabase
import com.rupeshbhurke.househunter.core.media.MediaFiles
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReferencePointRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var points: ReferencePointRepository
    private lateinit var collections: CollectionRepository
    private lateinit var entries: EntryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val files = MediaFiles(context)
        points = ReferencePointRepository(db.referencePointDao())
        collections = CollectionRepository(db.collectionDao(), db.templateDao(), db.entryDao(), files)
        entries = EntryRepository(
            db, db.entryDao(), db.collectionDao(), db.templateDao(),
            db.entryFieldDefDao(), db.fieldValueDao(), db.contactDao(), db.attachmentDao(), db.noteDao(), files,
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun points_areListedByNameIgnoringCaseAndCanBeDeleted() = runBlocking {
        points.add(" school ", 18.50, 73.80)
        points.add("Office", 18.52, 73.85)
        points.add("airport", 18.58, 73.92)

        val list = points.observeAll().first()
        assertEquals(listOf("airport", "Office", "school"), list.map { it.name })

        points.delete(list.first().id)
        assertEquals(listOf("Office", "school"), points.observeAll().first().map { it.name })
    }

    @Test
    fun distances_comeBackInThePointsOrderAndOnlyForFlatsWithAPosition() = runBlocking {
        points.add("Office", 18.5204, 73.8567)
        points.add("School", 18.5300, 73.8700)
        val list = points.observeAll().first()
        val entryId = entries.createEntry(collections.create("Pune"))

        assertTrue(ReferencePointRepository.distances(entries.getDetail(entryId).entry, list).isEmpty())

        entries.setLocation(entryId, 18.5204, 73.8567, null)
        val result = ReferencePointRepository.distances(entries.getDetail(entryId).entry, list)

        assertEquals(listOf("Office", "School"), result.map { it.first.name })
        assertEquals(0.0, result[0].second, 0.01)
        assertTrue(result[1].second in 1.0..3.0)
    }
}
