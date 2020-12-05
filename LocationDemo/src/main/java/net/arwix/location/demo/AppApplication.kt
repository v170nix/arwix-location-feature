package net.arwix.location.demo

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
//import com.jakewharton.threetenabp.AndroidThreeTen
import net.arwix.location.data.GeocoderRepository
import net.arwix.location.data.gtimezone.TimeZoneGoogleRepository
import net.arwix.location.data.gtimezone.TimeZoneRepository
import net.arwix.location.data.room.LocationDatabase
import net.arwix.location.edit.domain.LocationCreateEditUseCase
import net.arwix.location.export.createLocationListFactory
import net.arwix.location.export.createLocationPositionFactory
import net.arwix.location.export.createLocationZoneFactory

class AppApplication : Application() {
    private lateinit var db: LocationDatabase
    private lateinit var locationListFactory: ViewModelProvider.Factory
    private lateinit var locationPositionFactory: ViewModelProvider.Factory
    private lateinit var locationZoneFactory: ViewModelProvider.Factory
    override fun onCreate() {
        super.onCreate()
//        AndroidThreeTen.init(this)
        db = Room.databaseBuilder(this, LocationDatabase::class.java, "location-db")
            .fallbackToDestructiveMigration()
            .build()
        val editRepository =
            LocationCreateEditUseCase(db.recordDao())
        val geocoderRepository = GeocoderRepository(this)
        val timeZoneRepository = TimeZoneRepository(this)
        val googleZoneRepository =
            TimeZoneGoogleRepository(resources.getString(R.string.google_timezone_key))
        locationListFactory = createLocationListFactory(
            this,
            dao = db.recordDao(),
            editUseCase = editRepository,
            geocoderRepository = geocoderRepository
        )

        locationPositionFactory = createLocationPositionFactory(
            editRepository,
            geocoderRepository
        )

        locationZoneFactory = createLocationZoneFactory(
            editUseCase = editRepository,
            timeZoneRepository = timeZoneRepository,
            googleZoneRepository = googleZoneRepository
        )
    }

    fun getLocationDatabase() = db

    fun getLocationListFactory() = locationListFactory
    fun getLocationPositionFactory() = locationPositionFactory
    fun getLocationZoneFactory() = locationZoneFactory


}