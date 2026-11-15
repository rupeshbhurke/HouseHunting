package com.rupeshbhurke.househunter.core.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import com.rupeshbhurke.househunter.core.data.model.AttachmentKind
import com.rupeshbhurke.househunter.core.data.model.MediaSource
import com.rupeshbhurke.househunter.core.data.repo.MediaRepository
import com.rupeshbhurke.househunter.core.location.Fix
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ImportResult(val imported: Int, val failed: Int)

/**
 * Copies photos and videos picked from the gallery or shared from another app (e.g. WhatsApp) into a
 * flat. Files are copied because shared URIs stop working once the sharing app lets go of them.
 */
@Singleton
class MediaImporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaFiles: MediaFiles,
    private val media: MediaRepository,
) {
    suspend fun import(entryId: Long, uris: List<Uri>): ImportResult = withContext(Dispatchers.IO) {
        var imported = 0
        var failed = 0
        for (uri in uris) if (importOne(entryId, uri)) imported++ else failed++
        ImportResult(imported, failed)
    }

    private suspend fun importOne(entryId: Long, uri: Uri): Boolean {
        val mime = mimeOf(uri) ?: return false
        val kind = when {
            mime.startsWith("image/") -> AttachmentKind.PHOTO
            mime.startsWith("video/") -> AttachmentKind.VIDEO
            else -> return false
        }
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?: if (kind == AttachmentKind.PHOTO) "jpg" else "mp4"
        val file = mediaFiles.newFile(entryId, extension)
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return false
            input.use { source -> file.outputStream().use { sink -> source.copyTo(sink) } }
        } catch (e: CancellationException) {
            file.delete()
            throw e
        } catch (_: Exception) {
            file.delete()
            return false
        }

        val info = readInfo(file, kind)
        media.addFile(
            entryId = entryId,
            file = file,
            kind = kind,
            mime = mime,
            source = MediaSource.IMPORTED,
            fix = info.fix,
            capturedAt = info.takenAt ?: System.currentTimeMillis(),
            durationMs = info.durationMs,
        )
        return true
    }

    private fun mimeOf(uri: Uri): String? =
        context.contentResolver.getType(uri)
            ?: MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                ?.takeIf { it.isNotEmpty() }
                ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.lowercase()) }

    private class Info(val fix: Fix?, val takenAt: Long?, val durationMs: Long?)

    private fun readInfo(file: File, kind: AttachmentKind): Info = try {
        if (kind == AttachmentKind.PHOTO) {
            val exif = ExifInterface(file.path)
            val point = exif.latLong
            Info(
                fix = point?.let { Fix(it[0], it[1], accuracyM = null) },
                takenAt = MediaMetadataParsing.parseExifDate(
                    exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                        ?: exif.getAttribute(ExifInterface.TAG_DATETIME),
                ),
                durationMs = null,
            )
        } else {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.path)
                val point = MediaMetadataParsing.parseIso6709(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION),
                )
                Info(
                    fix = point?.let { Fix(it.lat, it.lng, accuracyM = null) },
                    takenAt = MediaMetadataParsing.parseVideoDate(
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE),
                    ),
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                )
            } finally {
                retriever.release()
            }
        }
    } catch (_: Exception) {
        // Unreadable metadata is not a reason to refuse the file.
        Info(null, null, null)
    }
}
