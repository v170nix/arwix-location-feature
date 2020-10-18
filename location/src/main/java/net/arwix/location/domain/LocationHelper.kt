package net.arwix.location.domain

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import net.arwix.location.common.extension.await
import kotlin.coroutines.resume

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(context: Context): Location? {
        val provider = LocationServices.getFusedLocationProviderClient(context)
        return runCatching { provider.lastLocation.await() }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    suspend fun updateOneAndGetLastLocation(context: Context): Location? {
        val provider = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest().apply {
            numUpdates = 1
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        return suspendCancellableCoroutine { cont ->
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult?) {
                    super.onLocationResult(result)
                    val location = result?.locations?.firstOrNull { it != null }
                    provider.removeLocationUpdates(this)
                    cont.resume(location)
                }
            }
            cont.invokeOnCancellation { provider.removeLocationUpdates(callback) }
            provider.requestLocationUpdates(request, callback, null)
        }
    }

    suspend fun getLocation(context: Context, flagUpdate: Boolean) = runCatching {
        if (flagUpdate) updateOneAndGetLastLocation(context)
        else getLastLocation(context)
    }.getOrNull()

}