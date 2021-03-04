package net.arwix.location.data.gtimezone

import com.google.android.gms.maps.model.LatLng
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeZoneGoogleRepository(private val key: String) {

//    private val retrofit: Retrofit by lazy {
//        Retrofit.Builder()
//            .baseUrl("https://maps.googleapis.com/")
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//    }
//    private val api: TimeZoneGoogleApi by lazy { retrofit.create(TimeZoneGoogleApi::class.java) }

    suspend fun getZoneId(latLng: LatLng): ZoneId = withContext(Dispatchers.IO) {
        val data = runCatching { autoGetTimeZoneResult(latLng) }.getOrThrow()
        data.toZoneId() ?: throw IllegalArgumentException(data.id)
    }

//    @Throws(TimeZoneGoogleResponseException::class)
//    private suspend fun autoGetTimeZoneResult(
//        latLng: LatLng,
//        language: String = "en"
//    ): TimeZoneGoogleData = api.getTimeZone(
//        "${latLng.latitude},${latLng.longitude}", ZonedDateTime.now().toEpochSecond(),
//        key,
//        language
//    ).also {
//        TimeZoneGoogleStatus.parse(it.status).also { status ->
//            if (status != TimeZoneGoogleStatus.OK)
//                throw TimeZoneGoogleResponseException(status)
//        }
//    }

    @Throws(TimeZoneGoogleResponseException::class)
    private suspend fun autoGetTimeZoneResult(
        latLng: LatLng,
        language: String = "en"
    ): TimeZoneGoogleData {
        return HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer(json)
            }
            defaultRequest {
                parameter("key", key)
                if (this.method != HttpMethod.Get) contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
            }
        }.use { client ->
            client.get("https://maps.googleapis.com/maps/api/timezone/json") {
                parameter("location", "${latLng.latitude},${latLng.longitude}")
                parameter("timestamp", ZonedDateTime.now().toEpochSecond())
            }
        }
    }

}

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = false
}

//interface TimeZoneService {
//
//    @GET("maps/api/timezone/json")
//    fun getTimeZone(
//        @Query("location") location: String,
//        @Query("timestamp") timestamp: Long,
//        @Query("key") key: String): Call<GoogleTimeZone>
//}

//@Serializable
//private data class GoogleTimeZone(
//    val dstOffset: Long,
//    val rawOffset: Long,
//    @SerialName("timeZoneId") val id: String,
//    @SerialName("timeZoneName") val name: String,
//    val status: String
//)