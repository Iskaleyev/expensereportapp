package com.timur.receiptlogger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    /** Returns the new trip's generated id. */
    @Insert
    suspend fun insert(trip: Trip): Long

    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Trip>>
}
