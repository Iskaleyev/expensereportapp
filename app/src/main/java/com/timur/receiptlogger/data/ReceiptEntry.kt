package com.timur.receiptlogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One logged receipt: the photo taken of it, the amount the user typed in,
 * an optional note, and when it was saved.
 */
@Entity(tableName = "receipts")
data class ReceiptEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val photoPath: String,
    val amount: Double,
    val note: String,
    val timestamp: Long
)
