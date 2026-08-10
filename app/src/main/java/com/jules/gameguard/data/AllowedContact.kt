package com.jules.gameguard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "allowed_contacts")
data class AllowedContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val name: String
)
