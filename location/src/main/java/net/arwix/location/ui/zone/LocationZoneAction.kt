package net.arwix.location.ui.zone

import com.google.android.gms.maps.model.LatLng
import org.threeten.bp.ZoneId

sealed class LocationZoneAction {
    object Init : LocationZoneAction()
    object LoadZoneList : LocationZoneAction()
    data class UpdateAutoZone(val latLng: LatLng) : LocationZoneAction()
    data class SelectZoneFromList(val zone: ZoneId) : LocationZoneAction()
    data class SelectZoneFormAuto(val zone: ZoneId) : LocationZoneAction()
}