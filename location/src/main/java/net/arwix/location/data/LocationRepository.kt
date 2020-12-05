@file:Suppress("unused")

package net.arwix.location.data

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.arwix.location.data.room.LocationDao
import net.arwix.location.data.room.LocationTimeZoneData

class LocationRepository(private val dao: LocationDao) {

    suspend fun getLastLocation(): LocationTimeZoneData? = dao.getSelectedItem()

    val location: StateFlow<LocationState> = dao.getSelectedItemAsFlow()
        .map {
            if (it == null) LocationState.Empty else LocationState.Current(it)
        }
        .stateIn(
            ProcessLifecycleOwner.get().lifecycleScope,
            SharingStarted.WhileSubscribed(),
            LocationState.Loading
        )

}