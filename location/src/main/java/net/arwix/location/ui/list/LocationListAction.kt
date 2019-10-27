package net.arwix.location.ui.list

import android.app.Activity
import net.arwix.location.data.room.LocationTimeZoneData
import java.lang.ref.WeakReference

sealed class LocationListAction {
    data class CheckPermission(val refActivity: WeakReference<Activity>) : LocationListAction()
    data class GetLocation(val refActivity: WeakReference<Activity>) : LocationListAction()
    data class UpdateAutoLocation(val refActivity: WeakReference<Activity>) : LocationListAction()
    object CancelUpdateAutoLocation : LocationListAction()
    data class Delete(val item: LocationTimeZoneData) : LocationListAction()
    data class Edit(val item: LocationTimeZoneData) : LocationListAction()
    object Add : LocationListAction()
    data class SelectFromList(val data: LocationTimeZoneData) : LocationListAction()
    data class SelectFormAuto(val data: LocationTimeZoneData) : LocationListAction()
}