package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PulseAppContainer
import com.example.data.local.OptimizationLogEntity
import com.example.data.local.UserPreferences
import com.example.data.service.HealthCheckWorker
import com.example.data.service.SystemIntentsHelper
import com.example.domain.model.BoostActionType
import com.example.domain.model.BoostReport
import com.example.domain.model.DeviceTelemetry
import com.example.domain.model.OptimizationOpportunity
import com.example.domain.model.TelemetryHistory
import com.example.util.FormatUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PulseMainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = PulseAppContainer.get(application)
    private val telemetryService = container.telemetryService
    private val storageService = container.storageScannerService
    private val optimizationEngine = container.optimizationEngine
    private val localRepo = container.localRepository
    private val prefsRepo = container.preferencesRepository

    val telemetry: StateFlow<DeviceTelemetry> = telemetryService.telemetry
    val history: StateFlow<TelemetryHistory> = telemetryService.history

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )

    val optimizationLogs: StateFlow<List<OptimizationLogEntity>> = localRepo.optimizationLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalBytesRecovered: StateFlow<Long> = localRepo.totalBytesRecovered.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    val opportunities: StateFlow<List<OptimizationOpportunity>> = combine(
        telemetryService.telemetry,
        storageService.summary
    ) { tele, scan ->
        optimizationEngine.buildOpportunities(tele, scan)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isBoostScanning = MutableStateFlow(false)
    val isBoostScanning: StateFlow<Boolean> = _isBoostScanning.asStateFlow()

    private val _boostScanProgress = MutableStateFlow(0f)
    val boostScanProgress: StateFlow<Float> = _boostScanProgress.asStateFlow()

    private val _boostStageMessage = MutableStateFlow("")
    val boostStageMessage: StateFlow<String> = _boostStageMessage.asStateFlow()

    private val _lastBoostReport = MutableStateFlow<BoostReport?>(null)
    val lastBoostReport: StateFlow<BoostReport?> = _lastBoostReport.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private var pollingJob: Job? = null
    private val overrideRefreshIntervalMs = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            telemetryService.refreshSnapshot()
            // Perform initial scan of app cache + any previously persisted SAF folders
            val savedFolders = localRepo.safFolders.first()
            val prefs = prefsRepo.preferences.first()
            storageService.scanAllSources(
                folders = savedFolders,
                largeFileThresholdMb = prefs.largeFileThresholdMb,
                oldDownloadDays = prefs.oldDownloadDays,
                onFolderStatsUpdated = { localRepo.upsertSafFolder(it) }
            )
            telemetryService.refreshSnapshot(
                scannedClutterBytes = storageService.summary.value.let {
                    it.duplicateExtraBytes + it.oldDownloadsBytes
                }
            )
        }
    }

    fun startMonitoring() {
        telemetryService.startSensorMonitoring()
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            combine(prefsRepo.preferences, overrideRefreshIntervalMs) { prefs, overrideMs ->
                (overrideMs ?: prefs.monitorRefreshMs).coerceIn(500L, 10000L)
            }.collectLatest { interval ->
                while (isActive) {
                    val scanSummary = storageService.summary.value
                    telemetryService.refreshSnapshot(
                        scannedClutterBytes = scanSummary.duplicateExtraBytes + scanSummary.oldDownloadsBytes
                    )
                    delay(interval)
                }
            }
        }
    }

    fun stopMonitoring() {
        telemetryService.stopSensorMonitoring()
        pollingJob?.cancel()
        pollingJob = null
    }

    fun setGamingPollingOverride(intervalMs: Long?) {
        overrideRefreshIntervalMs.value = intervalMs
    }

    fun refreshNow() {
        viewModelScope.launch {
            val scan = storageService.summary.value
            telemetryService.refreshSnapshot(
                scannedClutterBytes = scan.duplicateExtraBytes + scan.oldDownloadsBytes
            )
        }
    }

    fun runBoostDeviceAnalysis() {
        if (_isBoostScanning.value) return
        viewModelScope.launch {
            _isBoostScanning.value = true
            _boostScanProgress.value = 0.08f
            _boostStageMessage.value = "Reading ActivityManager RAM & CPU telemetry..."
            delay(360)

            _boostScanProgress.value = 0.30f
            _boostStageMessage.value = "Inspecting thermal sensors & battery health..."
            telemetryService.refreshSnapshot()
            delay(360)

            _boostScanProgress.value = 0.58f
            _boostStageMessage.value = "Auditing accessible storage & SAF folders..."
            val prefs = prefsRepo.preferences.first()
            val folders = localRepo.safFolders.first()
            val scanSummary = storageService.scanAllSources(
                folders = folders,
                largeFileThresholdMb = prefs.largeFileThresholdMb,
                oldDownloadDays = prefs.oldDownloadDays,
                onFolderStatsUpdated = { localRepo.upsertSafFolder(it) }
            )

            _boostScanProgress.value = 0.84f
            _boostStageMessage.value = "Clearing safe app cache & compiling verified report..."
            delay(300)

            val report = optimizationEngine.runFullDeviceAnalysisAndSafeBoost(
                scanSummary = scanSummary,
                autoClearAppCache = true
            )

            _boostScanProgress.value = 1f
            _lastBoostReport.value = report
            localRepo.logOptimization(
                OptimizationLogEntity(
                    timestampMillis = report.timestampMillis,
                    scoreBefore = report.scoreBefore,
                    scoreAfter = report.scoreAfter,
                    bytesRecovered = report.bytesRecovered,
                    actionsSummary = report.executedActions.joinToString(" • "),
                    honestNotes = report.honestSummary
                )
            )
            _isBoostScanning.value = false
        }
    }

    fun dismissBoostReport() {
        _lastBoostReport.value = null
    }

    fun executeOpportunity(
        opportunity: OptimizationOpportunity,
        onNavigateToStorageCleaner: () -> Unit
    ) {
        val ctx = getApplication<Application>()
        when (opportunity.actionType) {
            BoostActionType.CLEAR_APP_CACHE -> {
                viewModelScope.launch {
                    val beforeScore = telemetry.value.pulseScore.totalScore
                    val cleared = storageService.clearAppOwnCache()
                    val after = telemetryService.refreshSnapshot(
                        scannedClutterBytes = storageService.summary.value.let {
                            it.duplicateExtraBytes + it.oldDownloadsBytes
                        }
                    )
                    if (cleared > 0L) {
                        localRepo.logOptimization(
                            OptimizationLogEntity(
                                timestampMillis = System.currentTimeMillis(),
                                scoreBefore = beforeScore,
                                scoreAfter = after.pulseScore.totalScore,
                                bytesRecovered = cleared,
                                actionsSummary = "Cleared Pulse Mobile app cache",
                                honestNotes = "Removed ${FormatUtils.formatBytes(cleared)} of temporary cache files."
                            )
                        )
                        _uiMessage.value = "Cleared ${FormatUtils.formatBytes(cleared)} from app cache."
                    } else {
                        _uiMessage.value = "App cache is already clean."
                    }
                }
            }
            BoostActionType.OPEN_STORAGE_CLEANER -> onNavigateToStorageCleaner()
            BoostActionType.OPEN_SYSTEM_STORAGE -> {
                if (!SystemIntentsHelper.openStorageSettings(ctx)) {
                    _uiMessage.value = "Storage settings unavailable on this Android build."
                }
            }
            BoostActionType.OPEN_APP_MANAGEMENT -> {
                if (!SystemIntentsHelper.openAppManagementSettings(ctx)) {
                    _uiMessage.value = "App settings unavailable on this device."
                }
            }
            BoostActionType.OPEN_BATTERY_SAVER -> {
                if (!SystemIntentsHelper.openBatterySaverSettings(ctx)) {
                    _uiMessage.value = "Battery Saver settings unavailable."
                }
            }
            BoostActionType.OPEN_BATTERY_OPTIMIZATION -> {
                if (!SystemIntentsHelper.openBatteryOptimizationSettings(ctx)) {
                    _uiMessage.value = "Battery optimization settings unavailable."
                }
            }
            BoostActionType.OPEN_DISPLAY_SETTINGS -> {
                if (!SystemIntentsHelper.openDisplaySettings(ctx)) {
                    _uiMessage.value = "Display settings unavailable."
                }
            }
            BoostActionType.OPEN_DEVELOPER_ANIMATIONS -> {
                if (!SystemIntentsHelper.openDeveloperAnimationSettings(ctx)) {
                    _uiMessage.value = "Developer options not accessible."
                }
            }
            BoostActionType.OPEN_NETWORK_SETTINGS -> {
                if (!SystemIntentsHelper.openNetworkSettings(ctx)) {
                    _uiMessage.value = "Network settings unavailable."
                }
            }
            BoostActionType.NONE_INFO_ONLY -> {
                _uiMessage.value = opportunity.description
            }
        }
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun setMonitorRefreshMs(ms: Long) {
        viewModelScope.launch { prefsRepo.setMonitorRefreshMs(ms) }
    }

    fun setReducedAnimations(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setReducedAnimations(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setHapticsEnabled(enabled) }
    }

    fun setLargeFileThresholdMb(mb: Int) {
        viewModelScope.launch { prefsRepo.setLargeFileThresholdMb(mb) }
    }

    fun setOldDownloadDays(days: Int) {
        viewModelScope.launch { prefsRepo.setOldDownloadDays(days) }
    }

    fun setScheduledHealthCheck(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setScheduledHealthCheck(enabled)
            HealthCheckWorker.schedule(getApplication(), enabled)
        }
    }

    fun clearOptimizationLogs() {
        viewModelScope.launch { localRepo.clearOptimizationLogs() }
    }
}
