package net.arwix.location.ui.list

import android.location.Address
import android.location.Location
import androidx.lifecycle.LiveDataScope
import com.google.android.gms.maps.model.LatLng
import net.arwix.location.data.getSubTitle
import net.arwix.location.data.getTitle
import net.arwix.location.data.room.LocationTimeZoneData
import org.threeten.bp.ZoneId

sealed class LocationListResult {

    suspend fun emitTo(scope: LiveDataScope<LocationListResult>) {
        scope.emit(this)
    }

    object PermissionGranted : LocationListResult()
    data class PermissionDenied(val shouldRationale: Boolean) : LocationListResult()
    data class AutoLocation(val autoItem: AutoItem) : LocationListResult()

    data class Select(val item: LocationTimeZoneData, val isAuto: Boolean) : LocationListResult()
    object Deselect : LocationListResult()

    data class Init(
        val permission: Boolean,
        val list: List<LocationTimeZoneData>,
        val autoItem: AutoItem,
        val selectedItem: Select? = null
    ) : LocationListResult()

    data class ManualList(val list: List<LocationTimeZoneData>) : LocationListResult()

    sealed class AutoItem {
        data class None(val isPermissionAllow: Boolean) : AutoItem()
        object UpdateBegin : AutoItem()
        data class UpdateEnd(val location: LocationTimeZoneData?) : AutoItem() {
            constructor(
                location: Location,
                zoneId: ZoneId,
                address: Address? = null
            ) : this(createLocationTimeZoneData(location, zoneId, address))
        }

        data class Success(val location: LocationTimeZoneData) : AutoItem() {
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