package com.rupeshbhurke.househunter.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** A position and how far off it may be, in metres (null when the source didn't say). */
data class Fix(val lat: Double, val lng: Double, val accuracyM: Float?)

@Singleton
class LocationClient @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val fused = LocationServices.getFusedLocationProviderClient(context)

    /**
     * One high-accuracy fix, falling back to the last known position. Returns null when no position
     * could be had in time (location switched off, no signal) or the permission is missing.
     */
    @SuppressLint("MissingPermission")
    suspend fun currentFix(): Fix? = try {
        val fresh = withTimeoutOrNull(FIX_TIMEOUT_MS) {
            val token = CancellationTokenSource()
            suspendCancellableCoroutine<Location?> { cont ->
                cont.invokeOnCancellation { token.cancel() }
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).resumeWith(cont)
            }
        }
        (fresh ?: lastKnown())?.toFix()
    } catch (_: SecurityException) {
        null
    }

    @SuppressLint("MissingPermission")
    private suspend fun lastKnown(): Location? =
        suspendCancellableCoroutine { cont -> fused.lastLocation.resumeWith(cont) }

    private fun Task<Location?>.resumeWith(cont: kotlinx.coroutines.CancellableContinuation<Location?>) {
        addOnSuccessListener { if (cont.isActive) cont.resume(it) }
        addOnFailureListener { if (cont.isActive) cont.resume(null) }
    }

    private fun Location.toFix() = Fix(latitude, longitude, if (hasAccuracy()) accuracy else null)

    private companion object {
        const val FIX_TIMEOUT_MS = 20_000L
    }
}
