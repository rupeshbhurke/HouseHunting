package com.rupeshbhurke.househunter.core.data.repo

import com.rupeshbhurke.househunter.core.data.db.AttachmentDao
import com.rupeshbhurke.househunter.core.data.db.AttachmentEntity
import com.rupeshbhurke.househunter.core.data.model.AttachmentKind
import com.rupeshbhurke.househunter.core.data.model.MediaSource
import com.rupeshbhurke.househunter.core.location.Fix
import com.rupeshbhurke.househunter.core.media.MediaFiles
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val attachmentDao: AttachmentDao,
    private val mediaFiles: MediaFiles,
) {
    /** Photos, videos and recordings that belong to the flat itself (note attachments excluded). */
    fun observeMedia(entryId: Long): Flow<List<AttachmentEntity>> = attachmentDao.observeMedia(entryId)

    /** Records a file that is already inside the flat's media folder. Returns the new row id. */
    suspend fun addFile(
        entryId: Long,
        file: File,
        kind: AttachmentKind,
        mime: String,
        source: MediaSource,
        fix: Fix?,
        capturedAt: Long = System.currentTimeMillis(),
        durationMs: Long? = null,
        isNote: Boolean = false,
    ): Long = attachmentDao.insert(
        AttachmentEntity(
            entryId = entryId,
            kind = kind,
            filePath = mediaFiles.relativePath(file),
            mime = mime,
            lat = fix?.lat,
            lng = fix?.lng,
            capturedAt = capturedAt,
            source = source,
            durationMs = durationMs,
            isNote = isNote,
        ),
    )

    suspend fun setCaption(id: Long, caption: String) =
        attachmentDao.setCaption(id, caption.trim().ifEmpty { null })

    /** Removes the row and its file. */
    suspend fun delete(attachment: AttachmentEntity) {
        attachmentDao.delete(attachment.id)
        mediaFiles.delete(attachment.filePath)
    }
}
