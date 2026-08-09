package com.jules.gameguard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ping_records")
data class PingRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val pingMs: Long,
    val packetLossPercent: Int,
    val status: String // "Buena", "Regular", "Mala"
)
