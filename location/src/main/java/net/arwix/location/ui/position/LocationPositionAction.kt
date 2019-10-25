package net.arwix.location.ui.position

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place

sealed class LocationPositionAction {

    object Init : LocationPositionAction()

    data class ChangeFromPlace(val place: Place, val cameraPosition: CameraPosition?) :
        LocationPositionAction()

    data class ChangeFromMap(val latLng: LatLng, val cameraPosition: CameraPosition?) :
        LocationPositionAction()

    data class ChangeFromInput(
        val latitude: Double?,
        val longitude: Double?,
        val cameraPosition: CameraPosition?
    ) : LocationPositionAction()
}