package net.arwix.location.export

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.arwix.location.LocationZoneIdSelectedDatabase
import net.arwix.location.data.GeocoderRepository
import net.arwix.location.data.LocationCreateEditRepository
import net.arwix.location.data.room.LocationDao
import net.arwix.location.domain.LocationGeocoderUseCase
import net.arwix.location.ui.list.LocationListViewModel
import net.arwix.location.ui.position.LocationPositionViewModel

@Suppress("UNCHECKED_CAST")
fun createLocationListFactory(
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

fun createLocationPositionFactory(
    editRepository: LocationCreateEditRepository,
    locationGeocoderUseCase: LocationGeocoderUseCase
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return LocationPositionViewModel(
                editRepository, locationGeocoderUseCase
            ) as T
        }
    }
}