package net.arwix.location.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.CameraPosition
import net.arwix.location.data.room.LocationDao
import net.arwix.location.data.room.LocationTimeZoneData
import org.threeten.bp.ZoneId

class LocationCreateEditRepository(private val dao: LocationDao) {

    val locationData = MutableLiveData<FeatureLocationData>()
    val timeZoneData = MutableLiveData<ZoneId>()
    private val _isNewData = MutableLiveData<Boolean>()
    private val _isEditData = MutableLiveData<LocationTimeZoneData>()
    val isNewData: LiveData<Boolean> = _isNewData
    val isEditData: LiveData<LocationTimeZoneData> = _isEditData

    fun create() {
        locationData.value = null
        timeZoneData.value = null
        _isEditData.value = null
        _isNewData.value = true
    }

    fun edit(data: LocationTimeZoneData) {
        timeZoneData.value = data.zone
        locationData.value = FeatureLocationData(data.name,
            data.subName,
            data.latLng,
            CameraPosition.builder()
                .target(data.latLng)
                .apply { data.zoom?.run(::zoom) }
                .apply { data.tilt?.run(::tilt) }
                .apply { data.bearing?.run(::bearing) }
                .build()
        )
        _isNewData.value = false
        _isEditData.value = data
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