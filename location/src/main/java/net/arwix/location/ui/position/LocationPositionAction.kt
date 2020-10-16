package net.arwix.location.ui.position

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import net.arwix.mvi.StateViewModel
import net.arwix.mvi.StateViewModel.ActionType

sealed class LocationPositionAction : StateViewModel.Action() {

    object Init : LocationPositionAction() {
        override val type: ActionType
            get() = ActionType.Sync
    }

    data class ChangeFromPlace(
        val place: Place, val cameraPosition: CameraPosition?,
        override val type: ActionType = ActionType.Latest
    ) :
        LocationPositionAction()

    data class ChangeFromMap(
        val latLng: LatLng, val cameraPosition: CameraPosition?,
        override val type: ActionType = ActionType.Latest
    ) :
        LocationPositionAction()

    data class ChangeFromInput(
        val latitude: Double?,
        val longitude: Double?,
        val cameraPosition: CameraPosition?, override val type: ActionType = ActionType.Latest
    ) : LocationPositionAction()
}