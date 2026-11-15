package com.rupeshbhurke.househunter.core.data.repo

import androidx.room.withTransaction
import com.rupeshbhurke.househunter.core.data.db.AppDatabase
import com.rupeshbhurke.househunter.core.data.db.AttachmentDao
import com.rupeshbhurke.househunter.core.data.db.AttachmentEntity
import com.rupeshbhurke.househunter.core.data.db.CollectionDao
import com.rupeshbhurke.househunter.core.data.db.CollectionEntity
import com.rupeshbhurke.househunter.core.data.db.ContactDao
import com.rupeshbhurke.househunter.core.data.db.ContactEntity
import com.rupeshbhurke.househunter.core.data.db.EntryDao
import com.rupeshbhurke.househunter.core.data.db.EntryEntity
import com.rupeshbhurke.househunter.core.data.db.EntryFieldDefDao
import com.rupeshbhurke.househunter.core.data.db.EntryFieldDefEntity
import com.rupeshbhurke.househunter.core.data.db.FieldValueDao
import com.rupeshbhurke.househunter.core.data.db.NoteDao
import com.rupeshbhurke.househunter.core.data.db.FieldValueEntity
import com.rupeshbhurke.househunter.core.data.db.TemplateDao
import com.rupeshbhurke.househunter.core.data.model.ContactRole
import com.rupeshbhurke.househunter.core.data.model.EntryStatus
import com.rupeshbhurke.househunter.core.data.model.EntryTitles
import com.rupeshbhurke.househunter.core.data.model.FieldDef
import com.rupeshbhurke.househunter.core.data.model.FieldResolver
import com.rupeshbhurke.househunter.core.data.model.FieldScope
import com.rupeshbhurke.househunter.core.data.model.FieldType
import com.rupeshbhurke.househunter.core.data.model.HouseHuntTemplate
import com.rupeshbhurke.househunter.core.data.model.ScopedField
import com.rupeshbhurke.househunter.core.media.MediaFiles
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** A field of a flat together with its stored value (either column may be null). */
data class FieldEntry(
    val scoped: ScopedField,
    val text: String?,
    val number: Double?,
) {
    val def: FieldDef get() = scoped.def
    val isBlank: Boolean get() = text.isNullOrBlank() && number == null
}

data class EntryDetail(
    val entry: EntryEntity,
    val fields: List<FieldEntry>,
    val contacts: List<ContactEntity>,
    val attachments: List<AttachmentEntity> = emptyList(),
)

data class EntrySummary(
    val entry: EntryEntity,
    val price: Double?,
    val bhk: String?,
    /** First photo or video of the flat, if any, for the list thumbnail. */
    val cover: AttachmentEntity? = null,
    /** Every stored value of the flat by field key, for filtering and comparing. */
    val values: Map<String, FieldValueEntity> = emptyMap(),
    /** Lower-case text of everything about the flat, for free-text search. */
    val searchText: String = "",
)

@Singleton
class EntryRepository @Inject constructor(
    private val db: AppDatabase,
    private val entryDao: EntryDao,
    private val collectionDao: CollectionDao,
    private val templateDao: TemplateDao,
    private val fieldDefDao: EntryFieldDefDao,
    private val fieldValueDao: FieldValueDao,
    private val contactDao: ContactDao,
    private val attachmentDao: AttachmentDao,
    private val noteDao: NoteDao,
    private val mediaFiles: MediaFiles,
) {
    fun observeSummaries(collectionId: Long): Flow<List<EntrySummary>> {
        val core = combine(
            entryDao.observeByCollection(collectionId),
            fieldValueDao.observeByCollection(collectionId),
            attachmentDao.observeVisualMediaByCollection(collectionId),
        ) { entries, values, media -> Triple(entries, values, media) }
        val extra = combine(
            contactDao.observeByCollection(collectionId),
            noteDao.observeByCollection(collectionId),
            observeFields(collectionId),
        ) { contacts, notes, fields -> Triple(contacts, notes, fields) }

        return combine(core, extra) { (entries, values, media), (contacts, notes, fields) ->
            val defs = fields.associateBy { it.key }
            val valuesByEntry = values.groupBy { it.entryId }
            val mediaByEntry = media.groupBy { it.entryId }
            val contactsByEntry = contacts.groupBy { it.entryId }
            val notesByEntry = notes.groupBy { it.entryId }
            entries.map { entry ->
                val own = valuesByEntry[entry.id].orEmpty()
                val byKey = own.associateBy { it.fieldKey }
                val entryMedia = mediaByEntry[entry.id].orEmpty()
                EntrySummary(
                    entry = entry,
                    price = byKey[HouseHuntTemplate.Keys.PRICE]?.numValue,
                    bhk = byKey[HouseHuntTemplate.Keys.BHK]?.textValue,
                    cover = entryMedia.firstOrNull(),
                    values = byKey,
                    searchText = SearchText.build(
                        entry = entry,
                        defs = defs,
                        values = own,
                        contacts = contactsByEntry[entry.id].orEmpty(),
                        notes = notesByEntry[entry.id].orEmpty(),
                        captions = entryMedia.mapNotNull { it.caption },
                    ),
                )
            }
        }
    }

    /** Every field that exists in the collection: the template's, the shared extras and each flat's own. */
    fun observeFields(collectionId: Long): Flow<List<FieldDef>> =
        combine(
            collectionDao.observe(collectionId).filterNotNull(),
            fieldDefDao.observeByCollection(collectionId),
        ) { collection, ownDefs ->
            resolve(collection, ownDefs).map { it.def }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDetail(entryId: Long): Flow<EntryDetail> =
        entryDao.observe(entryId).filterNotNull().flatMapLatest { entry ->
            combine(
                collectionDao.observe(entry.collectionId).filterNotNull(),
                fieldDefDao.observe(entryId),
                fieldValueDao.observe(entryId),
                contactDao.observe(entryId),
                attachmentDao.observeMedia(entryId),
            ) { collection, ownDefs, values, contacts, attachments ->
                buildDetail(entry, collection, ownDefs, values, contacts, attachments)
            }
        }

    suspend fun getDetail(entryId: Long): EntryDetail = observeDetail(entryId).first()

    suspend fun createEntry(collectionId: Long): Long {
        val sequence = entryDao.countInCollection(collectionId) + 1
        val now = System.currentTimeMillis()
        return entryDao.insert(
            EntryEntity(
                collectionId = collectionId,
                title = EntryTitles.default(sequence, LocalDate.now()),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun deleteEntry(entryId: Long) {
        entryDao.delete(entryId)
        mediaFiles.deleteEntryFolder(entryId)
    }

    /** Stores the flat's position. [accuracyM] is null for positions typed in by hand. */
    suspend fun setLocation(entryId: Long, lat: Double, lng: Double, accuracyM: Float?) =
        entryDao.setLocation(entryId, lat, lng, accuracyM, System.currentTimeMillis())

    suspend fun clearLocation(entryId: Long) =
        entryDao.setLocation(entryId, null, null, null, System.currentTimeMillis())

    /**
     * Saves the flat's core columns and replaces its field values.
     * [values] holds raw user input keyed by field key; blank or unparsable input is dropped.
     */
    suspend fun saveEntry(
        entryId: Long,
        title: String,
        status: EntryStatus,
        rating: Int?,
        values: Map<String, String>,
    ) = db.withTransaction {
        val entry = entryDao.get(entryId) ?: return@withTransaction
        val defs = effectiveFields(entry).associateBy { it.def.key }
        val rows = values.mapNotNull { (key, raw) ->
            val def = defs[key]?.def ?: return@mapNotNull null
            toFieldValue(entryId, def, raw)
        }
        fieldValueDao.deleteForEntry(entryId)
        fieldValueDao.insertAll(rows)
        entryDao.update(
            entry.copy(
                title = title.trim().ifBlank { entry.title },
                status = status,
                rating = rating,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * Adds a custom field. With [addToAll] it joins the collection's extra fields so it shows (empty)
     * on every flat; otherwise it belongs to this flat only. Returns the new field's key.
     */
    suspend fun addCustomField(
        entryId: Long,
        label: String,
        type: FieldType,
        options: List<String>,
        addToAll: Boolean,
    ): String = db.withTransaction {
        val entry = checkNotNull(entryDao.get(entryId)) { "Entry $entryId not found" }
        val collection = checkNotNull(collectionDao.get(entry.collectionId))
        val taken = effectiveFields(entry).map { it.def.key }.toSet()
        val def = FieldDef(
            key = FieldResolver.uniqueKey(label.trim(), taken),
            label = label.trim(),
            type = type,
            group = FieldDef.GROUP_OTHER,
            options = if (type == FieldType.ENUM) options else emptyList(),
        )
        if (addToAll) {
            val extras = decodeDefs(collection.extraFieldDefsJson) + def
            collectionDao.setExtraFieldDefs(collection.id, FieldJson.encodeToString(extras))
        } else {
            fieldDefDao.insert(
                EntryFieldDefEntity(
                    entryId = entryId,
                    key = def.key,
                    label = def.label,
                    type = def.type,
                    groupName = def.group,
                    optionsJson = FieldJson.encodeToString(def.options),
                ),
            )
        }
        def.key
    }

    suspend fun addContact(entryId: Long, role: ContactRole, name: String, phone: String) =
        contactDao.insert(ContactEntity(entryId = entryId, role = role, name = name.trim(), phone = phone.trim()))

    suspend fun updateContact(contact: ContactEntity) = contactDao.update(contact)

    suspend fun deleteContact(contact: ContactEntity) = contactDao.delete(contact)

    private suspend fun effectiveFields(entry: EntryEntity): List<ScopedField> {
        val collection = checkNotNull(collectionDao.get(entry.collectionId))
        return resolve(collection, fieldDefDao.get(entry.id))
    }

    private suspend fun resolve(
        collection: CollectionEntity,
        ownDefs: List<EntryFieldDefEntity>,
    ): List<ScopedField> {
        val template = templateDao.get(collection.templateId)
            ?.let { decodeDefs(it.fieldDefsJson) }
            ?: HouseHuntTemplate.fields
        return FieldResolver.effectiveFields(
            template = template,
            collectionExtras = decodeDefs(collection.extraFieldDefsJson),
            entryOwn = ownDefs.map { it.toFieldDef() },
        )
    }

    private suspend fun buildDetail(
        entry: EntryEntity,
        collection: CollectionEntity,
        ownDefs: List<EntryFieldDefEntity>,
        values: List<FieldValueEntity>,
        contacts: List<ContactEntity>,
        attachments: List<AttachmentEntity>,
    ): EntryDetail {
        val byKey = values.associateBy { it.fieldKey }
        val fields = resolve(collection, ownDefs).map { scoped ->
            val v = byKey[scoped.def.key]
            FieldEntry(scoped, v?.textValue, v?.numValue)
        }
        return EntryDetail(entry, fields, contacts, attachments)
    }

    private fun toFieldValue(entryId: Long, def: FieldDef, raw: String): FieldValueEntity? {
        val input = raw.trim()
        if (input.isEmpty()) return null
        return if (def.type.isNumeric) {
            input.toDoubleOrNull()?.let { FieldValueEntity(entryId, def.key, numValue = it) }
        } else {
            FieldValueEntity(entryId, def.key, textValue = input)
        }
    }

    private fun EntryFieldDefEntity.toFieldDef() = FieldDef(
        key = key,
        label = label,
        type = type,
        group = groupName,
        options = FieldJson.decodeFromString(optionsJson),
    )

    private fun decodeDefs(json: String): List<FieldDef> = FieldJson.decodeFromString(json)
}
