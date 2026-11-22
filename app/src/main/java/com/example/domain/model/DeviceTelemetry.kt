package com.example.domain.model

enum class BottleneckSeverity {
    INFO,
    MODERATE,
    HIGH
}

data class PerformanceBottleneck(
    val id: String,
    val title: String,
    val detail: String,
    val recommendation: String,
    val severity: BottleneckSeverity
)

data class PulseScoreBreakdown(
    val totalScore: Int = 0,
    val storageScore: Int = 0,       // out of 30
    val memoryScore: Int = 0,        // out of 25
    val thermalBatteryScore: Int = 0,// out of 25
    val readinessScore: Int = 0,     // out of 20
    val ratingLabel: String = "Analyzing",
    val bottlenecks: List<PerformanceBottleneck> = emptyList()
)

data class TelemetryHistory(
    val ramPercentHistory: List<Float> = emptyList(),
    val cpuFreqNormalizedHistory: List<Float> = emptyList(),
    val netRxKbpsHistory: List<Float> = emptyList(),
    val netTxKbpsHistory: List<Float> = emptyList(),
    val batteryTempHistory: List<Float> = emptyList()
)

data class DeviceTelemetry(
    val deviceName: String = "",
    val manufacturer: String = "",
    val model: String = "",
    val androidVersion: String = "",
    val sdkInt: Int = 0,
    val socLabel: String = "Unavailable",
    val primaryAbi: String = "",

    // Memory (ActivityManager)
    val ramTotalBytes: Long = 0L,
    val ramAvailBytes: Long = 0L,
    val ramUsedBytes: Long = 0L,
    val ramPercent: Float = 0f,
    val isLowMemory: Boolean = false,
    val ramThresholdBytes: Long = 0L,

    // Storage (StatFs & StorageManager)
    val storageTotalBytes: Long = 0L,
    val storageAvailBytes: Long = 0L,
    val storageUsedBytes: Long = 0L,
    val storagePercent: Float = 0f,
    val appOwnCacheBytes: Long = 0L,
    val allocatableBytes: Long? = null,

    // Battery (BatteryManager & PowerManager)
    val batteryLevel: Int = 0,
    val batteryTempC: Float? = null,
    val batteryStatusLabel: String = "Unknown",
    val batteryPluggedLabel: String = "Battery",
    val batteryHealthLabel: String = "Good",
    val batteryVoltageMv: Int? = null,
    val isCharging: Boolean = false,
    val isPowerSaveMode: Boolean = false,

    // CPU
    val cpuCores: Int = 0,
    val cpuFrequenciesMhz: List<Int?> = emptyList(),
    val cpuMaxFrequenciesMhz: List<Int?> = emptyList(),
    val cpuAvgCurrentMhz: Int? = null,
    val cpuPeakMaxMhz: Int? = null,
    val cpuNormalizedLoadPercent: Float? = null,
    val cpuAccessNote: String = "Checking CPU telemetry...",

    // Thermal & Sensors
    val thermalStatusLabel: String = "Nominal",
    val thermalSeverityCode: Int? = null,
    val ambientTempC: Float? = null,
    val sensorNames: List<String> = emptyList(),

    // Uptime
    val uptimeMillis: Long = 0L,
    val awakeMillis: Long = 0L,

    // Network (TrafficStats & ConnectivityManager)
    val networkTransport: String = "Offline",
    val isNetworkMetered: Boolean = false,
    val rxBytesPerSec: Long? = null,
    val txBytesPerSec: Long? = null,
    val totalRxBytes: Long? = null,
    val totalTxBytes: Long? = null,
    val linkDownKbps: Int? = null,
    val linkUpKbps: Int? = null,

    // Display & System Settings
    val displayRefreshRateHz: Float = 60f,
    val screenBrightness: Int? = null,
    val isAutoBrightness: Boolean = false,
    val windowAnimScale: Float? = null,
    val transitionAnimScale: Float? = null,
    val animatorDurationScale: Float? = null,

    // Capabilities / Permissions state
    val hasDndPolicyAccess: Boolean = false,
    val hasWriteSettingsAccess: Boolean = false,
    val hasPostNotificationsAccess: Boolean = false,
    val systemReducedAnimations: Boolean = false,

    // Transparent Score
    val pulseScore: PulseScoreBreakdown = PulseScoreBreakdown(),
    val updatedAtMillis: Long = 0L
)
