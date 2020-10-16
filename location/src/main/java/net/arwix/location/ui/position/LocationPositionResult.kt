package net.arwix.location.ui.position

import android.location.Address
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import net.arwix.location.common.extension.getSubTitle
import net.arwix.location.data.EditLocationSubData
import net.arwix.location.data.getSubTitle
import net.arwix.location.data.getTitle

sealed class LocationPositionResult {
    data class SuccessPlace(val subData: EditLocationSubData) : LocationPositionResult() {
        constructor(place: Place, cameraPosition: CameraPosition?) : this(
            EditLocationSubData(
                place.name.orEmpty(),
                place.getSubTitle(),
                place.latLng!!,
                cameraPosition
            )
        )
    }

    data class ProgressGeocoderFromMap(val subData: EditLocationSubData) :
        LocationPositionResult() {
        constructor(latLng: LatLng, cameraPosition: CameraPosition?) : this(
            EditLocationSubData("", "", latLng, cameraPosition)
        )
    }

    data class SuccessGeocoder(val subData: EditLocationSubData) : LocationPositionResult() {
        constructor(address: Address, latLng: LatLng, cameraPosition: CameraPosition?) : this(
            EditLocationSubData(
                address.getTitle(),
                address.getSubTitle(),
                latLng,
                cameraPosition
            )
        )
    }

    data class ProgressGeocoderFromInput(val subData: EditLocationSubData) :
        LocationPositionResult() {
        constructor(latLng: LatLng, cameraPosition: CameraPosition?) : this(
            EditLocationSubData("", "", latLng, cameraPosition)
        )
    }

    data class ErrorGeocoder(val subData: EditLocationSubData) : LocationPositionResult() {
        constructor(latLng: LatLng, cameraPosition: CameraPosition?) : this(
            EditLocationSubData("", "", latLng, cameraPosition)
        )
    }

    data class InitData(val subData: EditLocationSubData?) : LocationPositionResult()

    data class ErrorPlaceLatLng(val place: Place) : LocationPositionResult()
    data class ErrorInput(val latitude: Double?, val longitude: Double?) : LocationPositionResult()
}