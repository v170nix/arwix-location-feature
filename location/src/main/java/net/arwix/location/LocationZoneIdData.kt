package net.arwix.location

import org.threeten.bp.ZoneId

sealed class LocationZoneId(
    val name: String?,
    val subName: String?,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val zoneId: ZoneId
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

    class Manual(
        val id: Int,
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
}