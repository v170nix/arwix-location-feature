package net.arwix.location.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.io.IOException
import java.util.*

class GeocoderRepository(context: Context) {

    private val geocoder: Geocoder? = if (Geocoder.isPresent()) {
        Geocoder(context, Locale.getDefault())
    } else null


    @Throws(IOException::class)
    fun getAddress(latitude: Double, longitude: Double): Address? =
        geocoder?.getFromLocation(latitude, longitude, 1)?.getOrNull(0)
}

fun Address.getTitle(): String {
    return premises
        ?: thoroughfare?.let {
            if (it == "Unnamed Road") return@let null
            return@let try {
                Integer.parseInt(it)
                null
            } catch (e: Exception) {
                it
            }
        }
        ?: locality
        ?: subAdminArea
        ?: adminArea
        ?: countryName
        ?: ""
}

fun Address.getSubTitle(): String {
    val placeName = getTitle()
    val shortCountry = countryCode
    val level1 = adminArea
    val locality = this.locality
    val list = ArrayList<String>()
    if (locality != null && locality != placeName) list += locality
    if (level1 != null && level1 != placeName) list += level1
    if (shortCountry != null && shortCountry != placeName) list += shortCountry
    return list.joinToString()
}