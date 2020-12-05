package net.arwix.location.edit.zone.ui

import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.arwix.location.data.gtimezone.TimeZoneGoogleRepository
import net.arwix.location.data.gtimezone.TimeZoneRepository
import net.arwix.location.edit.data.EditZoneData
import net.arwix.location.edit.domain.LocationCreateEditUseCase
import net.arwix.mvi.FlowViewModel

class LocationZoneViewModel(
    private val editUseCase: LocationCreateEditUseCase,
    private val zoneRepository: TimeZoneRepository,
    private val googleZoneRepository: TimeZoneGoogleRepository
) : FlowViewModel<LocationZoneAction, LocationZoneResult, LocationZoneState>(LocationZoneState()) {

    private var previousLatLng: LatLng? = null
    @Volatile
    private var updateAutoLocationJob: Job? = null

    init {
        editUseCase.initEditingFlow.onEach {
            if (it == null) {
                nextResult(LocationZoneResult.InitData(null))
            } else {
                // edit
                val latLng = editUseCase.editLocationFlow.value?.latLng
                val data = EditZoneData.createFromLTZData(it).let { ezd ->
                    if (latLng != null) ezd.copy(latLng = latLng) else ezd
                }
                nextResult(LocationZoneResult.InitData(data))
                updateAutoZone(data.latLng)
            }

        }.launchIn(viewModelScope)

        editUseCase.editLocationFlow.onEach {
            if (it != null && it.latLng != previousLatLng) updateAutoZone(it.latLng)
            previousLatLng = it?.latLng
        }.launchIn(viewModelScope)
        nextMergeAction(LocationZoneAction.LoadZoneList)
    }

    override suspend fun dispatchAction(action: LocationZoneAction): Flow<LocationZoneResult> =
        flow {
            when (action) {
                is LocationZoneAction.LoadZoneList -> {
                    val list = withContext(Dispatchers.IO) {
                        zoneRepository.getZonesList()
                    }
                    emit(LocationZoneResult.Zones(list))
                }
//                is LocationZoneAction.UpdateAutoZone -> {
//                    autoZone(action.latLng)
//                }
                is LocationZoneAction.SelectZoneFormAuto -> {
                    val currentData = state.value.selectedData ?: return@flow
                    emit(
                        LocationZoneResult.SelectZone(
                            EditZoneData(action.zone, true, currentData.latLng)
                        )
                    )
                }
                is LocationZoneAction.SelectZoneFromList -> {
                    val currentData = state.value.selectedData ?: return@flow
                    emit(
                        LocationZoneResult.SelectZone(
                            EditZoneData(action.zone, false, currentData.latLng)
                        )
                    )
                }
            }
        }

    private fun updateAutoZone(inLatLng: LatLng) {
        updateAutoLocationJob?.cancel()
        updateAutoLocationJob = viewModelScope.launch {
            val intState = state.value
            if (intState.autoZoneStatus is LocationZoneState.AutoZoneStatus.Ok &&
                intState.autoZoneStatus.latLng == inLatLng
            ) return@launch
            nextResult(LocationZoneResult.AutoZoneLoading(inLatLng))
            runCatching {
                googleZoneRepository.getZoneId(inLatLng)
            }.onSuccess {
                nextResult(LocationZoneResult.AutoZoneOk(inLatLng, it))
            }.onFailure {
                nextResult(LocationZoneResult.AutoZoneError(inLatLng, it))
            }
        }
    }

    override suspend fun reduce(
        state: LocationZoneState,
        result: LocationZoneResult
    ): LocationZoneState {
        return when (result) {
            is LocationZoneResult.InitData -> {
                state.copy(
                    selectedData = result.data,
                    finishStepAvailable = result.data != null
                )
            }
            is LocationZoneResult.Zones -> {
                state.copy(listZones = result.list)
            }
            is LocationZoneResult.AutoZoneLoading -> {
                val isAutoZone = state.selectedData?.isAutoZone ?: false
                state.copy(
                    autoZoneStatus = LocationZoneState.AutoZoneStatus.Loading(result.latLng),
                    finishStepAvailable = if (isAutoZone) false else state.finishStepAvailable
                )
            }
            is LocationZoneResult.AutoZoneOk -> {
                val selectedData =
                    state.selectedData ?: EditZoneData(result.data, true, result.latLng)
                state.copy(
                    selectedData = selectedData,
                    autoZoneStatus = LocationZoneState.AutoZoneStatus.Ok(
                        result.latLng,
                        result.data
                    ),
                    finishStepAvailable = if (selectedData.isAutoZone) true else state.finishStepAvailable
                )
            }
            is LocationZoneResult.AutoZoneError -> {
                state.copy(
                    autoZoneStatus = LocationZoneState.AutoZoneStatus.Error(
                        result.latLng,
                        result.error
                    )
                )
            }
            is LocationZoneResult.SelectZone -> {
                state.copy(
                    selectedData = result.data,
                    finishStepAvailable = true
                )
            }
//            is LocationZoneResult.ClearZone -> {
//                state.copy(
//                    autoZoneStatus = null,
//                    selectedData = null,
//                    finishStepAvailable = false
//                )
//            }
        }.also {
            editUseCase.updateZone(it.selectedData)
        }
    }

    suspend fun submit() {
        super.onCleared()
        editUseCase.submit()
    }
}