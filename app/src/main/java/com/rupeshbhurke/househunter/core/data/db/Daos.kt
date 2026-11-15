package com.rupeshbhurke.househunter.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CollectionWithCount(
    @Embedded val collection: CollectionEntity,
    val entryCount: Int,
)

@Dao
interface TemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: TemplateEntity)

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun get(id: String): TemplateEntity?
}

@Dao
interface CollectionDao {
    @Query(
        "SELECT c.*, (SELECT COUNT(*) FROM entries e WHERE e.collectionId = c.id) AS entryCount " +
            "FROM collections c ORDER BY c.createdAt DESC",
    )
    fun observeAllWithCount(): Flow<List<CollectionWithCount>>

    @Query("SELECT * FROM collections WHERE id = :id")
    fun observe(id: Long): Flow<CollectionEntity?>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun get(id: Long): CollectionEntity?

    @Insert
    suspend fun insert(collection: CollectionEntity): Long

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE collections SET extraFieldDefsJson = :json WHERE id = :id")
    suspend fun setExtraFieldDefs(id: Long, json: String)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE collectionId = :collectionId ORDER BY updatedAt DESC")
    fun observeByCollection(collectionId: Long): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE id = :id")
    fun observe(id: Long): Flow<EntryEntity?>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun get(id: Long): EntryEntity?

    @Query("SELECT id FROM entries WHERE collectionId = :collectionId")
    suspend fun idsInCollection(collectionId: Long): List<Long>

    @Query("SELECT COUNT(*) FROM entries WHERE collectionId = :collectionId")
    suspend fun countInCollection(collectionId: Long): Int

    @Insert
    suspend fun insert(entry: EntryEntity): Long

    @Update
    suspend fun update(entry: EntryEntity)

    @Query("UPDATE entries SET lat = :lat, lng = :lng, accuracyM = :accuracyM, updatedAt = :now WHERE id = :id")
    suspend fun setLocation(id: Long, lat: Double?, lng: Double?, accuracyM: Float?, now: Long)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface EntryFieldDefDao {
    @Query("SELECT * FROM entry_field_defs WHERE entryId = :entryId ORDER BY id")
    fun observe(entryId: Long): Flow<List<EntryFieldDefEntity>>

    @Query("SELECT * FROM entry_field_defs WHERE entryId = :entryId ORDER BY id")
    suspend fun get(entryId: Long): List<EntryFieldDefEntity>

    @Query(
        "SELECT d.* FROM entry_field_defs d JOIN entries e ON e.id = d.entryId " +
            "WHERE e.collectionId = :collectionId ORDER BY d.id",
    )
    fun observeByCollection(collectionId: Long): Flow<List<EntryFieldDefEntity>>

    @Insert
    suspend fun insert(def: EntryFieldDefEntity)
}

@Dao
interface FieldValueDao {
    @Query("SELECT * FROM field_values WHERE entryId = :entryId")
    fun observe(entryId: Long): Flow<List<FieldValueEntity>>

    @Query(
        "SELECT v.* FROM field_values v JOIN entries e ON e.id = v.entryId WHERE e.collectionId = :collectionId",
    )
    fun observeByCollection(collectionId: Long): Flow<List<FieldValueEntity>>

    @Query("DELETE FROM field_values WHERE entryId = :entryId")
    suspend fun deleteForEntry(entryId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(values: List<FieldValueEntity>)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE entryId = :entryId ORDER BY id")
    fun observe(entryId: Long): Flow<List<ContactEntity>>

    @Query(
        "SELECT c.* FROM contacts c JOIN entries e ON e.id = c.entryId WHERE e.collectionId = :collectionId",
    )
    fun observeByCollection(collectionId: Long): Flow<List<ContactEntity>>

    @Insert
    suspend fun insert(contact: ContactEntity)

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE entryId = :entryId AND isNote = 0 ORDER BY capturedAt, id")
    fun observeMedia(entryId: Long): Flow<List<AttachmentEntity>>

    /** Every photo and video of the collection, oldest first; the list uses the first per flat as its cover. */
    @Query(
        "SELECT a.* FROM attachments a JOIN entries e ON e.id = a.entryId " +
            "WHERE e.collectionId = :collectionId AND a.isNote = 0 AND a.kind IN ('PHOTO', 'VIDEO') " +
            "ORDER BY a.capturedAt, a.id",
    )
    fun observeVisualMediaByCollection(collectionId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE entryId = :entryId AND isNote = 1")
    fun observeNoteFiles(entryId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun get(id: Long): AttachmentEntity?

    @Insert
    suspend fun insert(attachment: AttachmentEntity): Long

    @Query("UPDATE attachments SET caption = :caption WHERE id = :id")
    suspend fun setCaption(id: Long, caption: String?)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE entryId = :entryId ORDER BY createdAt DESC, id DESC")
    fun observe(entryId: Long): Flow<List<NoteEntity>>

    @Query("SELECT n.* FROM notes n JOIN entries e ON e.id = n.entryId WHERE e.collectionId = :collectionId")
    fun observeByCollection(collectionId: Long): Flow<List<NoteEntity>>

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Query("UPDATE notes SET text = :text WHERE id = :id")
    suspend fun updateText(id: Long, text: String)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}
