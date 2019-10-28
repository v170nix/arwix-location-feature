package net.arwix.location.export

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.arwix.location.LocationZoneIdSelectedDatabase
import net.arwix.location.data.GeocoderRepository
import net.arwix.location.data.LocationCreateEditRepository
import net.arwix.location.data.room.LocationDao
import net.arwix.location.ui.list.LocationListViewModel

@Suppress("UNCHECKED_CAST")
fun createLocationMainFactory(
    applicationContext: Context,
    locationSelectedDatabase: LocationZoneIdSelectedDatabase,
    dao: LocationDao,
    editRepository: LocationCreateEditRepository,
    geocoderRepository: GeocoderRepository
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return LocationListViewModel(
                applicationContext,
                locationSelectedDatabase, dao, editRepository, geocoderRepository
            ) as T
        }
    }
}