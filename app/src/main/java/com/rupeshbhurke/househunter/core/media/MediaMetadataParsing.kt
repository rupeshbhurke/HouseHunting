package com.rupeshbhurke.househunter.core.media

import com.rupeshbhurke.househunter.core.util.LatLng
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Pure parsing of the date and place strings that photos and videos carry. */
object MediaMetadataParsing {
    private val exifDate = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
    private val videoDate = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    /** EXIF dates ("2026:11:16 10:15:00") carry no zone, so they are read in [zone] (the phone's own). */
    fun parseExifDate(value: String?, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            LocalDateTime.parse(text, exifDate).atZone(zone).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /** Video container dates look like "20261116T101500.000Z" and are UTC. */
    fun parseVideoDate(value: String?): Long? {
        val text = value?.trim()?.takeIf { it.length >= 15 } ?: return null
        return try {
            LocalDateTime.parse(text.substring(0, 15), videoDate).toInstant(ZoneOffset.UTC).toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private val iso6709 = Regex("^([+-]\\d+(?:\\.\\d+)?)([+-]\\d+(?:\\.\\d+)?)")

    /** Video location tags use ISO 6709: "+18.5204+073.8567/" (an altitude may follow). */
    fun parseIso6709(value: String?): LatLng? {
        val match = iso6709.find(value?.trim().orEmpty()) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lng = match.groupValues[2].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        return LatLng(lat, lng)
    }
}
