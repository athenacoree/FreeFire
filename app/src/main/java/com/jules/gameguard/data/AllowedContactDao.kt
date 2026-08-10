package com.jules.gameguard.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AllowedContactDao {
    @Query("SELECT * FROM allowed_contacts ORDER BY name ASC")
    fun getAllAllowedContactsFlow(): Flow<List<AllowedContact>>

    @Query("SELECT * FROM allowed_contacts")
    suspend fun getAllAllowedContacts(): List<AllowedContact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: AllowedContact)

    @Delete
    suspend fun delete(contact: AllowedContact)
}
