package net.arwix.location.common.extension

import com.google.android.libraries.places.api.model.Place

fun Place.getSubTitle(): String {
    val placeName = name
    return addressComponents?.asList()?.run {
        val shortCountry = find { it.types.indexOf("country") > -1 }?.shortName
        val level1 = find { it.types.indexOf("administrative_area_level_1") > -1 }?.name
        val locality = find { it.types.indexOf("locality") > -1 }?.name
        val list = ArrayList<String>()
        if (locality != null && locality != placeName) list += locality
        if (level1 != null && level1 != placeName) list += level1
        if (shortCountry != null && shortCountry != placeName) list += shortCountry
        list.joinToString()
    } ?: ""
}