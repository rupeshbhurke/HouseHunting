package com.rupeshbhurke.househunter.core.util

import com.rupeshbhurke.househunter.core.data.model.FieldType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object FieldFormat {
    /**
     * Indian digit grouping (12,34,567.5), up to two decimals. Done by hand because the JDK's
     * NumberFormat ignores the lakh grouping while Android's ICU-backed one honours it.
     */
    fun number(value: Double): String {
        val plain = BigDecimal(Math.abs(value)).setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros().toPlainString()
        val whole = plain.substringBefore('.')
        val fraction = plain.substringAfter('.', "")
        val grouped = if (whole.length <= 3) {
            whole
        } else {
            val head = whole.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
            "$head,${whole.takeLast(3)}"
        }
        val sign = if (value < 0 && plain != "0") "-" else ""
        return sign + grouped + if (fraction.isEmpty()) "" else ".$fraction"
    }

    fun money(value: Double): String = "₹ ${number(value)}"

    /** Dates are stored as UTC midnight millis, matching what the Material date picker returns. */
    fun date(millis: Long, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            .format(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC))

    /** "1:05" for 65 seconds, "1:02:03" past an hour. */
    fun duration(millis: Long): String {
        val total = millis / 1_000
        val h = total / 3_600
        val m = (total % 3_600) / 60
        val sec = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
    }

    /** Local date and time of an instant, e.g. for when a photo was taken. */
    fun dateTime(millis: Long, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
            .format(Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()))

    /** Human-readable value for display, or null when there is nothing to show. */
    fun display(type: FieldType, text: String?, number: Double?): String? = when (type) {
        FieldType.NUMBER -> number?.let(::number)
        FieldType.MONEY -> number?.let(::money)
        FieldType.DATE -> number?.let { date(it.toLong()) }
        FieldType.RATING -> number?.let { "★ ${it.toInt()}/5" }
        else -> text?.takeIf { it.isNotBlank() }
    }

    /** Raw editable text for a stored value (whole numbers without a trailing ".0"). */
    fun editable(type: FieldType, text: String?, number: Double?): String =
        if (type.isNumeric) number?.let(::plain).orEmpty() else text.orEmpty()

    private fun plain(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
