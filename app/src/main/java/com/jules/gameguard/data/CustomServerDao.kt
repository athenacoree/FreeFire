package com.jules.gameguard.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomServerDao {
    @Query("SELECT * FROM custom_servers ORDER BY name ASC")
    fun getAllCustomServersFlow(): Flow<List<CustomServer>>

    @Query("SELECT * FROM custom_servers")
    suspend fun getAllCustomServers(): List<CustomServer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(server: CustomServer)

    @Delete
    suspend fun delete(server: CustomServer)
}
