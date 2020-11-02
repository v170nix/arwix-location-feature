package net.arwix.location.list.ui

import net.arwix.location.data.room.LocationTimeZoneData

data class LocationListState(
    val isAutoLocationUpdate: Boolean = false,
//    val autoLocationTimeZoneData: LocationTimeZoneData? = null,
    val autoLocationPermission: LocationPermission = LocationPermission.DeniedRationale,
    val isSelected: Boolean = false,
    val locationList: List<LocationTimeZoneData>? = null
) {
    sealed class LocationPermission {
        object Denied : LocationPermission()
        object DeniedRationale : LocationPermission()
        object Allow : LocationPermission()
    }
}