package net.arwix.location.edit.zone.ui

import com.google.android.gms.maps.model.LatLng
import net.arwix.location.data.TimeZoneDisplayEntry
import net.arwix.location.edit.data.EditZoneData
import org.threeten.bp.ZoneId

data class LocationZoneState(
    val listZones: List<TimeZoneDisplayEntry>? = null,
    val autoZoneStatus: AutoZoneStatus? = null,
    val selectedData: EditZoneData? = null,
//    val selectZoneId: SelectZoneId? = null,
    val finishStepAvailable: Boolean = false
) {
//    data class SelectZoneId(val zoneId: ZoneId, val fromAuto: Boolean)

    sealed class AutoZoneStatus {
        data class Loading(val latLng: LatLng) : AutoZoneStatus()
        data class Ok(val latLng: LatLng, val data: ZoneId) : AutoZoneStatus()
        data class Error(val latLng: LatLng, val error: Throwable) : AutoZoneStatus()
    }
}