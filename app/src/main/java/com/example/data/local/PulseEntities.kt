package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "optimization_logs")
data class OptimizationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampMillis: Long,
    val scoreBefore: Int,
    val scoreAfter: Int,
    val bytesRecovered: Long,
    val actionsSummary: String,
    val honestNotes: String
)

@Entity(tableName = "game_library")
data class GameAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val preferredProfile: String = "BALANCED",
    val addedAtMillis: Long = System.currentTimeMillis(),
    val lastPlayedMillis: Long = 0L,
    val totalPlayMillis: Long = 0L
)

@Entity(tableName = "gaming_sessions")
data class GamingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String?,
    val appName: String,
    val profileUsed: String,
    val startTimeMillis: Long,
    val durationMillis: Long,
    val peakBatteryTempC: Float?,
    val avgRamPercent: Float,
    val startBatteryLevel: Int,
    val endBatteryLevel: Int
)

@Entity(tableName = "saf_folders")
data class SafFolderEntity(
    @PrimaryKey val uriString: String,
    val displayName: String,
    val addedAtMillis: Long = System.currentTimeMillis(),
    val lastScannedBytes: Long = 0L,
    val lastScannedFileCount: Int = 0
)
