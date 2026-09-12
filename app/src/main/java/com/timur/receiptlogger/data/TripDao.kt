package com.timur.receiptlogger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    /** Returns the new trip's generated id. */
    @Insert
    suspend fun insert(trip: Trip): Long

    @Update
    suspend fun update(trip: Trip)

    /** Deleting a trip cascades to every receipt logged under it (see ReceiptEntry's foreign key). */
    @Delete
    suspend fun delete(trip: Trip)

    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Trip>>
}
