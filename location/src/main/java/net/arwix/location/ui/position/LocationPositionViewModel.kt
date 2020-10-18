package net.arwix.location.ui.position

import android.util.Log
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import net.arwix.location.domain.LocationGeocoderUseCase
import net.arwix.location.edit.data.LocationCreateEditRepository
import net.arwix.mvi.StateViewModel

class LocationPositionViewModel(
    private val editRepository: LocationCreateEditRepository,
    private val locationGeocoderUseCase: LocationGeocoderUseCase
) :
    StateViewModel<LocationPositionAction, LocationPositionResult, LocationPositionState>() {

    private var geocodeJob: Job? = null

    override var internalViewState =
        LocationPositionState(
            subData = editRepository.locationData.value,
            nextStepIsAvailable = editRepository.locationData.value?.let { true } ?: false
        )


    init {
        viewModelScope.launch {
            editRepository.isNewData.asFlow().collect {
                if (it) nextResult(LocationPositionResult.InitData(null))
            }
        }
        viewModelScope.launch {
            editRepository.isEditData.asFlow().collect {
                editRepository.locationData.value?.run {
                    nextResult(LocationPositionResult.InitData(this))
                }
            }
        }
    }

    override suspend fun dispatchAction(
        action: LocationPositionAction
    ): Flow<LocationPositionResult> = flow {
        when (action) {
            is LocationPositionAction.Init -> {
                emit(LocationPositionResult.InitData(internalViewState.subData))
            }
            is LocationPositionAction.ChangeFromPlace -> {
                geocodeJob?.cancel()
                if (action.place.latLng == null)
                    emit(LocationPositionResult.ErrorPlaceLatLng(action.place))
                else
                    emit(LocationPositionResult.SuccessPlace(action.place, action.cameraPosition))
            }
            is LocationPositionAction.ChangeFromMap -> {
                emit(
                    LocationPositionResult.ProgressGeocoderFromMap(
                        action.latLng,
                        action.cameraPosition
                    )
                )
                yield()
                requestGeocode(action.latLng, action.cameraPosition)
                Log.e("error!!", "3")
            }
            is LocationPositionAction.ChangeFromInput -> {
                geocodeJob?.cancel()
                if (action.latitude == null || action.latitude < -90.0 || action.latitude > 90.0 ||
                    action.longitude == null || action.longitude < -180.0 || action.longitude > 180.0
                ) {
                    emit(LocationPositionResult.ErrorInput(action.latitude, action.longitude))
                } else {
                    val latLng = LatLng(action.latitude, action.longitude)
                    emit(
                        LocationPositionResult.ProgressGeocoderFromInput(
                            latLng,
                            action.cameraPosition
                        )
                    )
                    requestGeocode(latLng, action.cameraPosition)
                }
            }
        }
    }

    private fun requestGeocode(latLng: LatLng, cameraPosition: CameraPosition?) {
        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch(Dispatchers.IO) {
            locationGeocoderUseCase.geocode(latLng, cameraPosition).collect {
                nextResult(it)
            }
        }
    }

    override suspend fun reduce(
        state: LocationPositionState,
        result: LocationPositionResult
    ): LocationPositionState {
        val state = when (result) {
            is LocationPositionResult.InitData -> {
                internalViewState.copy(
                    updateMapAfterChangeLocation = true,
                    subData = result.subData,
                    nextStepIsAvailable = result.subData?.let { true } ?: false,
                    error = null
                )
            }
            is LocationPositionResult.SuccessPlace -> {
                internalViewState.copy(
                    updateMapAfterChangeLocation = true,
                    subData = result.subData,
                    nextStepIsAvailable = true,
                    error = null
                )
            }
            is LocationPositionResult.ProgressGeocoderFromMap -> {
                internalViewState.copy(
                    updateMapAfterChangeLocation = false,
                    subData = result.subData,
                    nextStepIsAvailable = true,
                    error = null
                )
            }

            is LocationPositionResult.ProgressGeocoderFromInput -> {
                internalViewState.copy(
                    updateMapAfterChangeLocation = true,
                    subData = result.subData,
                    nextStepIsAvailable = true,
                    error = null
                )
            }

            is LocationPositionResult.SuccessGeocoder -> {
                internalViewState.copy(
                    updateMapAfterChangeLocation = false,
                    subData = result.subData,
                    nextStepIsAvailable = true,
                    error = null
                )
            }

            is LocationPositionResult.ErrorGeocoder -> {
                internalViewState.copy(
                    updateMapAfterChangeLocation = false,
                    subData = result.subData,
                    nextStepIsAvailable = true,
                    error = LocationPositionState.ErrorState.Geocoder
                )
            }

            is LocationPositionResult.ErrorInput -> {
                internalViewState.copy(
                    updateMapAfterChangeLocation = false,
                    subData = null,
                    nextStepIsAvailable = false,
                    error = LocationPositionState.ErrorState.Input(
                        result.latitude,
                        result.longitude
                    )
                )
            }

            is LocationPositionResult.ErrorPlaceLatLng -> {
                internalViewState.copy(
                    updateMapAfterChangeLocation = false,
                    error = LocationPositionState.ErrorState.PlaceLatLng(
                        result.place
                    )
                )
            }
        }
        editRepository.locationData.postValue(state.subData)
        return state
    }
}