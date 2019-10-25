package net.arwix.location

import kotlinx.coroutines.channels.ReceiveChannel

interface LocationZoneIdDatabase {
    suspend fun setLZData(data: LocationZoneId): Boolean
    suspend fun getLZData(): LocationZoneId?
    fun share(): ReceiveChannel<LocationZoneId>
}