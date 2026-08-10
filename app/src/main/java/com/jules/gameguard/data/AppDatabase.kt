package com.jules.gameguard.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PingRecord::class,
        BlockedCall::class,
        AllowedContact::class,
        WhitelistedApp::class,
        CustomServer::class,
        RamCleanRecord::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pingRecordDao(): PingRecordDao
    abstract fun blockedCallDao(): BlockedCallDao
    abstract fun allowedContactDao(): AllowedContactDao
    abstract fun whitelistedAppDao(): WhitelistedAppDao
    abstract fun customServerDao(): CustomServerDao
    abstract fun ramCleanRecordDao(): RamCleanRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gameguard_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
