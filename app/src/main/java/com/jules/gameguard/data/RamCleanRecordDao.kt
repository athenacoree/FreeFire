package com.jules.gameguard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RamCleanRecordDao {
    @Query("SELECT * FROM ram_clean_records ORDER BY timestamp DESC")
    fun getAllRecordsFlow(): Flow<List<RamCleanRecord>>

    @Query("SELECT * FROM ram_clean_records ORDER BY timestamp DESC")
    suspend fun getAllRecords(): List<RamCleanRecord>

    @Insert
    suspend fun insert(record: RamCleanRecord)

    @Query("SELECT SUM(ramClearedMb) FROM ram_clean_records")
    fun getTotalRamClearedMbFlow(): Flow<Long?>

    @Query("SELECT SUM(ramClearedMb) FROM ram_clean_records")
    suspend fun getTotalRamClearedMb(): Long?
}
