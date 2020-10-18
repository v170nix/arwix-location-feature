package net.arwix.location.ui.list

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flow
import net.arwix.location.data.GeocoderRepository
import net.arwix.location.data.LocationZoneId
import net.arwix.location.data.room.LocationDao
import net.arwix.location.data.room.LocationTimeZoneData
import net.arwix.location.domain.LocationHelper
import net.arwix.location.domain.LocationPermissionHelper
import net.arwix.location.edit.data.LocationCreateEditRepository
import net.arwix.mvi.StateViewModel
import org.threeten.bp.ZoneId

class LocationListViewModel(
    private val applicationContext: Context,
//    private val selectedDatabase: LocationZoneIdSelectedDatabase,
    private val dao: LocationDao,
    private val editRepository: LocationCreateEditRepository,
    private val geocoderRepository: GeocoderRepository
) : StateViewModel<LocationListAction, LocationListResult, LocationListState>() {

    override var internalViewState: LocationListState = LocationListState()
    private val updateLocationJobs = mutableListOf<Job>()

    init {
        viewModelScope.launch {
            dao.getAllExceptAuto()
                .collectIndexed { index, list: List<LocationTimeZoneData> ->
                    if (index == 0) {
                        initState(list)
                    } else {
                        nextResult(LocationListResult.ManualList(list))
                    }
                }
        }
    }

    private suspend fun initState(customList: List<LocationTimeZoneData>) {
        val permission = LocationPermissionHelper.check(applicationContext)
        val selected: LocationListResult.Select? = dao.getSelectedItem()?.run {
            LocationListResult.Select(this, this.isAuto)
        }
        val location = LocationHelper.getLocation(applicationContext, false)
        val autoItem: LocationListResult.AutoItem = run {
            if (location != null) {
                dao.updateAutoItem(location, ZoneId.systemDefault())
                LocationListResult.AutoItem.Success(
                    location,
                    ZoneId.systemDefault()
                )
            } else {
                LocationListResult.AutoItem.None(permission)
            }
        }
        nextResult(
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
                dao.updateAutoItem(address)
                nextResult(
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
        flagUpdate: Boolean = false
    ) {
        val job = viewModelScope.launch(Dispatchers.Main, CoroutineStart.LAZY) {
            val location = LocationHelper.getLocation(applicationContext, flagUpdate)
            if (location != null) {
                val zoneId = ZoneId.systemDefault()
                dao.updateAutoItem(location, ZoneId.systemDefault())
                nextResult(
                    LocationListResult.AutoLocation(
                        LocationListResult.AutoItem.Success(
                            location,
                            zoneId
                        )
                    )
                )

                val address = withContext(Dispatchers.IO) {
                    geocoderRepository.getAddressOrNull(location.latitude, location.longitude)
                } ?: run {
                    if (flagUpdate) nextResult(
                        LocationListResult.AutoLocation(
                            LocationListResult.AutoItem.UpdateEnd(
                                null
                            )
                        )
                    )
                    return@launch
                }
                dao.updateAutoItem(address)
                nextResult(
                    LocationListResult.AutoLocation(
                        if (flagUpdate) LocationListResult.AutoItem.UpdateEnd(
                            location,
                            zoneId,
                            address
                        )
                        else LocationListResult.AutoItem.Success(location, zoneId, address)
                    )
                )

            } else {
                nextResult(
                    LocationListResult.AutoLocation(
                        LocationListResult.AutoItem.None(
                            LocationPermissionHelper.check(applicationContext)
                        )
                    )
                )
            }
        }
        updateLocationJobsCancel(job)
        job.start()
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
        internalViewState.selectedItem?.let { selectedItem: LocationListState.SelectedItem ->
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
//            dao.selectItem()
            return true // selectedDatabase.setLZData(data)
        }
        return false
    }

    override suspend fun dispatchAction(action: LocationListAction): Flow<LocationListResult> {
        return flow {
            when (action) {
                LocationListAction.UpdateAutoLocation -> {
                    emit(LocationListResult.AutoLocation(LocationListResult.AutoItem.UpdateBegin))
                    getLocation(applicationContext, true)
                }
                LocationListAction.CancelUpdateAutoLocation -> {
                    updateLocationJobsCancel()
                    emit(
                        LocationListResult.AutoLocation(
                            LocationListResult.AutoItem.UpdateEnd(null)
                        )
                    )
                }
                LocationListAction.GetAutoLocation ->
                    getLocation(applicationContext, false)

                is LocationListAction.CheckPermission -> {
                    val isAllow = LocationPermissionHelper.check(applicationContext)
                    if (!isAllow) {
                        val activity = action.refActivity?.get()
                        emit(
                            LocationListResult.PermissionDenied(
                                activity?.run(LocationPermissionHelper::isRationale) ?: true
                            )
                        )
                        return@flow
                    }
                    emit(LocationListResult.PermissionGranted)
                }
                is LocationListAction.SelectFormAuto -> {
                    action.data.copy(isSelected = true).also {
                        dao.selectAutoItem()
                        emit(LocationListResult.Select(action.data, true))
                    }
//                    LocationListResult.Select(action.data, true).emitTo(this)
                }
                is LocationListAction.SelectFromCustomList -> {
                    action.data.copy(isSelected = true).also {
                        dao.selectCustomItem(it)
                        emit(LocationListResult.Select(it, false))
                    }
//                    LocationListResult.Select(action.data, false).emitTo(this)
                }
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

    override suspend fun reduce(
        state: LocationListState,
        result: LocationListResult
    ): LocationListState {
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
            0.0,
            zoneId
        )
    }

}