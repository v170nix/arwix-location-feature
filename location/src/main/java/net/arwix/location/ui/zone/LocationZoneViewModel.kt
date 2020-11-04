package net.arwix.location.ui.zone

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataScope
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import net.arwix.location.data.TimeZoneGoogleRepository
import net.arwix.location.data.TimeZoneRepository
import net.arwix.location.edit.domain.LocationCreateEditUseCase
import net.arwix.mvi.SimpleIntentViewModel

class LocationZoneViewModel(
    private val useCase: LocationCreateEditUseCase,
    private val zoneRepository: TimeZoneRepository,
    private val googleZoneRepository: TimeZoneGoogleRepository
) : SimpleIntentViewModel<LocationZoneAction, LocationZoneResult, LocationZoneState>() {

    override var internalViewState = LocationZoneState()
    private var previousLatLng: LatLng? = null

    init {
        useCase.editLocationFlow.onEach {
            if (it == null || previousLatLng != it.latLng) notificationFromObserver(
                LocationZoneResult.ClearZone
            )
            previousLatLng = it?.latLng
        }.launchIn(viewModelScope)
        nonCancelableIntent(LocationZoneAction.LoadZoneList)
    }

    override fun dispatchAction(action: LocationZoneAction): LiveData<LocationZoneResult> =
        liveData {
            when (action) {
                is LocationZoneAction.Init -> {
                    val latLng = useCase.editLocationFlow.value?.latLng ?: return@liveData
                    autoZone(latLng)
                }
                is LocationZoneAction.LoadZoneList -> {
                    val list = withContext(Dispatchers.IO) {
                        zoneRepository.getZonesList()
                    }
                    emit(LocationZoneResult.Zones(list))
                }
                is LocationZoneAction.UpdateAutoZone -> {
                    autoZone(action.latLng)
                }
                is LocationZoneAction.SelectZoneFormAuto -> {
                    emit(
                        LocationZoneResult.SelectZone(
                            LocationZoneState.SelectZoneId(action.zone, true)
                        )
                    )
                }
                is LocationZoneAction.SelectZoneFromList -> {
                    emit(
                        LocationZoneResult.SelectZone(
                            LocationZoneState.SelectZoneId(action.zone, false)
                        )
                    )
                }
            }
        }

    private suspend fun LiveDataScope<LocationZoneResult>.autoZone(inLatLng: LatLng): Boolean {
        val intState = internalViewState
        if (intState.autoZone is LocationZoneState.AutoZone.Ok &&
            intState.autoZone.latLng == inLatLng
        ) return true
        emit(LocationZoneResult.AutoZoneLoading(inLatLng))
        runCatching {
            googleZoneRepository.getZoneId(inLatLng)
        }.onSuccess {
            emit(LocationZoneResult.AutoZoneOk(inLatLng, it))
        }.onFailure {
            emit(LocationZoneResult.AutoZoneError(inLatLng, it))
        }
        return false
    }

    override fun reduce(result: LocationZoneResult): LocationZoneState {
        val state = when (result) {
            is LocationZoneResult.Zones -> {
                internalViewState.copy(
                    listZones = result.list
                )
            }
            is LocationZoneResult.AutoZoneLoading -> {
                val isAutoZone = internalViewState.selectZoneId?.fromAuto ?: false
                internalViewState.copy(
                    autoZone = LocationZoneState.AutoZone.Loading(result.latLng),
                    selectZoneId = if (isAutoZone) null else internalViewState.selectZoneId,
                    finishStepAvailable = if (isAutoZone) false else internalViewState.finishStepAvailable
                )
            }
            is LocationZoneResult.AutoZoneOk -> {
                internalViewState.copy(
                    autoZone = LocationZoneState.AutoZone.Ok(result.latLng, result.data)
                )
            }
            is LocationZoneResult.AutoZoneError -> {
                internalViewState.copy(
                    autoZone = LocationZoneState.AutoZone.Error(result.latLng, result.error)
                )
            }
            is LocationZoneResult.SelectZone -> {
                internalViewState.copy(
                    selectZoneId = result.selectZoneId,
                    finishStepAvailable = true
                )
            }
            is LocationZoneResult.ClearZone -> {
                internalViewState.copy(
                    autoZone = null,
                    selectZoneId = null,
                    finishStepAvailable = false
                )
            }
        }
        useCase.timeZoneData.value = state.selectZoneId?.zoneId
        return state
    }

    suspend fun submit() {
        super.onCleared()
        useCase.submit()
    }
}