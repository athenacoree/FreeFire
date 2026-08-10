package com.jules.gameguard.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistedAppDao {
    @Query("SELECT * FROM whitelisted_apps ORDER BY appName ASC")
    fun getAllWhitelistedAppsFlow(): Flow<List<WhitelistedApp>>

    @Query("SELECT * FROM whitelisted_apps")
    suspend fun getAllWhitelistedApps(): List<WhitelistedApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: WhitelistedApp)

    @Delete
    suspend fun delete(app: WhitelistedApp)
}
