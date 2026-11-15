package com.rupeshbhurke.househunter.core.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rupeshbhurke.househunter.core.data.db.AppDatabase
import com.rupeshbhurke.househunter.core.data.model.AttachmentKind
import com.rupeshbhurke.househunter.core.data.model.MediaSource
import com.rupeshbhurke.househunter.core.location.Fix
import com.rupeshbhurke.househunter.core.media.MediaFiles
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var files: MediaFiles
    private lateinit var collections: CollectionRepository
    private lateinit var entries: EntryRepository
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
        media = MediaRepository(db.attachmentDao(), files)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun addPhoto(entryId: Long, isNote: Boolean = false) = files.newFile(entryId, "jpg").let { file ->
        file.writeText("x")
        media.addFile(
            entryId, file, AttachmentKind.PHOTO, "image/jpeg", MediaSource.CAPTURED,
            Fix(18.5, 73.8, 6f), isNote = isNote,
        ) to file
    }

    @Test
    fun addedMedia_isListedWithItsOwnLocationAndRelativePath() = runBlocking {
        val entryId = entries.createEntry(collections.create("Pune"))
        val (_, file) = addPhoto(entryId)

        val item = media.observeMedia(entryId).first().single()
        assertEquals(18.5, item.lat!!, 0.0)
        assertEquals(MediaSource.CAPTURED, item.source)
        assertEquals("media/$entryId/${file.name}", item.filePath)
        assertTrue(files.resolve(item.filePath).exists())
    }

    @Test
    fun noteAttachments_areKeptOutOfTheMediaStrip() = runBlocking {
        val entryId = entries.createEntry(collections.create("Pune"))
        addPhoto(entryId)
        addPhoto(entryId, isNote = true)
        assertEquals(1, media.observeMedia(entryId).first().size)
    }

    @Test
    fun captionCanBeSetAndCleared() = runBlocking {
        val entryId = entries.createEntry(collections.create("Pune"))
        val (id, _) = addPhoto(entryId)

        media.setCaption(id, "  Balcony view ")
        assertEquals("Balcony view", media.observeMedia(entryId).first().single().caption)
        media.setCaption(id, "   ")
        assertNull(media.observeMedia(entryId).first().single().caption)
    }

    @Test
    fun deletingAnItem_removesItsFile() = runBlocking {
        val entryId = entries.createEntry(collections.create("Pune"))
        val (_, file) = addPhoto(entryId)

        media.delete(media.observeMedia(entryId).first().single())

        assertFalse(file.exists())
        assertTrue(media.observeMedia(entryId).first().isEmpty())
    }

    @Test
    fun deletingAFlat_removesItsMediaFolder() = runBlocking {
        val entryId = entries.createEntry(collections.create("Pune"))
        val (_, file) = addPhoto(entryId)

        entries.deleteEntry(entryId)

        assertFalse(file.exists())
    }

    @Test
    fun deletingACollection_removesTheMediaOfItsFlats() = runBlocking {
        val collectionId = collections.create("Pune")
        val (_, file) = addPhoto(entries.createEntry(collectionId))

        collections.delete(collectionId)

        assertFalse(file.exists())
    }

    @Test
    fun listSummary_usesTheFirstPhotoAsCover() = runBlocking {
        val collectionId = collections.create("Pune")
        val entryId = entries.createEntry(collectionId)
        assertNull(entries.observeSummaries(collectionId).first().single().cover)

        val (firstId, _) = addPhoto(entryId)
        addPhoto(entryId)

        assertEquals(firstId, entries.observeSummaries(collectionId).first().single().cover!!.id)
    }
}
