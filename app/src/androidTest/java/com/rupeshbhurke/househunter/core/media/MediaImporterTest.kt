package com.rupeshbhurke.househunter.core.media

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rupeshbhurke.househunter.core.data.db.AppDatabase
import com.rupeshbhurke.househunter.core.data.model.AttachmentKind
import com.rupeshbhurke.househunter.core.data.model.MediaSource
import com.rupeshbhurke.househunter.core.data.repo.CollectionRepository
import com.rupeshbhurke.househunter.core.data.repo.EntryRepository
import com.rupeshbhurke.househunter.core.data.repo.MediaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class MediaImporterTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var files: MediaFiles
    private lateinit var media: MediaRepository
    private lateinit var importer: MediaImporter
    private var entryId = 0L

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        files = MediaFiles(context)
        media = MediaRepository(db.attachmentDao(), files)
        importer = MediaImporter(context, files, media)
        val collections = CollectionRepository(db.collectionDao(), db.templateDao(), db.entryDao(), files)
        val entries = EntryRepository(
            db, db.entryDao(), db.collectionDao(), db.templateDao(),
            db.entryFieldDefDao(), db.fieldValueDao(), db.contactDao(), db.attachmentDao(), files,
        )
        entryId = entries.createEntry(collections.create("Pune"))
    }

    @After
    fun tearDown() = db.close()

    private fun taggedJpeg(): File {
        val file = File(context.cacheDir, "src_${System.nanoTime()}.jpg")
        FileOutputStream(file).use { Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it) }
        ExifInterface(file.path).apply {
            setLatLong(18.5204, 73.8567)
            setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2026:11:16 10:15:00")
            saveAttributes()
        }
        return file
    }

    @Test
    fun photo_isCopiedWithItsExifPlaceAndDate() = runBlocking {
        val source = taggedJpeg()

        val result = importer.import(entryId, listOf(Uri.fromFile(source)))

        assertEquals(ImportResult(imported = 1, failed = 0), result)
        val item = media.observeMedia(entryId).first().single()
        assertEquals(AttachmentKind.PHOTO, item.kind)
        assertEquals(MediaSource.IMPORTED, item.source)
        assertEquals(18.5204, item.lat!!, 1e-4)
        assertEquals(73.8567, item.lng!!, 1e-4)
        assertEquals(MediaMetadataParsing.parseExifDate("2026:11:16 10:15:00"), item.capturedAt)
        val copy = files.resolve(item.filePath)
        assertTrue(copy.exists())
        assertNotEquals(source.absolutePath, copy.absolutePath)
        assertTrue(copy.absolutePath.contains("/media/$entryId/"))
    }

    @Test
    fun photoWithoutExif_getsNoPositionAndTheImportTime() = runBlocking {
        val file = File(context.cacheDir, "plain_${System.nanoTime()}.jpg")
        FileOutputStream(file).use { Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it) }
        val before = System.currentTimeMillis()

        importer.import(entryId, listOf(Uri.fromFile(file)))

        val item = media.observeMedia(entryId).first().single()
        assertNull(item.lat)
        assertTrue(item.capturedAt >= before)
    }

    @Test
    fun unsupportedAndMissingFiles_areCountedAsFailures() = runBlocking {
        val text = File(context.cacheDir, "note_${System.nanoTime()}.txt").apply { writeText("hello") }
        val missing = File(context.cacheDir, "gone_${System.nanoTime()}.jpg")

        val result = importer.import(entryId, listOf(Uri.fromFile(text), Uri.fromFile(missing), Uri.fromFile(taggedJpeg())))

        assertEquals(ImportResult(imported = 1, failed = 2), result)
        assertEquals(1, media.observeMedia(entryId).first().size)
    }
}
