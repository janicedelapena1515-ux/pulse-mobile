package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OptimizationLogDao {
    @Query("SELECT * FROM optimization_logs ORDER BY timestampMillis DESC LIMIT 50")
    fun observeLogs(): Flow<List<OptimizationLogEntity>>

    @Query("SELECT COALESCE(SUM(bytesRecovered), 0) FROM optimization_logs")
    fun observeTotalBytesRecovered(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: OptimizationLogEntity)

    @Query("DELETE FROM optimization_logs")
    suspend fun clearLogs()
}

@Dao
interface GameLibraryDao {
    @Query("SELECT * FROM game_library ORDER BY lastPlayedMillis DESC, appName ASC")
    fun observeGames(): Flow<List<GameAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGame(game: GameAppEntity)

    @Query("DELETE FROM game_library WHERE packageName = :packageName")
    suspend fun deleteGame(packageName: String)

    @Query("UPDATE game_library SET preferredProfile = :profile WHERE packageName = :packageName")
    suspend fun updateGameProfile(packageName: String, profile: String)

    @Query("UPDATE game_library SET lastPlayedMillis = :playedAt, totalPlayMillis = totalPlayMillis + :addedMillis WHERE packageName = :packageName")
    suspend fun recordGamePlayTime(packageName: String, playedAt: Long, addedMillis: Long)
}

@Dao
interface GamingSessionDao {
    @Query("SELECT * FROM gaming_sessions ORDER BY startTimeMillis DESC LIMIT 50")
    fun observeSessions(): Flow<List<GamingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: GamingSessionEntity)

    @Query("DELETE FROM gaming_sessions")
    suspend fun clearSessions()
}

@Dao
interface SafFolderDao {
    @Query("SELECT * FROM saf_folders ORDER BY addedAtMillis DESC")
    fun observeFolders(): Flow<List<SafFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: SafFolderEntity)

    @Query("DELETE FROM saf_folders WHERE uriString = :uriString")
    suspend fun deleteFolder(uriString: String)
}
