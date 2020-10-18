package net.arwix.location.data.room

import androidx.room.*
import com.google.android.gms.maps.model.LatLng
import org.threeten.bp.ZoneId

@Entity(tableName = "location_tz_table")
@TypeConverters(LocationTimeZoneData.ZoneConverters::class)
data class LocationTimeZoneData(
    @PrimaryKey(autoGenerate = true) val id: Int?,
    val name: String?,
    @ColumnInfo(name = "sub_name") val subName: String?,
    @ColumnInfo(name = "lat_lng") val latLng: LatLng,
    val altitude: Double = 0.0,
    val zone: ZoneId,
    val zoom: Float? = null,
    val bearing: Float? = null,
    val tilt: Float? = null,
    val isSelected: Boolean = false,
    val isAuto: Boolean = false
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