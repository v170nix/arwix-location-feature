package net.arwix.location.edit.zone.ui

import net.arwix.mvi.FlowViewModel
import org.threeten.bp.ZoneId

sealed class LocationZoneAction : FlowViewModel.Action {
    object LoadZoneList : LocationZoneAction(), FlowViewModel.MergeAction

    //    data class UpdateAutoZone(val latLng: LatLng) : LocationZoneAction(), FlowViewModel
    data class SelectZoneFromList(val zone: ZoneId) : LocationZoneAction(),
        FlowViewModel.MergeAction

    data class SelectZoneFormAuto(val zone: ZoneId) : LocationZoneAction(),
        FlowViewModel.MergeAction
}