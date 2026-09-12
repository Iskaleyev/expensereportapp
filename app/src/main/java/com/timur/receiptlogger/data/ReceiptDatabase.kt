package com.timur.receiptlogger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ReceiptEntry::class], version = 1, exportSchema = false)
abstract class ReceiptDatabase : RoomDatabase() {

    abstract fun receiptDao(): ReceiptDao

    companion object {
        @Volatile
        private var INSTANCE: ReceiptDatabase? = null

        fun getInstance(context: Context): ReceiptDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ReceiptDatabase::class.java,
                    "receipts.db"
                ).build().also { INSTANCE = it }
            }
    }
}
