package com.rupeshbhurke.househunter.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

data class LatLng(val lat: Double, val lng: Double)

object Coordinates {
    /**
     * Parses "18.5204, 73.8567" (the format Google Maps copies), or the two numbers separated by
     * whitespace or a semicolon. Returns null for anything else or out-of-range values.
     */
    fun parse(text: String): LatLng? {
        val parts = text.trim().split(Regex("[,;\\s]+")).filter { it.isNotEmpty() }
        if (parts.size != 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        return LatLng(lat, lng)
    }

    fun format(lat: Double, lng: Double): String = String.format(Locale.US, "%.5f, %.5f", lat, lng)

    fun accuracy(metres: Float): String = "±${Math.round(metres)} m"
}

object MapActions {
    /** Opens the position in any maps app, or in the browser if none is installed. */
    fun open(context: Context, lat: Double, lng: Double, label: String) {
        val point = String.format(Locale.US, "%.6f,%.6f", lat, lng)
        val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$point?q=$point(${Uri.encode(label)})"))
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/search/?api=1&query=$point"),
        )
        try {
            context.startActivity(geo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: ActivityNotFoundException) {
                // Nothing can open a map; nothing useful to do.
            }
        }
    }
}
