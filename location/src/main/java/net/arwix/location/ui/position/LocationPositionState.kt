package net.arwix.location.ui.position


import com.google.android.libraries.places.api.model.Place
import net.arwix.location.data.EditLocationData

data class LocationPositionState(
    val updateMapAfterChangeLocation: Boolean = false,
    val data: EditLocationData? = null,
    val nextStepIsAvailable: Boolean = false,
    val error: ErrorState? = null
) {
    sealed class ErrorState {
        data class PlaceLatLng(val place: Place) : ErrorState()
        object Geocoder : ErrorState()
        data class Input(val latitude: Double?, val longitude: Double?) : ErrorState()
    }
}

