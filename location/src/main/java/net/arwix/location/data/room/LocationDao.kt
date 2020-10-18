package net.arwix.location.data.room

import android.location.Address
import android.location.Location
import androidx.room.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import net.arwix.location.data.getSubTitle
import net.arwix.location.data.getTitle
import org.threeten.bp.ZoneId

@Dao
abstract class LocationDao {
    @Query("SELECT * FROM location_tz_table WHERE isAuto = 0")
    abstract fun getAllExceptAuto(): Flow<List<LocationTimeZoneData>>

    @Query("SELECT * FROM location_tz_table WHERE id = :id LIMIT 1")
    abstract suspend fun getItem(id: Int): LocationTimeZoneData?

    @Query("SELECT * FROM location_tz_table WHERE isAuto = 1 LIMIT 1")
    abstract suspend fun getAutoItem(): LocationTimeZoneData?

    @Query("SELECT * FROM location_tz_table WHERE isAuto = 1 LIMIT 1")
    abstract fun getAutoItemAsFlow(): Flow<LocationTimeZoneData>

    @Query("SELECT * FROM location_tz_table WHERE isSelected = 1 LIMIT 1")
    abstract suspend fun getSelectedItem(): LocationTimeZoneData?

//    @Transaction
//    open suspend fun selectItem(id: Int, isAuto: Boolean) {
//        allDeselect()
//        if (isAuto) allDeselectAuto()
//        innerSelectItem(id)
//        if (isAuto) innerSetAutoItem(id)
//    }

    @Transaction
    open suspend fun selectCustomItem(data: LocationTimeZoneData) {
        allDeselect()
        update(data)
    }

    @Transaction
    open suspend fun updateAutoItem(location: Location, zoneId: ZoneId) {
        val autoItem = getAutoItem()
        if (autoItem != null) {
            update(
                autoItem.copy(
                    latLng = LatLng(location.latitude, location.longitude),
                    altitude = location.altitude,
                    zone = zoneId
                )
            )
        } else {
            insert(
                LocationTimeZoneData(
                    id = null,
                    name = null,
                    subName = null,
                    latLng = LatLng(location.latitude, location.longitude),
                    altitude = location.altitude,
                    zone = zoneId,
                    isAuto = true
                )
            )
        }
    }

    @Transaction
    open suspend fun updateAutoItem(address: Address) {
        val autoItem = getAutoItem()
        if (autoItem != null) {
            update(
                autoItem.copy(
                    name = address.getTitle(),
                    subName = address.getSubTitle()
                )
            )
        }
    }

    @Transaction
    open suspend fun selectAutoItem() {
        allDeselect()
        selectAuto()
    }

    @Insert
    abstract suspend fun insert(data: LocationTimeZoneData)

    @Update
    abstract suspend fun update(data: LocationTimeZoneData)

    @Query("DELETE FROM location_tz_table WHERE id = :id")
    abstract suspend fun deleteById(id: Int)

//    @Query("UPDATE location_tz_table SET isAuto = 0")
//    protected abstract suspend fun allDeselectAuto()

//    @Query("UPDATE location_tz_table SET isAuto = 1 WHERE id = :id")
//    protected abstract suspend fun innerSetAutoItem(id: Int)

    @Query("UPDATE location_tz_table SET isSelected = 1 WHERE isAuto = 1")
    protected abstract suspend fun selectAuto()

    @Query("UPDATE location_tz_table SET isSelected = 0")
    protected abstract suspend fun allDeselect()


//    @Query("UPDATE location_tz_table SET isSelected = 1 WHERE id = :id")
//    protected abstract suspend fun innerSelectItem(id: Int)
}