package com.timur.receiptlogger

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.timur.receiptlogger.data.ReceiptDatabase
import com.timur.receiptlogger.data.ReceiptEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReceiptViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = ReceiptDatabase.getInstance(application).receiptDao()

    val receipts: StateFlow<List<ReceiptEntry>> = dao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addReceipt(photoPath: String, amount: Double, note: String) {
        viewModelScope.launch {
            dao.insert(
                ReceiptEntry(
                    photoPath = photoPath,
                    amount = amount,
                    note = note,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
