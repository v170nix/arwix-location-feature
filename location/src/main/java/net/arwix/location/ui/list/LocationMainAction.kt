package net.arwix.location.ui.list

import android.app.Activity
import net.arwix.location.data.room.LocationTimeZoneData
import java.lang.ref.WeakReference

sealed class LocationMainAction {
    data class CheckPermission(val refActivity: WeakReference<Activity>) : LocationMainAction()
    data class GetLocation(val refActivity: WeakReference<Activity>) : LocationMainAction()
    data class UpdateAutoLocation(val refActivity: WeakReference<Activity>) : LocationMainAction()
    object CancelUpdateAutoLocation : LocationMainAction()
    data class Delete(val item: LocationTimeZoneData) : LocationMainAction()
    data class Edit(val item: LocationTimeZoneData) : LocationMainAction()
    object Add : LocationMainAction()
    data class SelectFromList(val data: LocationTimeZoneData) : LocationMainAction()
    data class SelectFormAuto(val data: LocationTimeZoneData) : LocationMainAction()
}