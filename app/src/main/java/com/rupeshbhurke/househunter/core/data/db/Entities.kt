package com.rupeshbhurke.househunter.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rupeshbhurke.househunter.core.data.model.AttachmentKind
import com.rupeshbhurke.househunter.core.data.model.ContactRole
import com.rupeshbhurke.househunter.core.data.model.EntryStatus
import com.rupeshbhurke.househunter.core.data.model.FieldType
import com.rupeshbhurke.househunter.core.data.model.MediaSource

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val fieldDefsJson: String,
)

@Entity(tableName = "collections", indices = [Index("templateId")])
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val templateId: String,
    /** Fields promoted to "all flats" in this collection (JSON list of FieldDef). */
    val extraFieldDefsJson: String = "[]",
    val createdAt: Long,
)

@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(CollectionEntity::class, ["id"], ["collectionId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("collectionId")],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectionId: Long,
    val title: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val accuracyM: Float? = null,
    val status: EntryStatus = EntryStatus.NEW,
    val rating: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** A custom field that belongs to a single flat only. */
@Entity(
    tableName = "entry_field_defs",
    foreignKeys = [
        ForeignKey(EntryEntity::class, ["id"], ["entryId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["entryId", "key"], unique = true)],
)
data class EntryFieldDefEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val key: String,
    val label: String,
    val type: FieldType,
    val groupName: String,
    val optionsJson: String = "[]",
)

@Entity(
    tableName = "field_values",
    primaryKeys = ["entryId", "fieldKey"],
    foreignKeys = [
        ForeignKey(EntryEntity::class, ["id"], ["entryId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("fieldKey")],
)
data class FieldValueEntity(
    val entryId: Long,
    val fieldKey: String,
    val textValue: String? = null,
    val numValue: Double? = null,
)

@Entity(
    tableName = "contacts",
    foreignKeys = [
        ForeignKey(EntryEntity::class, ["id"], ["entryId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("entryId")],
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val role: ContactRole,
    val name: String,
    val phone: String,
)

/**
 * A photo, video or recording of a flat. [filePath] is relative to the app's files folder.
 * [isNote] marks files that belong to a note (shown in the notes timeline, not the media strip).
 */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(EntryEntity::class, ["id"], ["entryId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("entryId")],
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val kind: AttachmentKind,
    val filePath: String,
    val mime: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val capturedAt: Long,
    val source: MediaSource,
    val caption: String? = null,
    val durationMs: Long? = null,
    val isNote: Boolean = false,
)
