package net.arwix.location.ui.position


import com.google.android.libraries.places.api.model.Place
import net.arwix.location.data.FeatureLocationData

data class LocationPositionState(
    val updateMapAfterChangeLocation: Boolean = false,
    val data: FeatureLocationData? = null,
    val nextStepAvailable: Boolean = false,
    val error: ErrorState? = null
) {
    sealed class ErrorState {
        data class PlaceLatLng(val place: Place) : ErrorState()
        object Geocoder : ErrorState()
        data class Input(val latitude: Double?, val longitude: Double?) : ErrorState()
    }
}

