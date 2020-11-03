package net.arwix.location.list.ui

import android.app.Activity
import net.arwix.location.data.room.LocationTimeZoneData
import net.arwix.mvi.FlowViewModel
import net.arwix.mvi.FlowViewModel.LatestAction
import net.arwix.mvi.FlowViewModel.SyncAction
import java.lang.ref.WeakReference

sealed class LocationListAction : FlowViewModel.Action {
    data class CheckPermission(val refActivity: WeakReference<Activity>? = null) :
        LocationListAction(), SyncAction

    object GetAutoLocation : LocationListAction(), SyncAction
    object UpdateAutoLocation : LocationListAction(), SyncAction
    object CancelUpdateAutoLocation : LocationListAction(), SyncAction
    data class DeleteItem(val item: LocationTimeZoneData) : LocationListAction(), SyncAction
    data class EditItem(val item: LocationTimeZoneData) : LocationListAction(), SyncAction
    data class SelectFromCustomList(val data: LocationTimeZoneData) : LocationListAction(),
        LatestAction

    data class SelectFormAuto(val data: LocationTimeZoneData) : LocationListAction(), LatestAction
}