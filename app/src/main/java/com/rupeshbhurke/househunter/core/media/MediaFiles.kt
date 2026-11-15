package com.rupeshbhurke.househunter.core.media

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/** Resolves a stored relative media path (e.g. "media/7/170000_1.jpg") to a file. */
fun Context.mediaFile(relativePath: String): File = File(filesDir, relativePath)

/** Owns the app-private folder that holds every flat's photos, videos and recordings. */
@Singleton
class MediaFiles @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val counter = AtomicInteger()

    /** A fresh, not-yet-existing file inside the flat's media folder. */
    fun newFile(entryId: Long, extension: String): File {
        val dir = File(context.filesDir, "media/$entryId").apply { mkdirs() }
        return File(dir, "${System.currentTimeMillis()}_${counter.incrementAndGet()}.$extension")
    }

    fun relativePath(file: File): String =
        file.relativeTo(context.filesDir).path.replace(File.separatorChar, '/')

    fun resolve(relativePath: String): File = context.mediaFile(relativePath)

    fun delete(relativePath: String) {
        resolve(relativePath).delete()
    }

    fun deleteEntryFolder(entryId: Long) {
        File(context.filesDir, "media/$entryId").deleteRecursively()
    }
}
