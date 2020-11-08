package net.arwix.location.edit.domain

import android.util.Log
import androidx.annotation.UiThread
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import net.arwix.location.data.room.LocationDao
import net.arwix.location.data.room.LocationTimeZoneData
import net.arwix.location.edit.data.EditLocationData
import net.arwix.location.edit.data.EditZoneData

class LocationCreateEditUseCase(
    val dao: LocationDao
) {

    private val _initEditingFlow = MutableSharedFlow<LocationTimeZoneData?>(replay = 1)
    val initEditingFlow = _initEditingFlow.asSharedFlow()

    private val _editLocationFlow = MutableStateFlow<EditLocationData?>(null)
    val editLocationFlow = _editLocationFlow.asStateFlow()

    private val _editZoneFlow = MutableStateFlow<EditZoneData?>(null)
    val editZoneFlow = _editZoneFlow.asStateFlow()

//    private val _isEditData = MutableLiveData<LocationTimeZoneData>()
//    val isEditData: LiveData<LocationTimeZoneData> = _isEditData

//    val timeZoneData = MutableLiveData<ZoneId>()

    @UiThread
    suspend fun create() {
        _initEditingFlow.emit(null)
        _editLocationFlow.value = null
        _editZoneFlow.value = null
//        timeZoneData.value = null
//        _isEditData.value = null
    }

    suspend fun edit(data: LocationTimeZoneData) {
//        timeZoneData.postValue(data.zone)
        _editLocationFlow.value = EditLocationData.createFromLTZData(data)
        _editZoneFlow.value = EditZoneData.createFromLTZData(data)
        _initEditingFlow.emit(data)
    }

    fun updateLocation(editLocationData: EditLocationData?) {
        Log.e("updateLocation", editLocationData.toString())
        _editLocationFlow.value = editLocationData
        val editZoneData = _editZoneFlow.value ?: return
        if (editZoneData.latLng != editLocationData?.latLng) {
            editLocationData?.latLng?.run {
                _editZoneFlow.value = editZoneData.copy(latLng = this)
            }
        }
    }

    fun updateZone(editZoneData: EditZoneData?) {
        _editZoneFlow.value = editZoneData
    }

    suspend fun submit() {
        val location = editLocationFlow.value ?: return
        val timeZone = editZoneFlow.value ?: return
        val data = LocationTimeZoneData(
            id = null,
            name = location.name,
            subName = location.subName,
            latLng = location.latLng,
            zone = timeZone.zone,
            isAutoZone = timeZone.isAutoZone,
            zoom = location.cameraPosition?.zoom,
            bearing = location.cameraPosition?.bearing,
            tilt = location.cameraPosition?.tilt
        )
        initEditingFlow.replayCache.lastOrNull()?.let { editData ->
            dao.update(data.copy(id = editData.id))
        } ?: dao.insert(data)
    }

}