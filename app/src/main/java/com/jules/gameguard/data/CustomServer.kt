package com.jules.gameguard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_servers")
data class CustomServer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ipOrDomain: String
)
