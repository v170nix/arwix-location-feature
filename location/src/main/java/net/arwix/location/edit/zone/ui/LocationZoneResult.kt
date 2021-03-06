package net.arwix.location.edit.zone.ui

import com.google.android.gms.maps.model.LatLng
import net.arwix.location.data.gtimezone.TimeZoneDisplayEntry
import net.arwix.location.edit.data.EditZoneData
//import org.threeten.bp.ZoneId
import java.time.ZoneId

sealed class LocationZoneResult {
    data class InitData(val data: EditZoneData?) : LocationZoneResult()
    data class Zones(val list: List<TimeZoneDisplayEntry>) : LocationZoneResult()
    data class AutoZoneLoading(val latLng: LatLng) : LocationZoneResult()
    data class AutoZoneOk(val latLng: LatLng, val data: ZoneId) : LocationZoneResult()
    data class AutoZoneError(val latLng: LatLng, val error: Throwable) : LocationZoneResult()
    data class SelectZone(val data: EditZoneData) : LocationZoneResult()
//    object ClearZone : LocationZoneResult()
}