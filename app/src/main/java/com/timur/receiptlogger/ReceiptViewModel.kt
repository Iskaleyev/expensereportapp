package com.timur.receiptlogger

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.timur.receiptlogger.data.ReceiptDatabase
import com.timur.receiptlogger.data.ReceiptEntry
import com.timur.receiptlogger.data.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReceiptViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ReceiptDatabase.getInstance(application)
    private val tripDao = database.tripDao()
    private val receiptDao = database.receiptDao()

    val trips: StateFlow<List<Trip>> = tripDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Suspends until the trip is inserted, then returns its new id. */
    suspend fun addTrip(name: String, startDate: Long, endDate: Long): Long =
        tripDao.insert(
            Trip(
                name = name,
                startDate = startDate,
                endDate = endDate,
                createdAt = System.currentTimeMillis()
            )
        )

    fun receiptsForTrip(tripId: Long): Flow<List<ReceiptEntry>> =
        receiptDao.getByTrip(tripId)

    fun totalForTrip(tripId: Long): Flow<Double> =
        receiptDao.getTotalForTrip(tripId)

    fun addReceipt(tripId: Long, photoPath: String, amount: Double, note: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            receiptDao.insert(
                ReceiptEntry(
                    tripId = tripId,
                    photoPath = photoPath,
                    amount = amount,
                    note = note,
                    timestamp = now,
                    createTime = now
                )
            )
        }
    }
}
