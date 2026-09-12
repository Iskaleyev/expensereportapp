package com.timur.receiptlogger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {

    @Insert
    suspend fun insert(entry: ReceiptEntry)

    @Query("SELECT * FROM receipts ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ReceiptEntry>>
}
