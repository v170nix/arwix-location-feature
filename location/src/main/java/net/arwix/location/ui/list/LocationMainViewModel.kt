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

class LocationMainViewModel(
    private val database: LocationZoneIdDatabase,
    private val dao: LocationDao,
    private val editRepository: LocationCreateEditRepository,
    private val geocoderRepository: GeocoderRepository
) : SimpleIntentViewModel<LocationMainAction, LocationMainResult, LocationMainState>() {

    override var internalViewState: LocationMainState = LocationMainState()
    val listItems = dao.getAll()
    private val updateLocationJobs = mutableListOf<Job>()

    init {
        Log.e("init", "LocationMainViewModel")
        viewModelScope.launch {
            val dbItem = database.getLZData() ?: return@launch

            val innerData = LocationTimeZoneData(
                if (dbItem is LocationZoneId.Manual) dbItem.id else null,
                dbItem.name, dbItem.subName, LatLng(dbItem.latitude, dbItem.longitude),
                dbItem.zoneId
            )
            yield()
            notificationFromObserver(
                LocationMainResult.Select(
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
        scope: LiveDataScope<LocationMainResult>
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
                    LocationMainResult.AutoLocation(
                        LocationMainResult.LocationMainAutoResult.Success(
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
                        LocationMainResult.AutoLocation(
                            LocationMainResult.LocationMainAutoResult.UpdateEnd(
                                null
                            )
                        )
                    )
                    return@launch
                }
                val result = if (flagUpdate) {
                    LocationMainResult.AutoLocation(
                        LocationMainResult.LocationMainAutoResult.UpdateEnd(
                            location,
                            zoneId,
                            address
                        )
                    )
                } else {
                    LocationMainResult.AutoLocation(
                        LocationMainResult.LocationMainAutoResult.Success(
                            location,
                            zoneId,
                            address
                        )
                    )
                }
                scope.emit(result)

            } else {
                scope.emit(
                    LocationMainResult.AutoLocation(
                        LocationMainResult.LocationMainAutoResult.None(
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

    override fun dispatchAction(action: LocationMainAction): LiveData<LocationMainResult> {
        return liveData {
            when (action) {
                is LocationMainAction.UpdateAutoLocation -> {
                    val activity = action.refActivity.get() ?: return@liveData
                    emit(LocationMainResult.AutoLocation(LocationMainResult.LocationMainAutoResult.UpdateBegin))
                    getLocation(activity.applicationContext, true, this)
                }
                LocationMainAction.CancelUpdateAutoLocation -> {
                    updateJobsCancel()
                    emit(
                        LocationMainResult.AutoLocation(
                            LocationMainResult.LocationMainAutoResult.UpdateEnd(
                                null
                            )
                        )
                    )
                }
                is LocationMainAction.GetLocation -> {
                    val activity = action.refActivity.get() ?: return@liveData
                    getLocation(activity.applicationContext, false, this)
                }
                is LocationMainAction.CheckPermission -> {
                    val activity = action.refActivity.get() ?: return@liveData
                    val isAllow = LocationPermissionHelper.check(activity)
                    if (!isAllow) {
                        emit(
                            LocationMainResult
                                .PermissionDenied((LocationPermissionHelper.isRationale(activity)))
                        )
                        return@liveData
                    }
                    emit(LocationMainResult.PermissionGranted)
                }

                is LocationMainAction.SelectFormAuto -> {
                    emit(LocationMainResult.Select(action.data, true))
                }
                is LocationMainAction.SelectFromList -> {
                    emit(LocationMainResult.Select(action.data, false))
                }
                is LocationMainAction.Delete -> {
                    action.item.id?.let {
                        dao.deleteById(it)
                        if (internalViewState.selectedItem?.data?.id == it) {
                            emit(LocationMainResult.Deselect)
                        }
                    }
                }
                is LocationMainAction.Edit -> {
                    editRepository.edit(action.item)
                }
                LocationMainAction.Add -> {
                    withContext(Dispatchers.Main) {
                        editRepository.create()
                    }
                }
            }
        }
    }

    override fun reduce(result: LocationMainResult): LocationMainState {
        return when (result) {
            is LocationMainResult.Select -> internalViewState.copy(
                selectedItem = LocationMainState.SelectedItem(result.item, result.isAuto)
            )
            is LocationMainResult.Deselect -> internalViewState.copy(
                selectedItem = null
            )
            is LocationMainResult.PermissionGranted -> internalViewState.copy(
                autoLocationPermission = LocationMainState.LocationPermission.Allow
            )
            is LocationMainResult.PermissionDenied -> internalViewState.copy(
                autoLocationPermission = if (result.shouldRationale)
                    LocationMainState.LocationPermission.DeniedRationale
                else
                    LocationMainState.LocationPermission.Denied
            )
            is LocationMainResult.AutoLocation -> {
                when (result.locationResult) {
                    is LocationMainResult.LocationMainAutoResult.None -> internalViewState.copy(
                        autoLocationPermission = if (result.locationResult.isPermissionAllow)
                            LocationMainState.LocationPermission.Allow else
                            LocationMainState.LocationPermission.Denied,
                        autoLocationTimeZoneData = null
                    )
                    is LocationMainResult.LocationMainAutoResult.UpdateBegin -> internalViewState.copy(
                        isAutoLocationUpdate = true
                    )
                    is LocationMainResult.LocationMainAutoResult.UpdateEnd -> internalViewState.copy(
                        isAutoLocationUpdate = false,
                        autoLocationTimeZoneData = result.locationResult.location
                            ?: internalViewState.autoLocationTimeZoneData
                    )
                    is LocationMainResult.LocationMainAutoResult.Success -> internalViewState.copy(
                        autoLocationPermission = LocationMainState.LocationPermission.Allow,
                        autoLocationTimeZoneData = result.locationResult.location
                    )
                }
            }
        }
    }

}