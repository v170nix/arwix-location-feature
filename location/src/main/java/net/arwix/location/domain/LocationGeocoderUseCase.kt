package net.arwix.location.domain

import androidx.lifecycle.LiveDataScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.arwix.location.data.GeocoderRepository
import net.arwix.location.ui.position.LocationPositionResult

class LocationGeocoderUseCase(private val geocoderRepository: GeocoderRepository) {

    suspend fun geocode(
        scope: LiveDataScope<LocationPositionResult>,
        latLng: LatLng,
        cameraPosition: CameraPosition?
    ) = withContext(Dispatchers.IO) {
        val address = runCatching {
            geocoderRepository.getAddress(latLng.latitude, latLng.longitude)
        }.getOrNull()
        address ?: run {
            scope.emit(LocationPositionResult.ErrorGeocoder(latLng, cameraPosition))
            return@withContext
        }
        scope.emit(
            LocationPositionResult.SuccessGeocoder(
                address,
                latLng,
                cameraPosition
            )
        )
    }

}