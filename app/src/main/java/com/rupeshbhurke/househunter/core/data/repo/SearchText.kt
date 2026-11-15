package com.rupeshbhurke.househunter.core.data.repo

import com.rupeshbhurke.househunter.core.data.db.ContactEntity
import com.rupeshbhurke.househunter.core.data.db.EntryEntity
import com.rupeshbhurke.househunter.core.data.db.FieldValueEntity
import com.rupeshbhurke.househunter.core.data.db.NoteEntity
import com.rupeshbhurke.househunter.core.data.model.FieldDef
import com.rupeshbhurke.househunter.core.util.FieldFormat
import java.util.Locale

/** Builds the lower-case text that free-text search looks through for one flat. */
object SearchText {
    fun build(
        entry: EntryEntity,
        defs: Map<String, FieldDef>,
        values: Collection<FieldValueEntity>,
        contacts: Collection<ContactEntity>,
        notes: Collection<NoteEntity>,
        captions: Collection<String>,
    ): String = buildList {
        add(entry.title)
        add(entry.status.name.replace('_', ' '))
        for (value in values) {
            val def = defs[value.fieldKey]
            val label = def?.label ?: value.fieldKey
            val shown = def?.let { FieldFormat.display(it.type, value.textValue, value.numValue) }
            // Numbers are searchable both as shown (₹ 28,000) and as typed (28000).
            val raw = def?.let { FieldFormat.editable(it.type, value.textValue, value.numValue) }
            add("$label: ${shown.orEmpty()} ${raw.orEmpty()}")
        }
        contacts.forEach { add("${it.name} ${it.phone}") }
        notes.mapNotNull { it.text }.forEach(::add)
        captions.forEach(::add)
    }.joinToString("\n").lowercase(Locale.ROOT)
}
