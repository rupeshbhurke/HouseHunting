package com.rupeshbhurke.househunter.core.util

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    private const val EARTH_RADIUS_KM = 6371.0088

    /** Straight-line (great-circle) distance between two positions, by the haversine formula. */
    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }

    /** "<50 m", "850 m" (to the nearest 50), "3.2 km", "120 km". */
    fun format(km: Double): String = when {
        km < 0.05 -> "<50 m"
        km < 0.975 -> "${Math.round(km * 1000 / 50) * 50} m"
        km < 100.0 -> String.format(Locale.US, "%.1f km", km)
        else -> "${Math.round(km)} km"
    }
}
