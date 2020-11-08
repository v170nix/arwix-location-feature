@file:Suppress("UNCHECKED_CAST")

package net.arwix.location.export

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.arwix.location.data.GeocoderRepository
import net.arwix.location.data.TimeZoneGoogleRepository
import net.arwix.location.data.TimeZoneRepository
import net.arwix.location.data.room.LocationDao
import net.arwix.location.edit.domain.LocationCreateEditUseCase
import net.arwix.location.edit.position.ui.LocationPositionViewModel
import net.arwix.location.edit.zone.ui.LocationZoneViewModel
import net.arwix.location.list.ui.LocationListViewModel

fun createLocationListFactory(
    applicationContext: Context,
    dao: LocationDao,
    editUseCase: LocationCreateEditUseCase,
    geocoderRepository: GeocoderRepository
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return LocationListViewModel(
                applicationContext, dao, editUseCase, geocoderRepository
            ) as T
        }
    }
}

fun createLocationPositionFactory(
    editUseCase: LocationCreateEditUseCase,
    geocoderRepository: GeocoderRepository
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return LocationPositionViewModel(
                editUseCase, geocoderRepository
            ) as T
        }
    }
}

fun createLocationZoneFactory(
    editUseCase: LocationCreateEditUseCase,
    timeZoneRepository: TimeZoneRepository,
    googleZoneRepository: TimeZoneGoogleRepository
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return LocationZoneViewModel(
                editUseCase, timeZoneRepository, googleZoneRepository
            ) as T
        }
    }
}