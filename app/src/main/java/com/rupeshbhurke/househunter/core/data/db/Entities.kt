package com.rupeshbhurke.househunter.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rupeshbhurke.househunter.core.data.model.ContactRole
import com.rupeshbhurke.househunter.core.data.model.EntryStatus
import com.rupeshbhurke.househunter.core.data.model.FieldType

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
