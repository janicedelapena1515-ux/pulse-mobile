package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        OptimizationLogEntity::class,
        GameAppEntity::class,
        GamingSessionEntity::class,
        SafFolderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun optimizationLogDao(): OptimizationLogDao
    abstract fun gameLibraryDao(): GameLibraryDao
    abstract fun gamingSessionDao(): GamingSessionDao
    abstract fun safFolderDao(): SafFolderDao

    companion object {
        @Volatile
        private var INSTANCE: PulseDatabase? = null

        fun getInstance(context: Context): PulseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PulseDatabase::class.java,
                    "pulse_mobile_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
