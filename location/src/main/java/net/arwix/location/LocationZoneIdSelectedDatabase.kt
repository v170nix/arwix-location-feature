package net.arwix.location

import kotlinx.coroutines.channels.ReceiveChannel
import net.arwix.location.data.LocationZoneId

interface LocationZoneIdSelectedDatabase {
    suspend fun setLZData(data: LocationZoneId): Boolean
    suspend fun getLZData(): LocationZoneId?
    fun share(): ReceiveChannel<LocationZoneId>
}