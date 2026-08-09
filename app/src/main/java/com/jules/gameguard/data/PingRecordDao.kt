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

    @Query("DELETE FROM ping_records WHERE id NOT IN (SELECT id FROM ping_records ORDER BY timestamp DESC LIMIT 50)")
    suspend fun pruneOldRecords()

    @Transaction
    suspend fun insertAndPrune(record: PingRecord) {
        insertRecord(record)
        pruneOldRecords()
    }
}
