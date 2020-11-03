package net.arwix.location.ui.position

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import net.arwix.mvi.FlowViewModel

sealed class LocationPositionAction : FlowViewModel.Action {

//    object Init : LocationPositionAction(), FlowViewModel.SyncAction

    data class ChangeFromPlace(
        val place: Place,
        val cameraPosition: CameraPosition?
    ) : LocationPositionAction(), FlowViewModel.LatestAction

    data class ChangeFromMap(
        val latLng: LatLng,
        val cameraPosition: CameraPosition?
    ) : LocationPositionAction(), FlowViewModel.LatestAction

    data class ChangeFromInput(
        val latitude: Double?,
        val longitude: Double?,
        val cameraPosition: CameraPosition?
    ) : LocationPositionAction(), FlowViewModel.LatestAction
}