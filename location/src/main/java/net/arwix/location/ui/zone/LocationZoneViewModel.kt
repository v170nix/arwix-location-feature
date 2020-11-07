package net.arwix.location.ui.zone

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.arwix.location.data.TimeZoneGoogleRepository
import net.arwix.location.data.TimeZoneRepository
import net.arwix.location.edit.data.EditZoneData
import net.arwix.location.edit.domain.LocationCreateEditUseCase
import net.arwix.mvi.FlowViewModel

class LocationZoneViewModel(
    private val editUseCase: LocationCreateEditUseCase,
    private val zoneRepository: TimeZoneRepository,
    private val googleZoneRepository: TimeZoneGoogleRepository
) : FlowViewModel<LocationZoneAction, LocationZoneResult, LocationZoneState>(LocationZoneState()) {

    private var previousLatLng: LatLng? = null

    init {
        editUseCase.initEditingFlow.onEach {
            if (it == null) {
                // add
                nextResult(LocationZoneResult.InitData(null))
//                nextMergeAction(LocationZoneAction.LoadZoneList)
            } else {
                // edit
                val data = EditZoneData.createFromLTZData(it)
                nextResult(LocationZoneResult.InitData(data))
                autoZone(data.latLng)
//                nextMergeAction(LocationZoneAction.LoadZoneList)
            }

        }.launchIn(viewModelScope)

        editUseCase.editLocationFlow.onEach {
//            if (it == null || previousLatLng != it.latLng)
//                nextResult(LocationZoneResult.ClearZone)
            if (it != null && it.latLng != previousLatLng) autoZone(it.latLng)
            previousLatLng = it?.latLng
        }.launchIn(viewModelScope)
//        nextMergeAction(LocationZoneAction.Init)
        nextMergeAction(LocationZoneAction.LoadZoneList)
    }

    override suspend fun dispatchAction(action: LocationZoneAction): Flow<LocationZoneResult> =
        flow {
            Log.e("action", action.toString())
            when (action) {
//                is LocationZoneAction.Init -> {
//                    val latLng = editUseCase.editLocationFlow.value?.latLng ?: return@flow
//                    autoZone(latLng)
//                }
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
                    val currentData = state.value.data ?: return@flow
                    emit(
                        LocationZoneResult.SelectZone(
                            EditZoneData(action.zone, true, currentData.latLng)
                        )
                    )
                }
                is LocationZoneAction.SelectZoneFromList -> {
                    val currentData = state.value.data ?: return@flow
                    emit(
                        LocationZoneResult.SelectZone(
                            EditZoneData(action.zone, false, currentData.latLng)
                        )
                    )
                }
            }
        }

    private fun autoZone(inLatLng: LatLng) {
        viewModelScope.launch {
            val intState = state.value
            if (intState.autoZone is LocationZoneState.AutoZone.Ok &&
                intState.autoZone.latLng == inLatLng
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
        Log.e("result", result.toString())
        return when (result) {
            is LocationZoneResult.InitData -> {
                state.copy(data = result.data)
            }
            is LocationZoneResult.Zones -> {
                state.copy(listZones = result.list)
            }
            is LocationZoneResult.AutoZoneLoading -> {
                val isAutoZone = state.data?.isAutoZone ?: false
                state.copy(
                    autoZone = LocationZoneState.AutoZone.Loading(result.latLng),
                    finishStepAvailable = if (isAutoZone) false else state.finishStepAvailable
                )
            }
            is LocationZoneResult.AutoZoneOk -> {
                state.copy(
                    data = state.data ?: EditZoneData(result.data, true, result.latLng),
                    autoZone = LocationZoneState.AutoZone.Ok(result.latLng, result.data)
                )
            }
            is LocationZoneResult.AutoZoneError -> {
                state.copy(
                    autoZone = LocationZoneState.AutoZone.Error(result.latLng, result.error)
                )
            }
            is LocationZoneResult.SelectZone -> {
                state.copy(
                    data = result.data,
                    finishStepAvailable = true
                )
            }
            is LocationZoneResult.ClearZone -> {
                state.copy(
                    autoZone = null,
                    data = null,
                    finishStepAvailable = false
                )
            }
        }.also {
            editUseCase.timeZoneData.postValue(it.data?.zone)
        }
    }

    suspend fun submit() {
        super.onCleared()
        editUseCase.submit()
    }
}