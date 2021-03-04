package net.arwix.location.data.gtimezone

import android.os.Build
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZoneId

//import org.threeten.bp.ZoneId
@Serializable
data class TimeZoneGoogleData(
    val dstOffset: Long,
    val rawOffset: Long,
    @SerialName("timeZoneId") val id: String,
    @SerialName("timeZoneName") val name: String,
    val status: String
) {
    fun toZoneId(): ZoneId? = runCatching {
        ZoneId.of(id)
    }.getOrElse {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            runCatching { ZoneId.of(android.icu.util.TimeZone.getCanonicalID(id)) }.getOrNull()
        else null
    }
}

@Suppress("unused")
enum class TimeZoneGoogleStatus {
    OK, INVALID_REQUEST, OVER_DAILY_LIMIT, OVER_QUERY_LIMIT, REQUEST_DENIED, UNKNOWN_ERROR, ZERO_RESULTS;

    companion object {
        fun parse(string: String) = values().find { it.name == string }
    }
}

@Suppress("MemberVisibilityCanBePrivate")
class TimeZoneGoogleResponseException(val status: TimeZoneGoogleStatus?) : Exception() {
    override val message: String?
        get() = status?.name

}