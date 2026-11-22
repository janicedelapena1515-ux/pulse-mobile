package com.example.data.local

import kotlinx.coroutines.flow.Flow

class PulseLocalRepository(private val database: PulseDatabase) {
    private val optimizationDao = database.optimizationLogDao()
    private val gameDao = database.gameLibraryDao()
    private val sessionDao = database.gamingSessionDao()
    private val safFolderDao = database.safFolderDao()

    val optimizationLogs: Flow<List<OptimizationLogEntity>> = optimizationDao.observeLogs()
    val totalBytesRecovered: Flow<Long> = optimizationDao.observeTotalBytesRecovered()
    val games: Flow<List<GameAppEntity>> = gameDao.observeGames()
    val gamingSessions: Flow<List<GamingSessionEntity>> = sessionDao.observeSessions()
    val safFolders: Flow<List<SafFolderEntity>> = safFolderDao.observeFolders()

    suspend fun logOptimization(log: OptimizationLogEntity) = optimizationDao.insertLog(log)
    suspend fun clearOptimizationLogs() = optimizationDao.clearLogs()

    suspend fun addOrUpdateGame(game: GameAppEntity) = gameDao.upsertGame(game)
    suspend fun removeGame(packageName: String) = gameDao.deleteGame(packageName)
    suspend fun updateGameProfile(packageName: String, profile: String) =
        gameDao.updateGameProfile(packageName, profile)

    suspend fun recordSession(session: GamingSessionEntity) {
        sessionDao.insertSession(session)
        val pkg = session.packageName
        if (!pkg.isNullOrBlank()) {
            gameDao.recordGamePlayTime(pkg, session.startTimeMillis, session.durationMillis)
        }
    }

    suspend fun clearGamingSessions() = sessionDao.clearSessions()

    suspend fun upsertSafFolder(folder: SafFolderEntity) = safFolderDao.upsertFolder(folder)
    suspend fun removeSafFolder(uriString: String) = safFolderDao.deleteFolder(uriString)
}
