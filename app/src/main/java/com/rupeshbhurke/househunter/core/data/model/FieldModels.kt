package com.rupeshbhurke.househunter.core.data.model

import androidx.annotation.StringRes
import com.rupeshbhurke.househunter.R
import kotlinx.serialization.Serializable

@Serializable
enum class FieldType(@StringRes val labelRes: Int) {
    TEXT(R.string.field_type_text),
    LONG_TEXT(R.string.field_type_long_text),
    NUMBER(R.string.field_type_number),
    MONEY(R.string.field_type_money),
    PHONE(R.string.field_type_phone),
    ENUM(R.string.field_type_enum),
    RATING(R.string.field_type_rating),
    DATE(R.string.field_type_date),
    ;

    /** Numeric types are stored in the numeric column so they can be range-filtered later. */
    val isNumeric: Boolean get() = this == NUMBER || this == MONEY || this == RATING || this == DATE
}

@Serializable
data class FieldDef(
    val key: String,
    val label: String,
    val type: FieldType,
    val group: String = GROUP_OTHER,
    val options: List<String> = emptyList(),
) {
    companion object {
        const val GROUP_BASICS = "Basics"
        const val GROUP_MONEY = "Money"
        const val GROUP_EVALUATION = "Evaluation"
        const val GROUP_OTHER = "Other"
    }
}

enum class FieldScope { TEMPLATE, COLLECTION, ENTRY }

data class ScopedField(val def: FieldDef, val scope: FieldScope)

enum class EntryStatus(@StringRes val labelRes: Int) {
    NEW(R.string.status_new),
    SHORTLISTED(R.string.status_shortlisted),
    VISIT_AGAIN(R.string.status_visit_again),
    REJECTED(R.string.status_rejected),
}

enum class ContactRole(@StringRes val labelRes: Int) {
    AGENT(R.string.contact_role_agent),
    OWNER(R.string.contact_role_owner),
}

enum class AttachmentKind { PHOTO, VIDEO, AUDIO }

enum class MediaSource { CAPTURED, IMPORTED }
