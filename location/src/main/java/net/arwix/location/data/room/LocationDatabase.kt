package net.arwix.location.data.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LocationTimeZoneData::class], version = 5)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun recordDao(): LocationDao
}