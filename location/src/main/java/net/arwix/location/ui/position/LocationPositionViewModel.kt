package net.arwix.location.ui.position

import androidx.lifecycle.LiveDataScope
import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.arwix.location.data.LocationCreateEditRepository
import net.arwix.location.domain.LocationGeocoderUseCase
import net.arwix.mvi.SimpleIntentViewModel

class LocationPositionViewModel(
    private val repository: LocationCreateEditRepository,
    private val locationGeocoderUseCase: LocationGeocoderUseCase
) :
    SimpleIntentViewModel<LocationPositionAction, LocationPositionResult, LocationPositionState>() {

    override var internalViewState =
        LocationPositionState(
            data = repository.locationData.value,
            nextStepAvailable = repository.locationData.value?.let { true } ?: false
        )


    init {
        viewModelScope.launch {
            repository.isNewData.asFlow().collect {
                notificationFromObserver(LocationPositionResult.InitData(null))
            }
        }
        viewModelScope.launch {
            repository.isEditData.asFlow().collect {
                repository.locationData.value?.run {
                    notificationFromObserver(LocationPositionResult.InitData(this))
                }
            }
        }
    }

    override fun dispatchAction(action: LocationPositionAction) =
        liveData<LocationPositionResult> {
            action.dispatch(this)
        }

    private suspend fun LocationPositionAction.dispatch(
        scope: LiveDataScope<LocationPositionResult>
    ) = when (this) {
        is LocationPositionAction.Init -> {
            scope.emit(LocationPositionResult.InitData(internalViewState.data))
        }
        is LocationPositionAction.ChangeFromPlace -> {
            if (place.latLng == null)
                scope.emit(LocationPositionResult.ErrorPlaceLatLng(place))
            else
                scope.emit(LocationPositionResult.SuccessPlace(place, cameraPosition))
        }
        is LocationPositionAction.ChangeFromMap -> {
            scope.emit(LocationPositionResult.ProgressGeocoderFromMap(latLng, cameraPosition))
            locationGeocoderUseCase.geocode(scope, latLng, cameraPosition)
        }
        is LocationPositionAction.ChangeFromInput -> {
            if (latitude == null || latitude < -90.0 || latitude > 90.0 ||
                longitude == null || longitude < -180.0 || longitude > 180.0
            ) {
                scope.emit(LocationPositionResult.ErrorInput(latitude, longitude))
            } else {
                val latLng = LatLng(latitude, longitude)
                scope.emit(LocationPositionResult.ProgressGeocoderFromInput(latLng, cameraPosition))
                locationGeocoderUseCase.geocode(scope, latLng, cameraPosition)
            }
        }
    }

    override fun reduce(result: LocationPositionResult): LocationPositionState {
        val state = when (result) {
            is LocationPositionResult.InitData -> {
                internalViewState.copy(
                    updateMapAfterChangeLocation = true,
                    data = result.data,
                    nextStepAvailable = result.data?.let { true } ?: false,
                    error = null
                )
            }
            is LocationPositionResult.SuccessPlace -> {
                internalViewState.copy(
                    updateMapAfterChangeLocation = true,
                    data = result.data,
                    nextStepAvailable = true,
                    error = null
                )
            }
            is LocationPositionResult.ProgressGeocoderFromMap -> {
                internalViewState.copy(
                    updateMapAfterChangeLocation = false,
                    data = result.data,
                    nextStepAvailable = true,
                    error = null
                )
            }

            is LocationPositionResult.ProgressGeocoderFromInput -> {
                internalViewState.copy(
                    updateMapAfterChangeLocation = true,
                    data = result.data,
                    nextStepAvailable = true,
                    error = null
                )
            }

            is LocationPositionResult.SuccessGeocoder -> {
                internalViewState.copy(
                    updateMapAfterChangeLocation = false,
                    data = result.data,
                    nextStepAvailable = true,
                    error = null
                )
            }

            is LocationPositionResult.ErrorGeocoder -> {
                internalViewState.copy(
                    updateMapAfterChangeLocation = false,
                    data = result.data,
                    nextStepAvailable = true,
                    error = LocationPositionState.ErrorState.Geocoder
                )
            }

            is LocationPositionResult.ErrorInput -> {
                internalViewState.copy(
                    updateMapAfterChangeLocation = false,
                    data = null,
                    nextStepAvailable = false,
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
        repository.locationData.value = state.data
        return state
    }

}