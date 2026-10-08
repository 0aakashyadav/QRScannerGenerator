package com.aakash.qrscanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HistoryKind { SCANNED, GENERATED }

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
