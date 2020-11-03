package net.arwix.location.list.ui

import android.app.Activity
import net.arwix.location.data.room.LocationTimeZoneData
import net.arwix.mvi.StateViewModel
import net.arwix.mvi.StateViewModel.ActionType
import java.lang.ref.WeakReference

sealed class LocationListAction : StateViewModel.Action() {
    data class CheckPermission(
        val refActivity: WeakReference<Activity>? = null,
        override val type: ActionType = ActionType.Sync
    ) : LocationListAction()

    object GetAutoLocation : LocationListAction() {
        override val type: ActionType = ActionType.Sync
    }

    object UpdateAutoLocation : LocationListAction() {
        override val type: ActionType = ActionType.Sync
    }

    object CancelUpdateAutoLocation : LocationListAction() {
        override val type: ActionType get() = ActionType.Sync
    }

    data class DeleteItem(
        val item: LocationTimeZoneData,
        override val type: ActionType = ActionType.Sync
    ) : LocationListAction()

    data class EditItem(
        val item: LocationTimeZoneData,
        override val type: ActionType = ActionType.Sync
    ) : LocationListAction()

    data class SelectFromCustomList(
        val data: LocationTimeZoneData,
        override val type: ActionType = ActionType.Latest
    ) : LocationListAction()

    data class SelectFormAuto(
        val data: LocationTimeZoneData,
        override val type: ActionType = ActionType.Latest
    ) : LocationListAction()
}