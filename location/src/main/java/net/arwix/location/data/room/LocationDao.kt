package net.arwix.location.data.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface LocationDao {
    @Query("SELECT * FROM location_tz_table")
    fun getAll(): LiveData<List<LocationTimeZoneData>>

    @Query("SELECT * FROM location_tz_table WHERE id = :id LIMIT 1")
    suspend fun getItem(id: Int): LocationTimeZoneData?

    @Insert
    suspend fun insert(data: LocationTimeZoneData)

    @Update
    suspend fun update(data: LocationTimeZoneData)

    @Query("DELETE FROM location_tz_table WHERE id = :id")
    suspend fun deleteById(id: Int)
}