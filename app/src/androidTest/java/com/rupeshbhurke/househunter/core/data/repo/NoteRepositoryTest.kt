package com.rupeshbhurke.househunter.core.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rupeshbhurke.househunter.core.data.db.AppDatabase
import com.rupeshbhurke.househunter.core.data.model.NoteKind
import com.rupeshbhurke.househunter.core.location.Fix
import com.rupeshbhurke.househunter.core.media.MediaFiles
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var files: MediaFiles
    private lateinit var collections: CollectionRepository
    private lateinit var entries: EntryRepository
    private lateinit var notes: NoteRepository
    private lateinit var media: MediaRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        files = MediaFiles(context)
        collections = CollectionRepository(db.collectionDao(), db.templateDao(), db.entryDao(), files)
        entries = EntryRepository(
            db, db.entryDao(), db.collectionDao(), db.templateDao(),
            db.entryFieldDefDao(), db.fieldValueDao(), db.contactDao(), db.attachmentDao(), files,
        )
        notes = NoteRepository(db.noteDao(), db.attachmentDao(), files)
        media = MediaRepository(db.attachmentDao(), files)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun newFlat() = entries.createEntry(collections.create("Pune"))

    @Test
    fun textNote_isStoredTrimmedWithPositionAndListedNewestFirst() = runBlocking {
        val entryId = newFlat()
        notes.addText(entryId, "  Water pressure is low ", Fix(18.5, 73.8, 5f))
        Thread.sleep(5)
        notes.addText(entryId, "Great light", null)

        val list = notes.observeNotes(entryId).first()
        assertEquals(listOf("Great light", "Water pressure is low"), list.map { it.note.text })
        assertEquals(18.5, list.last().note.lat!!, 0.0)
        assertNull(list.first().note.lat)
    }

    @Test
    fun textNote_canBeEdited() = runBlocking {
        val entryId = newFlat()
        val id = notes.addText(entryId, "old", null)
        notes.updateText(id, " new ")
        assertEquals("new", notes.observeNotes(entryId).first().single().note.text)
    }

    @Test
    fun audioNote_keepsItsRecordingOutOfTheMediaStripAndRemovesItOnDelete() = runBlocking {
        val entryId = newFlat()
        val file = files.newFile(entryId, "m4a").apply { writeText("audio") }
        notes.addRecording(entryId, file, NoteKind.AUDIO, 12_000, Fix(18.5, 73.8, null))

        val item = notes.observeNotes(entryId).first().single()
        assertEquals(NoteKind.AUDIO, item.note.kind)
        assertEquals(12_000L, item.attachment!!.durationMs)
        assertTrue(media.observeMedia(entryId).first().isEmpty())

        notes.delete(item)
        assertFalse(file.exists())
        assertTrue(notes.observeNotes(entryId).first().isEmpty())
    }

    @Test
    fun videoNote_isLinkedToItsClip() = runBlocking {
        val entryId = newFlat()
        val file = files.newFile(entryId, "mp4").apply { writeText("video") }
        notes.addRecording(entryId, file, NoteKind.VIDEO, 4_000, null)

        val item = notes.observeNotes(entryId).first().single()
        assertNotNull(item.attachment)
        assertEquals("media/$entryId/${file.name}", item.attachment!!.filePath)
    }

    @Test
    fun deletingAFlat_removesItsNotesAndRecordings() = runBlocking {
        val entryId = newFlat()
        val file = files.newFile(entryId, "m4a").apply { writeText("audio") }
        notes.addRecording(entryId, file, NoteKind.AUDIO, 1_000, null)
        notes.addText(entryId, "hello", null)

        entries.deleteEntry(entryId)

        assertFalse(file.exists())
        assertTrue(notes.observeNotes(entryId).first().isEmpty())
    }
}
