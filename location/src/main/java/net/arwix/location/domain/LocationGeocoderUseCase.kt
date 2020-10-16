package net.arwix.location.domain

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.FlowCollector
import net.arwix.location.data.GeocoderRepository
import net.arwix.location.ui.position.LocationPositionResult

class LocationGeocoderUseCase(private val geocoderRepository: GeocoderRepository) {

    suspend fun geocode(
        collector: FlowCollector<LocationPositionResult>,
        latLng: LatLng,
        cameraPosition: CameraPosition?
    ) {
        val address = runCatching {
            geocoderRepository.getAddress(latLng.latitude, latLng.longitude)
        }.getOrNull()
        address ?: run {
            collector.emit(LocationPositionResult.ErrorGeocoder(latLng, cameraPosition))
            return
        }
        collector.emit(
            LocationPositionResult.SuccessGeocoder(
                address,
                latLng,
                cameraPosition
            )
        )
    }

}