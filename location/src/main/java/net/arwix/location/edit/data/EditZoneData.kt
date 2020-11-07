package net.arwix.location.edit.data

import com.google.android.gms.maps.model.LatLng
import net.arwix.location.data.room.LocationTimeZoneData
import org.threeten.bp.ZoneId

data class EditZoneData(
    val zone: ZoneId,
    val isAutoZone: Boolean = false,
    val latLng: LatLng,
) {
    companion object {

        @JvmStatic
        fun createFromLTZData(data: LocationTimeZoneData): EditZoneData {
            return EditZoneData(data.zone, data.isAutoZone, data.latLng)
        }
    }
}