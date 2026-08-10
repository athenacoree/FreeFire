package com.jules.gameguard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedCallDao {
    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC")
    fun getAllBlockedCallsFlow(): Flow<List<BlockedCall>>

    @Insert
    suspend fun insert(blockedCall: BlockedCall)

    @Query("DELETE FROM blocked_calls")
    suspend fun clearAll()
}
