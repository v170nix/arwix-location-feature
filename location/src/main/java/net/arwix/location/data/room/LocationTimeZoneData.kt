package net.arwix.location.data.room

import androidx.room.*
import com.google.android.gms.maps.model.LatLng
import org.threeten.bp.ZoneId

@Entity(tableName = "location_tz_table")
@TypeConverters(LocationTimeZoneData.ZoneConverters::class)
data class LocationTimeZoneData(
    @PrimaryKey(autoGenerate = true) var id: Int?,
    var name: String?,
    @ColumnInfo(name = "sub_name") var subName: String?,
    @ColumnInfo(name = "lat_lng") var latLng: LatLng,
    var zone: ZoneId,
    var zoom: Float? = null,
    var bearing: Float? = null,
    var tilt: Float? = null
) {
    internal class ZoneConverters {

        @TypeConverter
        fun latLngToStr(latLng: LatLng?): String? = latLng?.let {
            buildString {
                append(it.latitude)
                append(";")
                append(it.longitude)
            }
        }

        @Suppress("SimpleRedundantLet")
        @TypeConverter
        fun strToLatLng(string: String?): LatLng? = string?.let {
            it.split(";").runCatching {
                LatLng(get(0).toDouble(), get(1).toDouble())
            }.getOrNull()
        }

        @TypeConverter
        fun zoneIdToStr(zone: ZoneId?): String? = zone?.id

        @TypeConverter
        fun strToZoneId(string: String?): ZoneId? =
            string.runCatching { ZoneId.of(this) }.getOrNull()
    }
}