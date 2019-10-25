package net.arwix.location.ui.zone

import com.google.android.gms.maps.model.LatLng
import net.arwix.location.data.TimeZoneDisplayEntry
import org.threeten.bp.ZoneId

sealed class LocationZoneResult {
    data class Zones(val list: List<TimeZoneDisplayEntry>) : LocationZoneResult()
    data class AutoZoneLoading(val latLng: LatLng) : LocationZoneResult()
    data class AutoZoneOk(val latLng: LatLng, val data: ZoneId) : LocationZoneResult()
    data class AutoZoneError(val latLng: LatLng, val error: Throwable) : LocationZoneResult()
    data class SelectZone(val selectZoneId: LocationZoneState.SelectZoneId) : LocationZoneResult()
    object ClearZone : LocationZoneResult()
}