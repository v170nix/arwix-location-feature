package net.arwix.location.domain

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.arwix.location.data.GeocoderRepository
import net.arwix.location.ui.position.LocationPositionResult

@Deprecated("not use")
class LocationGeocoderUseCase(private val geocoderRepository: GeocoderRepository) {

    fun geocode(
        latLng: LatLng,
        cameraPosition: CameraPosition?
    ): Flow<LocationPositionResult> = flow {
        val address = geocoderRepository.getAddressOrNull(latLng.latitude, latLng.longitude)
        if (address != null) {
            emit(
                LocationPositionResult.SuccessGeocoder(
                    address,
                    latLng,
                    cameraPosition
                )
            )
        } else emit(LocationPositionResult.ErrorGeocoder(latLng, cameraPosition))
    }

}