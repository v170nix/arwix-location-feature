package net.arwix.location.edit.data

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import net.arwix.location.data.room.LocationTimeZoneData

data class EditLocationData(
    val name: String?,
    val subName: String?,
    val latLng: LatLng,
    val cameraPosition: CameraPosition? = null
) {

    companion object {
        @JvmStatic
        fun createFromLTZData(data: LocationTimeZoneData): EditLocationData {
            return EditLocationData(data.name,
                data.subName,
                data.latLng,
                CameraPosition.builder()
                    .target(data.latLng)
                    .apply { data.zoom?.run(::zoom) }
                    .apply { data.tilt?.run(::tilt) }
                    .apply { data.bearing?.run(::bearing) }
                    .build()
            )
        }
    }
}