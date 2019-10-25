package net.arwix.location.data

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TimeZoneGoogleRepository(private val key: String) {

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    private val api by lazy { retrofit.create(TimeZoneGoogleApi::class.java) }

    suspend fun getZoneId(latLng: LatLng): ZoneId = withContext(Dispatchers.IO) {
        val data = runCatching { autoGetTimeZoneResult(latLng) }.getOrThrow()
        data.toZoneId() ?: throw IllegalArgumentException(data.id)
    }

    @Throws(TimeZoneGoogleResponseException::class)
    private suspend fun autoGetTimeZoneResult(
        latLng: LatLng,
        language: String = "en"
    ) = api.getTimeZone(
        "${latLng.latitude},${latLng.longitude}", ZonedDateTime.now().toEpochSecond(),
        key,
        language
    ).also {
        TimeZoneGoogleStatus.parse(it.status).also { status ->
            if (status != TimeZoneGoogleStatus.OK)
                throw TimeZoneGoogleResponseException(status)
        }
    }

}