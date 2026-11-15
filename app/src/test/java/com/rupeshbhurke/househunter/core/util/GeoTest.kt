package com.rupeshbhurke.househunter.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoTest {
    @Test
    fun distance_puneToMumbaiIsAboutOneTwentyKm() {
        val km = Geo.distanceKm(18.5204, 73.8567, 19.0760, 72.8777)
        assertEquals(120.0, km, 3.0)
    }

    @Test
    fun distance_isZeroForTheSamePointAndSymmetric() {
        assertEquals(0.0, Geo.distanceKm(18.5, 73.8, 18.5, 73.8), 1e-9)
        assertEquals(
            Geo.distanceKm(18.5204, 73.8567, 19.076, 72.8777),
            Geo.distanceKm(19.076, 72.8777, 18.5204, 73.8567),
            1e-9,
        )
    }

    @Test
    fun distance_oneDegreeOfLatitudeIsAbout111Km() {
        assertEquals(111.2, Geo.distanceKm(10.0, 70.0, 11.0, 70.0), 0.2)
    }

    @Test
    fun distance_worksAcrossTheDateLine() {
        assertEquals(22.2, Geo.distanceKm(0.0, 179.9, 0.0, -179.9), 0.1)
    }

    @Test
    fun format_picksAReadableUnit() {
        assertEquals("<50 m", Geo.format(0.02))
        assertEquals("350 m", Geo.format(0.352))
        assertEquals("3.2 km", Geo.format(3.24))
        assertEquals("1.0 km", Geo.format(0.99))
        assertEquals("120 km", Geo.format(119.6))
    }
}
