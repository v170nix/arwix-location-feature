package net.arwix.location.edit.position.ui

import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import net.arwix.location.data.GeocoderRepository
import net.arwix.location.edit.data.EditLocationData
import net.arwix.location.edit.domain.LocationCreateEditUseCase
import net.arwix.mvi.FlowViewModel

class LocationPositionViewModel(
    private val editUseCase: LocationCreateEditUseCase,
    private val geocoderRepository: GeocoderRepository
) : FlowViewModel<LocationPositionAction, LocationPositionResult, LocationPositionState>(
    LocationPositionState()
) {

    @Volatile
    private var geocodeJob: Job? = null

    init {
        editUseCase.initEditingFlow.onEach {
            if (it == null) onResult(LocationPositionResult.InitData(null))
            else onResult(
                LocationPositionResult.InitData(
                    EditLocationData.createFromLTZData(
                        it
                    )
                )
            )
        }.launchIn(viewModelScope)
    }

    override suspend fun dispatchAction(
        action: LocationPositionAction
    ): Flow<LocationPositionResult> = flow {
        when (action) {
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
            val address = geocoderRepository.getAddressOrNull(latLng.latitude, latLng.longitude)
            yield()
            if (address != null) {
                onResult(
                    LocationPositionResult.SuccessGeocoder(
                        address,
                        latLng,
                        cameraPosition
                    )
                )
            } else onResult(LocationPositionResult.ErrorGeocoder(latLng, cameraPosition))

        }
    }

    override suspend fun reduce(
        state: LocationPositionState,
        result: LocationPositionResult
    ): LocationPositionState {
        return when (result) {
            is LocationPositionResult.InitData -> {
                state.copy(
                    updateMapAfterChangeLocation = true,
                    data = result.data,
                    nextStepIsAvailable = result.data?.let { true } ?: false,
                    error = null
                )
            }
            is LocationPositionResult.SuccessPlace -> {
                state.copy(
                    updateMapAfterChangeLocation = true,
                    data = result.data,
                    nextStepIsAvailable = true,
                    error = null
                )
            }
            is LocationPositionResult.ProgressGeocoderFromMap -> {
                state.copy(
                    updateMapAfterChangeLocation = false,
                    data = result.data,
                    nextStepIsAvailable = true,
                    error = null
                )
            }

            is LocationPositionResult.ProgressGeocoderFromInput -> {
                state.copy(
                    updateMapAfterChangeLocation = true,
                    data = result.data,
                    nextStepIsAvailable = true,
                    error = null
                )
            }

            is LocationPositionResult.SuccessGeocoder -> {
                state.copy(
                    updateMapAfterChangeLocation = false,
                    data = result.data,
                    nextStepIsAvailable = true,
                    error = null
                )
            }

            is LocationPositionResult.ErrorGeocoder -> {
                state.copy(
                    updateMapAfterChangeLocation = false,
                    data = result.data,
                    nextStepIsAvailable = true,
                    error = LocationPositionState.ErrorState.Geocoder
                )
            }

            is LocationPositionResult.ErrorInput -> {
                state.copy(
                    updateMapAfterChangeLocation = false,
                    data = null,
                    nextStepIsAvailable = false,
                    error = LocationPositionState.ErrorState.Input(
                        result.latitude,
                        result.longitude
                    )
                )
            }

            is LocationPositionResult.ErrorPlaceLatLng -> {
                state.copy(
                    updateMapAfterChangeLocation = false,
                    error = LocationPositionState.ErrorState.PlaceLatLng(
                        result.place
                    )
                )
            }
        }.also {
            editUseCase.updateLocation(it.data)
        }
    }
}