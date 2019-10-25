package net.arwix.location.ui.position

import android.location.Address
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import net.arwix.location.common.extension.getSubTitle
import net.arwix.location.data.FeatureLocationData
import net.arwix.location.data.getSubTitle
import net.arwix.location.data.getTitle

sealed class LocationPositionResult {
    data class SuccessPlace(val data: FeatureLocationData) : LocationPositionResult() {
        constructor(place: Place, cameraPosition: CameraPosition?) : this(
            FeatureLocationData(
                place.name.orEmpty(),
                place.getSubTitle(),
                place.latLng!!,
                cameraPosition
            )
        )
    }

    data class ProgressGeocoderFromMap(val data: FeatureLocationData) : LocationPositionResult() {
        constructor(latLng: LatLng, cameraPosition: CameraPosition?) : this(
            FeatureLocationData("", "", latLng, cameraPosition)
        )
    }

    data class SuccessGeocoder(val data: FeatureLocationData) : LocationPositionResult() {
        constructor(address: Address, latLng: LatLng, cameraPosition: CameraPosition?) : this(
            FeatureLocationData(
                address.getTitle(),
                address.getSubTitle(),
                latLng,
                cameraPosition
            )
        )
    }

    data class ProgressGeocoderFromInput(val data: FeatureLocationData) : LocationPositionResult() {
        constructor(latLng: LatLng, cameraPosition: CameraPosition?) : this(
            FeatureLocationData("", "", latLng, cameraPosition)
        )
    }

    data class ErrorGeocoder(val data: FeatureLocationData) : LocationPositionResult() {
        constructor(latLng: LatLng, cameraPosition: CameraPosition?) : this(
            FeatureLocationData("", "", latLng, cameraPosition)
        )
    }

    data class InitData(val data: FeatureLocationData?) : LocationPositionResult()

    data class ErrorPlaceLatLng(val place: Place) : LocationPositionResult()
    data class ErrorInput(val latitude: Double?, val longitude: Double?) : LocationPositionResult()
}