package net.arwix.location.demo

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.jakewharton.threetenabp.AndroidThreeTen
import net.arwix.location.LocationZoneIdSelectedDatabase
import net.arwix.location.data.GeocoderRepository
import net.arwix.location.data.LocationCreateEditRepository
import net.arwix.location.data.room.LocationDatabase
import net.arwix.location.export.createLocationMainFactory

class AppApplication : Application() {
    private lateinit var db: LocationDatabase
    private lateinit var locationZoneIdSelectedDatabase: LocationZoneIdSelectedDatabase
    private lateinit var locationListFactory: ViewModelProvider.Factory
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        db = Room.databaseBuilder(this, LocationDatabase::class.java, "location-db").build()
        locationZoneIdSelectedDatabase = AppLocationPreferencesSelected(
            applicationContext.getSharedPreferences(
                "location_preferences",
                Context.MODE_PRIVATE
            )
        )
        locationListFactory = createLocationMainFactory(
            this,
            locationSelectedDatabase = locationZoneIdSelectedDatabase,
            dao = db.recordDao(),
            editRepository = LocationCreateEditRepository(db.recordDao()),
            geocoderRepository = GeocoderRepository(this)
        )
    }

    fun getLocationDatabase() = db
    fun getLocationZoneIdDatabase() = locationZoneIdSelectedDatabase

    fun getLocationListFactory() = locationListFactory


}