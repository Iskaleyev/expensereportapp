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

    /** Updates an existing trip's name and dates; its id and createdAt are kept unchanged. */
    suspend fun updateTrip(tripId: Long, name: String, startDate: Long, endDate: Long) {
        val existing = trips.value.find { it.id == tripId } ?: return
        tripDao.update(existing.copy(name = name, startDate = startDate, endDate = endDate))
    }

    /** Deletes a trip and, via the foreign key's cascade, every receipt logged under it. */
    fun deleteTrip(trip: Trip) {
        viewModelScope.launch { tripDao.delete(trip) }
    }

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

    /** Updates an existing receipt's photo, amount and note; its id and both timestamps
     *  (so it keeps its original place in date-sorted and createTime-sorted lists) are kept. */
    fun updateReceipt(entry: ReceiptEntry, photoPath: String, amount: Double, note: String) {
        viewModelScope.launch {
            receiptDao.update(entry.copy(photoPath = photoPath, amount = amount, note = note))
        }
    }

    fun deleteReceipt(entry: ReceiptEntry) {
        viewModelScope.launch { receiptDao.delete(entry) }
    }
}
