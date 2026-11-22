package com.example.domain.model

enum class GamingProfile(
    val title: String,
    val tagline: String,
    val targetBrightness: Int?,          // 0..255 (only applied if WRITE_SETTINGS granted)
    val enableDoNotDisturb: Boolean,     // only applied if DND policy access granted
    val telemetryIntervalMs: Long,
    val reduceUiEffects: Boolean,
    val supportedBehaviors: List<String>,
    val manualConfirmations: List<String>
) {
    BALANCED(
        title = "BALANCED",
        tagline = "Steady thermals, adaptive brightness & distraction control",
        targetBrightness = 175,
        enableDoNotDisturb = false,
        telemetryIntervalMs = 1500L,
        reduceUiEffects = false,
        supportedBehaviors = listOf(
            "Sets display brightness to ~68% (when Write Settings is granted)",
            "Keeps Do Not Disturb off for normal alerts",
            "Samples live battery temp & RAM every 1.5s during session"
        ),
        manualConfirmations = listOf(
            "Thermal throttling and CPU governors remain OS-managed",
            "Close unused background apps manually via App Info if RAM is low"
        )
    ),
    PERFORMANCE(
        title = "PERFORMANCE",
        tagline = "Peak visibility, Do Not Disturb focus & 0.5s telemetry",
        targetBrightness = 235,
        enableDoNotDisturb = true,
        telemetryIntervalMs = 500L,
        reduceUiEffects = true,
        supportedBehaviors = listOf(
            "Activates Do Not Disturb priority focus (when DND Policy Access is granted)",
            "Raises screen brightness to ~92% (when Write Settings is granted)",
            "Reduces Pulse Mobile overlay animations & polls thermals at 500ms"
        ),
        manualConfirmations = listOf(
            "Does not alter kernel CPU/GPU clocks or guarantee higher FPS",
            "Disable system Battery Saver manually in Battery settings if active"
        )
    ),
    BATTERY_SAVER(
        title = "BATTERY SAVER",
        tagline = "Low-glare brightness, DND silence & minimal polling overhead",
        targetBrightness = 105,
        enableDoNotDisturb = true,
        telemetryIntervalMs = 3000L,
        reduceUiEffects = true,
        supportedBehaviors = listOf(
            "Lowers screen brightness to ~41% to save display power (when permitted)",
            "Enables Do Not Disturb to limit wakeups (when permitted)",
            "Reduces UI animations and slows telemetry polling to 3s"
        ),
        manualConfirmations = listOf(
            "Open Battery Saver settings to enable OS-level power restrictions",
            "Lower screen refresh rate in Display settings if your device supports 120Hz"
        )
    )
}

data class InstalledAppCandidate(
    val packageName: String,
    val appName: String,
    val isCategoryGame: Boolean
)

data class ActiveGamingSession(
    val packageName: String?,
    val appName: String,
    val profile: GamingProfile,
    val startTimeMillis: Long,
    val elapsedMillis: Long,
    val startBatteryLevel: Int,
    val peakBatteryTempC: Float?,
    val ramSamples: List<Float>
)
