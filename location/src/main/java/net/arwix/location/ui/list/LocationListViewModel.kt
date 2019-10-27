package net.arwix.location.ui.list

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataScope
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.*
import net.arwix.location.LocationZoneId
import net.arwix.location.LocationZoneIdDatabase
import net.arwix.location.data.GeocoderRepository
import net.arwix.location.data.LocationCreateEditRepository
import net.arwix.location.data.room.LocationDao
import net.arwix.location.data.room.LocationTimeZoneData
import net.arwix.location.domain.LocationHelper
import net.arwix.location.domain.LocationPermissionHelper
import net.arwix.mvi.SimpleIntentViewModel
import org.threeten.bp.ZoneId

class LocationListViewModel(
    private val database: LocationZoneIdDatabase,
    private val dao: LocationDao,
    private val editRepository: LocationCreateEditRepository,
    private val geocoderRepository: GeocoderRepository
) : SimpleIntentViewModel<LocationListAction, LocationListResult, LocationListState>() {

    override var internalViewState: LocationListState = LocationListState()
    val listItems = dao.getAll()
    private val updateLocationJobs = mutableListOf<Job>()

    init {
        Log.e("init", "LocationListViewModel")
        viewModelScope.launch {
            val dbItem = database.getLZData() ?: return@launch

            val innerData = LocationTimeZoneData(
                if (dbItem is LocationZoneId.Manual) dbItem.id else null,
                dbItem.name, dbItem.subName, LatLng(dbItem.latitude, dbItem.longitude),
                dbItem.zoneId
            )
            yield()
            notificationFromObserver(
                LocationListResult.Select(
                    innerData, (dbItem is LocationZoneId.Auto)
                )
            )
        }
    }

    @Synchronized
    private fun updateJobsCancel(newJob: Job? = null) {
        synchronized(this) {
            updateLocationJobs.forEach { it.cancel() }
            updateLocationJobs.clear()
            newJob?.run(updateLocationJobs::add)
        }
    }

    private suspend fun getLocation(
        applicationContext: Context,
        flagUpdate: Boolean = false,
        scope: LiveDataScope<LocationListResult>
    ) {
        val job = viewModelScope.launch(Dispatchers.Main, CoroutineStart.LAZY) {
            val location = runCatching {
                if (flagUpdate) {
                    LocationHelper.updateOneAndGetLastLocation(applicationContext)
                } else {
                    LocationHelper.getLastLocation(applicationContext)
                }
            }.getOrNull()
            if (location != null) {
                val zoneId = ZoneId.systemDefault()
                scope.emit(
                    LocationListResult.AutoLocation(
                        LocationListResult.LocationMainAutoResult.Success(
                            location,
                            zoneId
                        )
                    )
                )
                val address = runCatching {
                    withContext(Dispatchers.IO) {
                        geocoderRepository.getAddress(location.latitude, location.longitude)
                    }
                }.getOrNull() ?: run {
                    if (flagUpdate) scope.emit(
                        LocationListResult.AutoLocation(
                            LocationListResult.LocationMainAutoResult.UpdateEnd(
                                null
                            )
                        )
                    )
                    return@launch
                }
                val result = if (flagUpdate) {
                    LocationListResult.AutoLocation(
                        LocationListResult.LocationMainAutoResult.UpdateEnd(
                            location,
                            zoneId,
                            address
                        )
                    )
                } else {
                    LocationListResult.AutoLocation(
                        LocationListResult.LocationMainAutoResult.Success(
                            location,
                            zoneId,
                            address
                        )
                    )
                }
                scope.emit(result)

            } else {
                scope.emit(
                    LocationListResult.AutoLocation(
                        LocationListResult.LocationMainAutoResult.None(
                            LocationPermissionHelper.check(applicationContext)
                        )
                    )
                )
            }
        }
        updateJobsCancel(job)
        job.start()
        job.join()
    }

    suspend fun submit(): Boolean {
        internalViewState.selectedItem?.let { selectedItem ->
            val selectedId = selectedItem.data.id
            if (selectedId == null && !selectedItem.isAuto) return false
            val data = if (selectedItem.isAuto) {
                LocationZoneId.Auto(
                    name = selectedItem.data.name,
                    subName = selectedItem.data.subName,
                    latitude = selectedItem.data.latLng.latitude,
                    longitude = selectedItem.data.latLng.longitude,
                    altitude = 0.0,
                    zoneId = selectedItem.data.zone
                )
            } else {
                LocationZoneId.Manual(
                    id = selectedId!!,
                    name = selectedItem.data.name,
                    subName = selectedItem.data.subName,
                    latitude = selectedItem.data.latLng.latitude,
                    longitude = selectedItem.data.latLng.longitude,
                    altitude = 0.0,
                    zoneId = selectedItem.data.zone
                )
            }
            return database.setLZData(data)
        }
        return false
    }

    override fun dispatchAction(action: LocationListAction): LiveData<LocationListResult> {
        return liveData {
            when (action) {
                is LocationListAction.UpdateAutoLocation -> {
                    val activity = action.refActivity.get() ?: return@liveData
                    emit(LocationListResult.AutoLocation(LocationListResult.LocationMainAutoResult.UpdateBegin))
                    getLocation(activity.applicationContext, true, this)
                }
                LocationListAction.CancelUpdateAutoLocation -> {
                    updateJobsCancel()
                    emit(
                        LocationListResult.AutoLocation(
                            LocationListResult.LocationMainAutoResult.UpdateEnd(
                                null
                            )
                        )
                    )
                }
                is LocationListAction.GetLocation -> {
                    val activity = action.refActivity.get() ?: return@liveData
                    getLocation(activity.applicationContext, false, this)
                }
                is LocationListAction.CheckPermission -> {
                    val activity = action.refActivity.get() ?: return@liveData
                    val isAllow = LocationPermissionHelper.check(activity)
                    if (!isAllow) {
                        emit(
                            LocationListResult
                                .PermissionDenied((LocationPermissionHelper.isRationale(activity)))
                        )
                        return@liveData
                    }
                    emit(LocationListResult.PermissionGranted)
                }

                is LocationListAction.SelectFormAuto -> {
                    emit(LocationListResult.Select(action.data, true))
                }
                is LocationListAction.SelectFromList -> {
                    emit(LocationListResult.Select(action.data, false))
                }
                is LocationListAction.Delete -> {
                    action.item.id?.let {
                        dao.deleteById(it)
                        if (internalViewState.selectedItem?.data?.id == it) {
                            emit(LocationListResult.Deselect)
                        }
                    }
                }
                is LocationListAction.Edit -> {
                    editRepository.edit(action.item)
                }
                LocationListAction.Add -> {
                    withContext(Dispatchers.Main) {
                        editRepository.create()
                    }
                }
            }
        }
    }

    override fun reduce(result: LocationListResult): LocationListState {
        return when (result) {
            is LocationListResult.Select -> internalViewState.copy(
                selectedItem = LocationListState.SelectedItem(result.item, result.isAuto)
            )
            is LocationListResult.Deselect -> internalViewState.copy(
                selectedItem = null
            )
            is LocationListResult.PermissionGranted -> internalViewState.copy(
                autoLocationPermission = LocationListState.LocationPermission.Allow
            )
            is LocationListResult.PermissionDenied -> internalViewState.copy(
                autoLocationPermission = if (result.shouldRationale)
                    LocationListState.LocationPermission.DeniedRationale
                else
                    LocationListState.LocationPermission.Denied
            )
            is LocationListResult.AutoLocation -> {
                when (result.locationResult) {
                    is LocationListResult.LocationMainAutoResult.None -> internalViewState.copy(
                        autoLocationPermission = if (result.locationResult.isPermissionAllow)
                            LocationListState.LocationPermission.Allow else
                            LocationListState.LocationPermission.Denied,
                        autoLocationTimeZoneData = null
                    )
                    is LocationListResult.LocationMainAutoResult.UpdateBegin -> internalViewState.copy(
                        isAutoLocationUpdate = true
                    )
                    is LocationListResult.LocationMainAutoResult.UpdateEnd -> internalViewState.copy(
                        isAutoLocationUpdate = false,
                        autoLocationTimeZoneData = result.locationResult.location
                            ?: internalViewState.autoLocationTimeZoneData
                    )
                    is LocationListResult.LocationMainAutoResult.Success -> internalViewState.copy(
                        autoLocationPermission = LocationListState.LocationPermission.Allow,
                        autoLocationTimeZoneData = result.locationResult.location
                    )
                }
            }
        }
    }

}