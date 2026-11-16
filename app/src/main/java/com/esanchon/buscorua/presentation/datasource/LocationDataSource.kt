package com.esanchon.buscorua.presentation.datasource

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Posición actual del reloj. Basta con precisión aproximada (~100 m): solo hay que
 * distinguir si estás cerca de casa o del trabajo, y así gasta menos batería.
 */
class LocationDataSource(context: Context) {

    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * "Permitir siempre": la complicación "Bus Trabajo-Casa" puede leer la posición
     * sin tener la app abierta. Antes de Android 10 no existe y basta el permiso normal.
     */
    fun hasBackgroundPermission(): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) hasPermission()
        else hasPermission() && ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    /** null sin permiso, con la ubicación desactivada o si no se consigue a tiempo. */
    @SuppressLint("MissingPermission") // comprobado en hasPermission()
    suspend fun getLocation(): Location? {
        if (!hasPermission()) return null
        return try {
            val last = client.lastLocation.await()
            if (last != null && last.ageMs() < FRESH_MS) return last

            val tokenSource = CancellationTokenSource()
            val current = withTimeoutOrNull(TIMEOUT_MS) {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token).await()
            }
            if (current == null) tokenSource.cancel()

            // Si no hay posición nueva, vale una algo antigua (no te mueves tan rápido de casa al trabajo)
            current ?: last?.takeIf { it.ageMs() < STALE_MS }
        } catch (err: Exception) {
            err.printStackTrace()
            null
        }
    }

    private fun Location.ageMs() = (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000

    private suspend fun <T> Task<T>.await(): T? = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task -> cont.resume(if (task.isSuccessful) task.result else null) }
    }

    companion object {
        private const val FRESH_MS = 2L * 60 * 1000
        private const val STALE_MS = 15L * 60 * 1000
        private const val TIMEOUT_MS = 15_000L
    }
}
