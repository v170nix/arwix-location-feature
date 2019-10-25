package net.arwix.location.ui.list

import android.location.Address
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import net.arwix.location.data.getSubTitle
import net.arwix.location.data.getTitle
import net.arwix.location.data.room.LocationTimeZoneData
import org.threeten.bp.ZoneId

sealed class LocationMainResult {
    object PermissionGranted : LocationMainResult()
    data class PermissionDenied(val shouldRationale: Boolean) : LocationMainResult()
    data class AutoLocation(val locationResult: LocationMainAutoResult) : LocationMainResult()

    data class Select(val item: LocationTimeZoneData, val isAuto: Boolean) : LocationMainResult()
    object Deselect : LocationMainResult()

    sealed class LocationMainAutoResult {
        data class None(val isPermissionAllow: Boolean) : LocationMainAutoResult()
        object UpdateBegin : LocationMainAutoResult()
        data class UpdateEnd(val location: LocationTimeZoneData?) : LocationMainAutoResult() {
            constructor(
                location: Location,
                zoneId: ZoneId,
                address: Address? = null
            ) : this(createLocationTimeZoneData(location, zoneId, address))
        }

        data class Success(val location: LocationTimeZoneData) : LocationMainAutoResult() {
            constructor(
                location: Location,
                zoneId: ZoneId,
                address: Address? = null
            ) : this(createLocationTimeZoneData(location, zoneId, address))


        }

        companion object {
            private fun createLocationTimeZoneData(
                location: Location,
                zoneId: ZoneId,
                address: Address? = null
            ) =
                LocationTimeZoneData(
                    id = null,
                    name = address?.getTitle(),
                    subName = address?.getSubTitle(),
                    latLng = LatLng(location.latitude, location.longitude),
                    zone = zoneId
                )
        }
    }

}