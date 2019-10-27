package net.arwix.location.ui.list

import net.arwix.location.data.room.LocationTimeZoneData

data class LocationListState(
    val isAutoLocationUpdate: Boolean = false,
    val autoLocationTimeZoneData: LocationTimeZoneData? = null,
    val autoLocationPermission: LocationPermission = LocationPermission.DeniedRationale,
    val selectedItem: SelectedItem? = null
) {
    data class SelectedItem(val data: LocationTimeZoneData, val isAuto: Boolean)

    sealed class LocationPermission {
        object Denied : LocationPermission()
        object DeniedRationale : LocationPermission()
        object Allow : LocationPermission()
    }
}