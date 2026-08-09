package com.jules.gameguard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PingRecordDao {

    @Query("SELECT * FROM ping_records ORDER BY timestamp DESC")
    fun getAllRecordsFlow(): Flow<List<PingRecord>>

    @Query("SELECT * FROM ping_records ORDER BY timestamp DESC")
    suspend fun getAllRecords(): List<PingRecord>

    @Insert
    suspend fun insertRecord(record: PingRecord)

    @Query("DELETE FROM ping_records WHERE timestamp < :cutoffTimestamp")
    suspend fun pruneOldRecords(cutoffTimestamp: Long)

    @Transaction
    suspend fun insertAndPrune(record: PingRecord) {
        insertRecord(record)
        // Keep records for the last 7 days (7 days * 24 hours * 60 mins * 60 secs * 1000 ms)
        val sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000
        val cutoff = System.currentTimeMillis() - sevenDaysInMillis
        pruneOldRecords(cutoff)
    }
}
