package com.timur.receiptlogger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {

    @Insert
    suspend fun insert(entry: ReceiptEntry)

    @Update
    suspend fun update(entry: ReceiptEntry)

    @Delete
    suspend fun delete(entry: ReceiptEntry)

    @Query("SELECT * FROM receipts WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getByTrip(tripId: Long): Flow<List<ReceiptEntry>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM receipts WHERE tripId = :tripId")
    fun getTotalForTrip(tripId: Long): Flow<Double>
}
