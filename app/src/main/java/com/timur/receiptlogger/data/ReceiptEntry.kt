package com.timur.receiptlogger.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One logged receipt: the photo taken of it, the amount the user typed in,
 * an optional note, and when it was saved. Always belongs to a [Trip].
 */
@Entity(
    tableName = "receipts",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class ReceiptEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val photoPath: String,
    val amount: Double,
    val note: String,
    val timestamp: Long
)
