package net.arwix.location.ui.position

import android.location.Address
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import net.arwix.location.common.extension.getSubTitle
import net.arwix.location.data.EditLocationData
import net.arwix.location.data.getSubTitle
import net.arwix.location.data.getTitle

sealed class LocationPositionResult {
    data class SuccessPlace(val data: EditLocationData) : LocationPositionResult() {
        constructor(place: Place, cameraPosition: CameraPosition?) : this(
            EditLocationData(
                place.name.orEmpty(),
                place.getSubTitle(),
                place.latLng!!,
                cameraPosition
            )
        )
    }

    data class ProgressGeocoderFromMap(val data: EditLocationData) :
        LocationPositionResult() {
        constructor(latLng: LatLng, cameraPosition: CameraPosition?) : this(
            EditLocationData("", "", latLng, cameraPosition)
        )
    }

    data class SuccessGeocoder(val data: EditLocationData) : LocationPositionResult() {
        constructor(address: Address, latLng: LatLng, cameraPosition: CameraPosition?) : this(
            EditLocationData(
                address.getTitle(),
                address.getSubTitle(),
                latLng,
                cameraPosition
            )
        )
    }

    data class ProgressGeocoderFromInput(val data: EditLocationData) :
        LocationPositionResult() {
        constructor(latLng: LatLng, cameraPosition: CameraPosition?) : this(
            EditLocationData("", "", latLng, cameraPosition)
        )
    }

    data class ErrorGeocoder(val data: EditLocationData) : LocationPositionResult() {
        constructor(latLng: LatLng, cameraPosition: CameraPosition?) : this(
            EditLocationData("", "", latLng, cameraPosition)
        )
    }

    data class InitData(val data: EditLocationData?) : LocationPositionResult()

    data class ErrorPlaceLatLng(val place: Place) : LocationPositionResult()
    data class ErrorInput(val latitude: Double?, val longitude: Double?) : LocationPositionResult()
}