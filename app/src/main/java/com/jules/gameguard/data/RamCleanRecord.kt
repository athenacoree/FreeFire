package com.jules.gameguard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ram_clean_records")
data class RamCleanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val ramClearedMb: Long
)
