package net.arwix.location.data

import java.time.ZoneId
import java.time.ZoneOffset

//import org.threeten.bp.ZoneId
//import org.threeten.bp.ZoneOffset

data class TimeZoneDisplayEntry(
    val id: ZoneId,
    val zoneOffset: ZoneOffset,
    val displayName: String,
    val displayLongName: String,
    val gmtOffsetString: String
)