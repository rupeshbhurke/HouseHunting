package com.rupeshbhurke.househunter.core.data.repo

import com.rupeshbhurke.househunter.core.data.db.AttachmentDao
import com.rupeshbhurke.househunter.core.data.db.AttachmentEntity
import com.rupeshbhurke.househunter.core.data.db.NoteDao
import com.rupeshbhurke.househunter.core.data.db.NoteEntity
import com.rupeshbhurke.househunter.core.data.model.AttachmentKind
import com.rupeshbhurke.househunter.core.data.model.MediaSource
import com.rupeshbhurke.househunter.core.data.model.NoteKind
import com.rupeshbhurke.househunter.core.location.Fix
import com.rupeshbhurke.househunter.core.media.MediaFiles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** A note together with its recording, for audio and video notes. */
data class NoteItem(val note: NoteEntity, val attachment: AttachmentEntity?)

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val attachmentDao: AttachmentDao,
    private val mediaFiles: MediaFiles,
) {
    /** Newest first. */
    fun observeNotes(entryId: Long): Flow<List<NoteItem>> =
        combine(noteDao.observe(entryId), attachmentDao.observeNoteFiles(entryId)) { notes, files ->
            val byId = files.associateBy { it.id }
            notes.map { NoteItem(it, it.attachmentId?.let(byId::get)) }
        }

    suspend fun addText(entryId: Long, text: String, fix: Fix?): Long = noteDao.insert(
        NoteEntity(
            entryId = entryId,
            kind = NoteKind.TEXT,
            text = text.trim(),
            lat = fix?.lat,
            lng = fix?.lng,
            createdAt = System.currentTimeMillis(),
        ),
    )

    /** [file] must already sit in the flat's media folder (see [MediaFiles.newFile]). */
    suspend fun addRecording(
        entryId: Long,
        file: File,
        kind: NoteKind,
        durationMs: Long,
        fix: Fix?,
    ): Long {
        require(kind != NoteKind.TEXT) { "Use addText for text notes" }
        val attachmentId = attachmentDao.insert(
            AttachmentEntity(
                entryId = entryId,
                kind = if (kind == NoteKind.AUDIO) AttachmentKind.AUDIO else AttachmentKind.VIDEO,
                filePath = mediaFiles.relativePath(file),
                mime = if (kind == NoteKind.AUDIO) "audio/mp4" else "video/mp4",
                lat = fix?.lat,
                lng = fix?.lng,
                capturedAt = System.currentTimeMillis(),
                source = MediaSource.CAPTURED,
                durationMs = durationMs,
                isNote = true,
            ),
        )
        return noteDao.insert(
            NoteEntity(
                entryId = entryId,
                kind = kind,
                attachmentId = attachmentId,
                lat = fix?.lat,
                lng = fix?.lng,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun updateText(noteId: Long, text: String) = noteDao.updateText(noteId, text.trim())

    /** Removes the note and, for recordings, its file. */
    suspend fun delete(item: NoteItem) {
        noteDao.delete(item.note.id)
        item.attachment?.let {
            attachmentDao.delete(it.id)
            mediaFiles.delete(it.filePath)
        }
    }
}
