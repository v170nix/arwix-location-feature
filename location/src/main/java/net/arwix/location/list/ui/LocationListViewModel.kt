package net.arwix.location.list.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.UiThread
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flow
import net.arwix.location.data.GeocoderRepository
import net.arwix.location.data.room.LocationDao
import net.arwix.location.data.room.LocationTimeZoneData
import net.arwix.location.domain.LocationHelper
import net.arwix.location.domain.LocationPermissionHelper
import net.arwix.location.edit.domain.LocationCreateEditUseCase
import net.arwix.mvi.FlowViewModel
import java.time.ZoneId

//import org.threeten.bp.ZoneId

class LocationListViewModel(
    context: Context,
    private val dao: LocationDao,
    private val editUseCase: LocationCreateEditUseCase,
    private val geocoderRepository: GeocoderRepository
) : FlowViewModel<LocationListAction, LocationListResult, LocationListState>(LocationListState()) {

    @SuppressLint("StaticFieldLeak")
    private val applicationContext: Context = context.applicationContext
    private val updateLocationJobs = mutableListOf<Job>()

    init {
        viewModelScope.launch {
            dao.getAll()
                .collectIndexed { index, list: List<LocationTimeZoneData> ->
                    if (index == 0) {
                        initState(list)
                    } else {
                        onResult(LocationListResult.LocationList(list))
                    }
                }
        }
    }

    private suspend fun initState(customList: List<LocationTimeZoneData>) {
        val permission = LocationPermissionHelper.check(applicationContext)
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
        onResult(
            LocationListResult.Init(
                permission,
                customList,
                autoItem
            )
        )

        location ?: return
        val job = viewModelScope.launch(Dispatchers.IO, CoroutineStart.LAZY) {
            val address = geocoderRepository.getAddressOrNull(location.latitude, location.longitude)
                ?: return@launch
            yield()
            withContext(Dispatchers.Main) {
                dao.updateAutoItem(address)
                onResult(
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
                dao.updateAutoItem(location, zoneId)
                onResult(
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
                    if (flagUpdate) onResult(
                        LocationListResult.AutoLocation(
                            LocationListResult.AutoItem.UpdateEnd(
                                null
                            )
                        )
                    )
                    return@launch
                }
                dao.updateAutoItem(address)
                onResult(
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
                onResult(
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
                is LocationListAction.SelectFormAuto ->
                    dao.selectAutoItem()

                is LocationListAction.SelectFromCustomList ->
                    dao.selectCustomItem(action.data.copy(isSelected = true))

                is LocationListAction.DeleteItem -> {
                    action.item.id?.let {
                        dao.deleteById(it)
                    }
                }
                is LocationListAction.EditItem -> {
                    withContext(Dispatchers.Main) {
                        editUseCase.edit(action.item)
                    }
                }
                is LocationListAction.UndoDeleteItem -> {
                    dao.insert(action.item)
                }
            }
        }
    }

    @UiThread
    suspend fun addNewLocation() {
        editUseCase.create()
    }

    override suspend fun reduce(
        state: LocationListState,
        result: LocationListResult
    ): LocationListState {
        return when (result) {
            is LocationListResult.Init -> {
                reduceAutoLocation(result.autoItem, state).copy(
                    locationList = result.list,
                    isSelected = checkSelected(result.list, result.autoItem)
                )
            }
            is LocationListResult.LocationList -> {
                state.copy(
                    locationList = result.list,
                    isSelected = checkSelected(
                        result.list,
                        state.autoLocationPermission
                    )
                )
            }
            is LocationListResult.PermissionGranted -> state.copy(
                autoLocationPermission = LocationListState.LocationPermission.Allow,
                isSelected = checkSelected(
                    state.locationList,
                    LocationListState.LocationPermission.Allow
                )
            )
            is LocationListResult.PermissionDenied -> state.copy(
                autoLocationPermission = if (result.shouldRationale)
                    LocationListState.LocationPermission.DeniedRationale
                else
                    LocationListState.LocationPermission.Denied,
                isSelected = checkSelected(
                    state.locationList,
                    LocationListState.LocationPermission.Denied
                )
            )
            is LocationListResult.AutoLocation -> {
                reduceAutoLocation(result.autoItem, state)
            }
        }
    }

    private companion object {

        private fun checkSelected(
            list: List<LocationTimeZoneData>?,
            autoItem: LocationListResult.AutoItem
        ): Boolean {
            list ?: return false
            val selectedItem = list.find { it.isSelected } ?: return false
            if (!selectedItem.isAutoLocation) return true
            return autoItem is LocationListResult.AutoItem.Success
        }

        private fun checkSelected(
            list: List<LocationTimeZoneData>?,
            permission: LocationListState.LocationPermission
        ): Boolean {
            list ?: return false
            val selectedItem = list.find { it.isSelected } ?: return false
            if (!selectedItem.isAutoLocation) return true
            return permission is LocationListState.LocationPermission.Allow
        }

        private fun reduceAutoLocation(
            autoItem: LocationListResult.AutoItem,
            internalViewState: LocationListState
        ): LocationListState {
            return when (autoItem) {
                is LocationListResult.AutoItem.None -> internalViewState.copy(
                    autoLocationPermission = if (autoItem.isPermissionAllow)
                        LocationListState.LocationPermission.Allow else
                        LocationListState.LocationPermission.Denied
                )
                is LocationListResult.AutoItem.UpdateBegin -> internalViewState.copy(
                    isAutoLocationUpdate = true
                )
                is LocationListResult.AutoItem.UpdateEnd -> internalViewState.copy(
                    isAutoLocationUpdate = false
                )
                is LocationListResult.AutoItem.Success -> internalViewState.copy(
                    autoLocationPermission = LocationListState.LocationPermission.Allow
                )
            }.let {
                it.copy(isSelected = checkSelected(it.locationList, it.autoLocationPermission))
            }
        }

    }

}