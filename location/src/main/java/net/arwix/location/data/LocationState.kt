package net.arwix.location.data

import net.arwix.location.data.room.LocationTimeZoneData

sealed class LocationState {
    object Loading : LocationState()
    object Empty : LocationState()
    data class Current(val value: LocationTimeZoneData) : LocationState()
}