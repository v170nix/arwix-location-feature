package net.arwix.location.data

import org.threeten.bp.ZoneId

sealed class LocationZoneId(
    open val name: String?,
    open val subName: String?,
    open val latitude: Double,
    open val longitude: Double,
    open val altitude: Double,
    open val zoneId: ZoneId
) {
    class Auto(
        name: String?,
        subName: String?,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        zoneId: ZoneId
    ) :
        LocationZoneId(
            name, subName, latitude, longitude, altitude, zoneId
        )

    data class Manual(
        val id: Int,
        override val name: String?,
        override val subName: String?,
        override val latitude: Double,
        override val longitude: Double,
        override val altitude: Double,
        override val zoneId: ZoneId
    ) :
        LocationZoneId(
            name, subName, latitude, longitude, altitude, zoneId
        )
}