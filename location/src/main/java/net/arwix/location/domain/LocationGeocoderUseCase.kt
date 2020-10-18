package net.arwix.location.domain

import android.util.Log
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.arwix.location.data.GeocoderRepository
import net.arwix.location.ui.position.LocationPositionResult

class LocationGeocoderUseCase(private val geocoderRepository: GeocoderRepository) {

    fun geocode(
        latLng: LatLng,
        cameraPosition: CameraPosition?
    ): Flow<LocationPositionResult> = flow {
        Log.e("error!!", "5")
        val address = runCatching {
            geocoderRepository.getAddress(latLng.latitude, latLng.longitude)
        }.getOrNull()
        Log.e("error!!", address.toString())
        if (address != null) {
            emit(
                LocationPositionResult.SuccessGeocoder(
                    address,
                    latLng,
                    cameraPosition
                )
            )
        } else
            emit(LocationPositionResult.ErrorGeocoder(latLng, cameraPosition))
    }

}