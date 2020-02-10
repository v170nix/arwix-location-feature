package net.arwix.location.ui.list

import android.content.Context
import androidx.lifecycle.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectIndexed
import net.arwix.location.LocationZoneIdSelectedDatabase
import net.arwix.location.data.GeocoderRepository
import net.arwix.location.data.LocationCreateEditRepository
import net.arwix.location.data.LocationZoneId
import net.arwix.location.data.room.LocationDao
import net.arwix.location.data.room.LocationTimeZoneData
import net.arwix.location.domain.LocationHelper
import net.arwix.location.domain.LocationPermissionHelper
import net.arwix.mvi.SimpleIntentViewModel
import org.threeten.bp.ZoneId

class LocationListViewModel(
    private val applicationContext: Context,
    private val selectedDatabase: LocationZoneIdSelectedDatabase,
    private val dao: LocationDao,
    private val editRepository: LocationCreateEditRepository,
    private val geocoderRepository: GeocoderRepository
) : SimpleIntentViewModel<LocationListAction, LocationListResult, LocationListState>() {

    override var internalViewState: LocationListState = LocationListState()
    private val updateLocationJobs = mutableListOf<Job>()

    init {
        viewModelScope.launch {
            dao.getAll().asFlow().collectIndexed { index, list: List<LocationTimeZoneData> ->
                if (index == 0) {
                    initState(list)
                } else {
                    notificationFromObserver(LocationListResult.ManualList(list))
                }
            }
        }
    }

    private suspend fun initState(customList: List<LocationTimeZoneData>) {
        val permission = LocationPermissionHelper.check(applicationContext)
        val selectedItem: LocationZoneId? = selectedDatabase.getLZData()
        val selected: LocationListResult.Select? = selectedItem?.run {
            val innerData = toLocationTimeZoneData()
            LocationListResult.Select(innerData, this is LocationZoneId.Auto)
        }
        val location = LocationHelper.getLocation(applicationContext, false)
        val autoItem: LocationListResult.AutoItem = run {
            if (location != null) {
                LocationListResult.AutoItem.Success(
                    location,
                    ZoneId.systemDefault()
                )
            } else {
                LocationListResult.AutoItem.None(permission)
            }
        }
        notificationFromObserver(
            LocationListResult.Init(
                permission,
                customList,
                autoItem,
                selected
            )
        )

        location ?: return
        val job = viewModelScope.launch(Dispatchers.IO, CoroutineStart.LAZY) {
            val address = geocoderRepository.getAddressOrNull(location.latitude, location.longitude)
                ?: return@launch
            yield()
            withContext(Dispatchers.Main) {
                notificationFromObserver(
                    LocationListResult.AutoLocation(
                        LocationListResult.AutoItem.Success(
                            location,
                            ZoneId.systemDefault(),
                            address
                        )
                    )
                )
            }
        }
        updateLocationJobsCancel(job)
        job.start()
    }

    private suspend fun getLocation(
        applicationContext: Context,
        flagUpdate: Boolean = false,
        scope: LiveDataScope<LocationListResult>
    ) {
        val job = viewModelScope.launch(Dispatchers.Main, CoroutineStart.LAZY) {
            val location = LocationHelper.getLocation(applicationContext, flagUpdate)
            if (location != null) {
                val zoneId = ZoneId.systemDefault()
                LocationListResult.AutoLocation(
                    LocationListResult.AutoItem.Success(
                        location,
                        zoneId
                    )
                ).emitTo(scope)

                val address = withContext(Dispatchers.IO) {
                    geocoderRepository.getAddressOrNull(location.latitude, location.longitude)
                } ?: run {
                    if (flagUpdate) LocationListResult.AutoLocation(
                        LocationListResult.AutoItem.UpdateEnd(
                            null
                        )
                    ).emitTo(scope)
                    return@launch
                }
                LocationListResult.AutoLocation(
                    if (flagUpdate) LocationListResult.AutoItem.UpdateEnd(location, zoneId, address)
                    else LocationListResult.AutoItem.Success(location, zoneId, address)
                ).emitTo(scope)

            } else {
                LocationListResult.AutoLocation(
                    LocationListResult.AutoItem.None(
                        LocationPermissionHelper.check(applicationContext)
                    )
                ).emitTo(scope)
            }
        }
        updateLocationJobsCancel(job)
        job.start()
        job.join()
    }

    @Synchronized
    private fun updateLocationJobsCancel(newJob: Job? = null) {
        synchronized(this) {
            updateLocationJobs.forEach { it.cancel() }
            updateLocationJobs.clear()
            newJob?.run(updateLocationJobs::add)
        }
    }

    suspend fun commitSelectedItem(): Boolean {
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
                if (selectedId == null) return false
                val item = dao.getItem(selectedId) ?: return false
                LocationZoneId.Manual(
                    id = selectedId,
                    name = item.name,
                    subName = item.subName,
                    latitude = item.latLng.latitude,
                    longitude = item.latLng.longitude,
                    altitude = 0.0,
                    zoneId = item.zone
                )
            }
            return selectedDatabase.setLZData(data)
        }
        return false
    }

    override fun dispatchAction(action: LocationListAction): LiveData<LocationListResult> {
        return liveData {
            when (action) {
                LocationListAction.UpdateAutoLocation -> {
                    LocationListResult.AutoLocation(LocationListResult.AutoItem.UpdateBegin)
                        .emitTo(this)
                    getLocation(applicationContext, true, this)
                }
                LocationListAction.CancelUpdateAutoLocation -> {
                    updateLocationJobsCancel()
                    LocationListResult.AutoLocation(
                        LocationListResult.AutoItem.UpdateEnd(null)
                    ).emitTo(this)
                }
                LocationListAction.GetAutoLocation ->
                    getLocation(applicationContext, false, this)

                is LocationListAction.CheckPermission -> {
                    val isAllow = LocationPermissionHelper.check(applicationContext)
                    if (!isAllow) {
                        val activity = action.refActivity?.get()
                        LocationListResult.PermissionDenied(
                            activity?.run(LocationPermissionHelper::isRationale) ?: true
                        ).emitTo(this)
                        return@liveData
                    }
                    emit(LocationListResult.PermissionGranted)
                }
                is LocationListAction.SelectFormAuto ->
                    LocationListResult.Select(action.data, true).emitTo(this)
                is LocationListAction.SelectFromCustomList ->
                    LocationListResult.Select(action.data, false).emitTo(this)
                is LocationListAction.DeleteItem -> {
                    action.item.id?.let {
                        dao.deleteById(it)
                        if (internalViewState.selectedItem?.data?.id == it) {
                            emit(LocationListResult.Deselect)
                        }
                    }
                }
                is LocationListAction.EditItem -> editRepository.edit(action.item)
                LocationListAction.AddItem -> editRepository.create()

            }
        }
    }

    override fun reduce(result: LocationListResult): LocationListState {
        return when (result) {
            is LocationListResult.Init -> {
                val selectedItem = result.selectedItem?.run {
                    LocationListState.SelectedItem(item, isAuto)
                }
                reduceAutoLocation(result.autoItem, internalViewState).copy(
                    customList = result.list,
                    selectedItem = selectedItem
                )
            }
            is LocationListResult.ManualList -> {
                internalViewState.copy(customList = result.list)
            }
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
                reduceAutoLocation(result.autoItem, internalViewState)
            }
        }
    }

    private companion object {

        private fun reduceAutoLocation(
            autoItem: LocationListResult.AutoItem,
            internalViewState: LocationListState
        ): LocationListState {
            return when (autoItem) {
                is LocationListResult.AutoItem.None -> internalViewState.copy(
                    autoLocationPermission = if (autoItem.isPermissionAllow)
                        LocationListState.LocationPermission.Allow else
                        LocationListState.LocationPermission.Denied,
                    autoLocationTimeZoneData = null
                )
                is LocationListResult.AutoItem.UpdateBegin -> internalViewState.copy(
                    isAutoLocationUpdate = true
                )
                is LocationListResult.AutoItem.UpdateEnd -> internalViewState.copy(
                    isAutoLocationUpdate = false,
                    autoLocationTimeZoneData = autoItem.location
                        ?: internalViewState.autoLocationTimeZoneData
                )
                is LocationListResult.AutoItem.Success -> internalViewState.copy(
                    autoLocationPermission = LocationListState.LocationPermission.Allow,
                    autoLocationTimeZoneData = autoItem.location
                )
            }
        }

        private fun LocationZoneId.toLocationTimeZoneData() = LocationTimeZoneData(
            if (this is LocationZoneId.Manual) id else null,
            name,
            subName,
            LatLng(latitude, longitude),
            zoneId
        )
    }

}