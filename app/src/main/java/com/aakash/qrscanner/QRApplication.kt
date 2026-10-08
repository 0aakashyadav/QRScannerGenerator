package com.aakash.qrscanner

import android.app.Application
import androidx.room.Room
import com.aakash.qrscanner.data.AppDatabase
import com.aakash.qrscanner.data.HistoryRepository

class QRApplication : Application() {
    lateinit var repository: HistoryRepository
        private set
    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "qr_history.db").build()
        repository = HistoryRepository(db.historyDao())
    }
}
