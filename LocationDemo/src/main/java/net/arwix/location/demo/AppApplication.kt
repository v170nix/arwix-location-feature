package net.arwix.location.demo

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.jakewharton.threetenabp.AndroidThreeTen
import net.arwix.location.LocationZoneIdSelectedDatabase
import net.arwix.location.data.GeocoderRepository
import net.arwix.location.data.LocationCreateEditRepository
import net.arwix.location.data.TimeZoneGoogleRepository
import net.arwix.location.data.TimeZoneRepository
import net.arwix.location.data.room.LocationDatabase
import net.arwix.location.domain.LocationGeocoderUseCase
import net.arwix.location.export.createLocationListFactory
import net.arwix.location.export.createLocationPositionFactory
import net.arwix.location.export.createLocationZoneFactory

class AppApplication : Application() {
    private lateinit var db: LocationDatabase
    private lateinit var locationZoneIdSelectedDatabase: LocationZoneIdSelectedDatabase
    private lateinit var locationListFactory: ViewModelProvider.Factory
    private lateinit var locationPositionFactory: ViewModelProvider.Factory
    private lateinit var locationZoneFactory: ViewModelProvider.Factory
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
        val editRepository =
            LocationCreateEditRepository(db.recordDao(), locationZoneIdSelectedDatabase)
        val geocoderRepository = GeocoderRepository(this)
        val geocoderUseCase = LocationGeocoderUseCase(geocoderRepository)
        val timeZoneRepository = TimeZoneRepository(this)
        val googleZoneRepository =
            TimeZoneGoogleRepository("")
        locationListFactory = createLocationListFactory(
            this,
            locationSelectedDatabase = locationZoneIdSelectedDatabase,
            dao = db.recordDao(),
            editRepository = editRepository,
            geocoderRepository = geocoderRepository
        )

        locationPositionFactory = createLocationPositionFactory(
            editRepository,
            geocoderUseCase
        )

        locationZoneFactory = createLocationZoneFactory(
            editRepository = editRepository,
            timeZoneRepository = timeZoneRepository,
            googleZoneRepository = googleZoneRepository
        )
    }

    fun getLocationDatabase() = db
    fun getLocationZoneIdDatabase() = locationZoneIdSelectedDatabase

    fun getLocationListFactory() = locationListFactory
    fun getLocationPositionFactory() = locationPositionFactory
    fun getLocationZoneFactory() = locationZoneFactory


}