package com.shomerapp.alerts.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Wraps FusedLocationProviderClient + Geocoder for the opt-in "auto-detect my settlement"
 *  feature (README "מיקום אוטומטי"). Never requests permission itself — callers must already
 *  hold it; every method returns null/empty rather than throwing when it doesn't. */
@Singleton
class LocationTracker @Inject constructor(@ApplicationContext private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    /** A single best-effort location fix, or null if permission is missing or the fix fails. */
    suspend fun currentLocation(): Location? {
        // Inline, not extracted to a helper — Android Lint's MissingPermission check only
        // recognizes a permission guard written directly before the guarded call.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            val cancellationSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationSource.token)
                .addOnSuccessListener { location -> continuation.resume(location) }
                .addOnFailureListener { continuation.resume(null) }
            continuation.invokeOnCancellation { cancellationSource.cancel() }
        }
    }

    /** Reverse-geocodes into candidate place names (locality-first) for
     *  [com.shomerapp.alerts.domain.LocationSettlementMatcher] to try in order. Runs on
     *  Dispatchers.IO — Geocoder's synchronous overload blocks on network I/O. */
    suspend fun placeNameCandidates(location: Location): List<String> = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext emptyList()
        val geocoder = Geocoder(context, Locale.forLanguageTag("he-IL"))
        @Suppress("DEPRECATION") // the async listener overload requires API 33+; this app's minSdk is 26
        val addresses = runCatching { geocoder.getFromLocation(location.latitude, location.longitude, 1) }.getOrNull().orEmpty()
        val address = addresses.firstOrNull() ?: return@withContext emptyList()
        listOfNotNull(address.locality, address.subLocality, address.subAdminArea, address.featureName)
    }
}
