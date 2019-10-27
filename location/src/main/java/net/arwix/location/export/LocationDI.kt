package net.arwix.location.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.arwix.location.LocationZoneIdDatabase
import net.arwix.location.data.GeocoderRepository
import net.arwix.location.data.LocationCreateEditRepository
import net.arwix.location.data.room.LocationDao
import net.arwix.location.ui.list.LocationListViewModel

@Suppress("UNCHECKED_CAST")
fun createLocationMainFactory(
    locationDatabase: LocationZoneIdDatabase,
    dao: LocationDao,
    editRepository: LocationCreateEditRepository,
    geocoderRepository: GeocoderRepository
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return LocationListViewModel(
                locationDatabase, dao, editRepository, geocoderRepository
            ) as T
        }
    }
}