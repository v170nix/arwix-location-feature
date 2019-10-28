package net.arwix.location.demo

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.arwix.location.LocationZoneIdSelectedDatabase
import net.arwix.location.data.LocationZoneId
import org.threeten.bp.ZoneId

class AppLocationPreferencesSelected(private val sharedPreferences: SharedPreferences) :
    LocationZoneIdSelectedDatabase {

    private val channel = ConflatedBroadcastChannel<LocationZoneId>()

    init {
        GlobalScope.launch {
            getLZData()?.run(channel::offer)
        }
    }

    override suspend fun setLZData(data: LocationZoneId): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putBoolean("app.location.isAuto", data is LocationZoneId.Auto)
            .putInt("app.location.id", if (data is LocationZoneId.Manual) data.id else -1)
            .putString("app.location.name", data.name)
            .putString("app.location.subName", data.subName)
            .putString("app.location.lat", data.latitude.toString())
            .putString("app.location.lng", data.longitude.toString())
            .putString("app.location.alt", data.altitude.toString())
            .putString("app.location.zoneId", data.zoneId.id)
            .commit().also {
                channel.offer(data)
            }
    }


    override suspend fun getLZData(): LocationZoneId? = try {
        val isAuto = sharedPreferences.getBoolean("app.location.isAuto", false)
        val id = sharedPreferences.getInt("app.location.id", -1)
        val name = sharedPreferences.getString("app.location.name", null)
        val subName = sharedPreferences.getString("app.location.subName", null)
        val lat = sharedPreferences.getString("app.location.lat", null)!!.toDouble()
        val lng = sharedPreferences.getString("app.location.lng", null)!!.toDouble()
        val alt = sharedPreferences.getString("app.location.alt", null)!!.toDouble()
        val zoneId = ZoneId.of(sharedPreferences.getString("app.location.zoneId", null)!!)
        if (isAuto) LocationZoneId.Auto(name, subName, lat, lng, alt, zoneId)
        else LocationZoneId.Manual(id, name, subName, lat, lng, alt, zoneId)
    } catch (i: Exception) {
        null
    }

    override fun share() = channel.openSubscription()
}