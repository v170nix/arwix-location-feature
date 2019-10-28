package net.arwix.location.ui.list

import android.app.Activity
import net.arwix.location.data.room.LocationTimeZoneData
import java.lang.ref.WeakReference

sealed class LocationListAction {
    data class CheckPermission(val refActivity: WeakReference<Activity>? = null) :
        LocationListAction()

    object GetAutoLocation : LocationListAction()
    object UpdateAutoLocation : LocationListAction()
    object CancelUpdateAutoLocation : LocationListAction()
    data class DeleteItem(val item: LocationTimeZoneData) : LocationListAction()
    data class EditItem(val item: LocationTimeZoneData) : LocationListAction()
    object AddItem : LocationListAction()
    data class SelectFromCustomList(val data: LocationTimeZoneData) : LocationListAction()
    data class SelectFormAuto(val data: LocationTimeZoneData) : LocationListAction()
}