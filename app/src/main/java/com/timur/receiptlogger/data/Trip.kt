package com.timur.receiptlogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A business trip. Every receipt belongs to exactly one of these. */
@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Epoch millis for the trip's start and end dates (date-only, no time-of-day meaning). */
    val startDate: Long,
    val endDate: Long,
    val createdAt: Long
)
