package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PulseAppContainer
import com.example.data.local.GameAppEntity
import com.example.data.local.GamingSessionEntity
import com.example.data.service.SystemIntentsHelper
import com.example.domain.model.ActiveGamingSession
import com.example.domain.model.GamingProfile
import com.example.domain.model.InstalledAppCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GamingViewModel(application: Application) : AndroidViewModel(application) {

    private val container = PulseAppContainer.get(application)
    private val localRepo = container.localRepository
    private val prefsRepo = container.preferencesRepository
    private val telemetryService = container.telemetryService

    val games: StateFlow<List<GameAppEntity>> = localRepo.games.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val sessions: StateFlow<List<GamingSessionEntity>> = localRepo.gamingSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val selectedProfile: StateFlow<GamingProfile> = prefsRepo.preferences
        .map { it.activeGamingProfile }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GamingProfile.BALANCED
        )

    private val _installedCandidates = MutableStateFlow<List<InstalledAppCandidate>>(emptyList())
    val installedCandidates: StateFlow<List<InstalledAppCandidate>> = _installedCandidates.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _activeSession = MutableStateFlow<ActiveGamingSession?>(null)
    val activeSession: StateFlow<ActiveGamingSession?> = _activeSession.asStateFlow()

    private val _gamingFeedback = MutableStateFlow<String?>(null)
    val gamingFeedback: StateFlow<String?> = _gamingFeedback.asStateFlow()

    private var timerJob: Job? = null
    private var dndAppliedBySession = false

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val list = withContext(Dispatchers.IO) {
                SystemIntentsHelper.queryLaunchableApps(getApplication())
            }
            _installedCandidates.value = list
            _isLoadingApps.value = false
        }
    }

    fun addGame(candidate: InstalledAppCandidate) {
        viewModelScope.launch {
            localRepo.addOrUpdateGame(
                GameAppEntity(
                    packageName = candidate.packageName,
                    appName = candidate.appName,
                    preferredProfile = selectedProfile.value.name
                )
            )
            _gamingFeedback.value = "Added ${candidate.appName} to Game Library."
        }
    }

    fun removeGame(packageName: String) {
        viewModelScope.launch {
            localRepo.removeGame(packageName)
        }
    }

    fun selectProfile(profile: GamingProfile) {
        viewModelScope.launch {
            prefsRepo.setActiveGamingProfile(profile)
            val ctx = getApplication<Application>()
            val appliedNotes = mutableListOf<String>()
            val pendingNotes = mutableListOf<String>()

            if (profile.enableDoNotDisturb) {
                if (SystemIntentsHelper.applyDoNotDisturbIfPermitted(ctx, true)) {
                    dndAppliedBySession = true
                    appliedNotes += "DND enabled"
                } else {
                    pendingNotes += "Grant DND Policy Access to toggle Do Not Disturb"
                }
            } else if (dndAppliedBySession) {
                if (SystemIntentsHelper.applyDoNotDisturbIfPermitted(ctx, false)) {
                    dndAppliedBySession = false
                    appliedNotes += "DND restored"
                }
            }

            profile.targetBrightness?.let { target ->
                if (SystemIntentsHelper.applyBrightnessIfPermitted(ctx, target)) {
                    appliedNotes += "Brightness set to ${(target * 100 / 255)}%"
                } else {
                    pendingNotes += "Allow Modify System Settings for auto-brightness tuning"
                }
            }

            telemetryService.refreshSnapshot()

            val msg = buildString {
                append("${profile.title} profile selected. ")
                if (appliedNotes.isNotEmpty()) append("Applied: ${appliedNotes.joinToString(", ")}. ")
                if (pendingNotes.isNotEmpty()) append(pendingNotes.joinToString("; "))
            }
            _gamingFeedback.value = msg.trim()
        }
    }

    fun startSession(
        game: GameAppEntity? = null,
        launchGameApp: Boolean = false,
        onPollingIntervalOverride: (Long?) -> Unit
    ) {
        val ctx = getApplication<Application>()
        val profile = game?.let {
            runCatching { GamingProfile.valueOf(it.preferredProfile) }.getOrNull()
        } ?: selectedProfile.value

        selectProfile(profile)
        onPollingIntervalOverride(profile.telemetryIntervalMs)

        val currentTele = telemetryService.telemetry.value
        val now = System.currentTimeMillis()
        _activeSession.value = ActiveGamingSession(
            packageName = game?.packageName,
            appName = game?.appName ?: "Custom Gaming Session",
            profile = profile,
            startTimeMillis = now,
            elapsedMillis = 0L,
            startBatteryLevel = currentTele.batteryLevel,
            peakBatteryTempC = currentTele.batteryTempC,
            ramSamples = listOf(currentTele.ramPercent)
        )

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val session = _activeSession.value ?: break
                val tele = telemetryService.telemetry.value
                val peakTemp = listOfNotNull(session.peakBatteryTempC, tele.batteryTempC).maxOrNull()
                _activeSession.value = session.copy(
                    elapsedMillis = System.currentTimeMillis() - session.startTimeMillis,
                    peakBatteryTempC = peakTemp,
                    ramSamples = (session.ramSamples + tele.ramPercent).takeLast(120)
                )
            }
        }

        if (launchGameApp && game != null) {
            if (!SystemIntentsHelper.launchInstalledApp(ctx, game.packageName)) {
                _gamingFeedback.value = "Could not launch ${game.appName}."
            }
        }
    }

    fun stopSession(onPollingIntervalOverride: (Long?) -> Unit) {
        val session = _activeSession.value ?: return
        timerJob?.cancel()
        timerJob = null
        _activeSession.value = null
        onPollingIntervalOverride(null)

        viewModelScope.launch {
            if (dndAppliedBySession) {
                SystemIntentsHelper.applyDoNotDisturbIfPermitted(getApplication(), false)
                dndAppliedBySession = false
            }
            val tele = telemetryService.telemetry.value
            val duration = (System.currentTimeMillis() - session.startTimeMillis).coerceAtLeast(1000L)
            val avgRam = if (session.ramSamples.isNotEmpty()) {
                session.ramSamples.average().toFloat()
            } else {
                tele.ramPercent
            }

            localRepo.recordSession(
                GamingSessionEntity(
                    packageName = session.packageName,
                    appName = session.appName,
                    profileUsed = session.profile.title,
                    startTimeMillis = session.startTimeMillis,
                    durationMillis = duration,
                    peakBatteryTempC = session.peakBatteryTempC,
                    avgRamPercent = avgRam,
                    startBatteryLevel = session.startBatteryLevel,
                    endBatteryLevel = tele.batteryLevel
                )
            )
            _gamingFeedback.value = "Saved ${session.appName} gaming session to statistics."
        }
    }

    fun clearFeedback() {
        _gamingFeedback.value = null
    }
}
