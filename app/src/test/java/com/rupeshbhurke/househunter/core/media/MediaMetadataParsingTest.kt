package com.rupeshbhurke.househunter.core.media

import com.rupeshbhurke.househunter.core.util.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class MediaMetadataParsingTest {
    @Test
    fun exifDate_isReadInTheGivenZone() {
        // 2026-11-16 10:15:00 in Kolkata (UTC+5:30) is 04:45:00 UTC.
        assertEquals(
            1_794_804_300_000L,
            MediaMetadataParsing.parseExifDate("2026:11:16 10:15:00", ZoneId.of("Asia/Kolkata")),
        )
    }

    @Test
    fun exifDate_rejectsMissingOrMalformedValues() {
        assertNull(MediaMetadataParsing.parseExifDate(null))
        assertNull(MediaMetadataParsing.parseExifDate("  "))
        assertNull(MediaMetadataParsing.parseExifDate("2026-11-16"))
        assertNull(MediaMetadataParsing.parseExifDate("0000:00:00 00:00:00"))
    }

    @Test
    fun videoDate_isUtc() {
        assertEquals(1_794_804_300_000L, MediaMetadataParsing.parseVideoDate("20261116T044500.000Z"))
        assertNull(MediaMetadataParsing.parseVideoDate("garbage"))
        assertNull(MediaMetadataParsing.parseVideoDate(null))
    }

    @Test
    fun iso6709_readsLatitudeAndLongitudeWithOrWithoutAltitude() {
        assertEquals(LatLng(18.5204, 73.8567), MediaMetadataParsing.parseIso6709("+18.5204+073.8567/"))
        assertEquals(LatLng(18.5204, 73.8567), MediaMetadataParsing.parseIso6709("+18.5204+073.8567+010.000/"))
        assertEquals(LatLng(-33.8688, 151.2093), MediaMetadataParsing.parseIso6709("-33.8688+151.2093/"))
    }

    @Test
    fun iso6709_rejectsGarbageAndOutOfRange() {
        assertNull(MediaMetadataParsing.parseIso6709(null))
        assertNull(MediaMetadataParsing.parseIso6709("18.5204,73.8567"))
        assertNull(MediaMetadataParsing.parseIso6709("+95.0000+073.8567/"))
    }
}
