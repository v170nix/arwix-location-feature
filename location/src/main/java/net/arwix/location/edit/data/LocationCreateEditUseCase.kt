package net.arwix.location.edit.data

import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.CameraPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.arwix.location.data.EditLocationData
import net.arwix.location.data.room.LocationDao
import net.arwix.location.data.room.LocationTimeZoneData
import org.threeten.bp.ZoneId

class LocationCreateEditUseCase(
    val dao: LocationDao
) {

    private val _editLocationData = MutableStateFlow<EditLocationData?>(null)
    val locationData = _editLocationData.asStateFlow()
    val timeZoneData = MutableLiveData<ZoneId>()

    private val _isNewData = MutableLiveData<Boolean>()
    val isNewData: LiveData<Boolean> = _isNewData

    private val _isEditData = MutableLiveData<LocationTimeZoneData>()
    val isEditData: LiveData<LocationTimeZoneData> = _isEditData

    @UiThread
    fun create() {
        _editLocationData.value = null
        timeZoneData.value = null
        _isEditData.value = null
        _isNewData.value = true
    }

    fun edit(data: LocationTimeZoneData) {
        timeZoneData.postValue(data.zone)
        _editLocationData.value = EditLocationData(data.name,
            data.subName,
            data.latLng,
            CameraPosition.builder()
                .target(data.latLng)
                .apply { data.zoom?.run(::zoom) }
                .apply { data.tilt?.run(::tilt) }
                .apply { data.bearing?.run(::bearing) }
                .build()
        )
        _isNewData.postValue(false)
        _isEditData.postValue(data)
    }

    fun updateLocation(editLocationData: EditLocationData?) {
        _editLocationData.value = editLocationData
    }

    suspend fun submit() {
        val location = locationData.value ?: return
        val timeZone = timeZoneData.value ?: return
        val data = LocationTimeZoneData(
            id = null,
            name = location.name,
            subName = location.subName,
            latLng = location.latLng,
            zone = timeZone,
            zoom = location.cameraPosition?.zoom,
            bearing = location.cameraPosition?.bearing,
            tilt = location.cameraPosition?.tilt
        )
        isEditData.value?.let { editData ->
            dao.update(data.copy(id = editData.id))
        } ?: dao.insert(data)
    }

}