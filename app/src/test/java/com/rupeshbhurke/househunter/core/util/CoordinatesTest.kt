package com.rupeshbhurke.househunter.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoordinatesTest {
    @Test
    fun parse_acceptsTheFormatGoogleMapsCopies() {
        assertEquals(LatLng(18.5204, 73.8567), Coordinates.parse("18.5204, 73.8567"))
    }

    @Test
    fun parse_acceptsSpaceOrSemicolonSeparatorsAndNegatives() {
        assertEquals(LatLng(18.5204, 73.8567), Coordinates.parse("  18.5204   73.8567 "))
        assertEquals(LatLng(-33.8688, 151.2093), Coordinates.parse("-33.8688;151.2093"))
    }

    @Test
    fun parse_rejectsOutOfRangeValues() {
        assertNull(Coordinates.parse("91, 10"))
        assertNull(Coordinates.parse("10, 181"))
    }

    @Test
    fun parse_rejectsAnythingElse() {
        assertNull(Coordinates.parse(""))
        assertNull(Coordinates.parse("18.5204"))
        assertNull(Coordinates.parse("18.5204, 73.8567, 5"))
        assertNull(Coordinates.parse("north, east"))
    }

    @Test
    fun format_usesFiveDecimalsWithADotRegardlessOfLocale() {
        assertEquals("18.52040, 73.85670", Coordinates.format(18.5204, 73.8567))
    }

    @Test
    fun accuracy_roundsToWholeMetres() {
        assertEquals("±12 m", Coordinates.accuracy(11.6f))
    }
}
