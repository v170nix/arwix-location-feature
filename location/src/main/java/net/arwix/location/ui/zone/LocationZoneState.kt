package net.arwix.location.ui.zone

import com.google.android.gms.maps.model.LatLng
import net.arwix.location.data.TimeZoneDisplayEntry
import net.arwix.location.edit.data.EditZoneData
import org.threeten.bp.ZoneId

data class LocationZoneState(
    val listZones: List<TimeZoneDisplayEntry>? = null,
    val autoZone: AutoZone? = null,
    val data: EditZoneData? = null,
//    val selectZoneId: SelectZoneId? = null,
    val finishStepAvailable: Boolean = false
) {
//    data class SelectZoneId(val zoneId: ZoneId, val fromAuto: Boolean)

    sealed class AutoZone {
        data class Loading(val latLng: LatLng) : AutoZone()
        data class Ok(val latLng: LatLng, val data: ZoneId) : AutoZone()
        data class Error(val latLng: LatLng, val error: Throwable) : AutoZone()
    }
}