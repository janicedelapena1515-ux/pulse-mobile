package com.example.data.service

import com.example.domain.model.BoostActionType
import com.example.domain.model.BoostReport
import com.example.domain.model.DeviceTelemetry
import com.example.domain.model.OptimizationCategory
import com.example.domain.model.OptimizationOpportunity
import com.example.domain.model.StorageScanSummary
import com.example.util.FormatUtils

class OptimizationEngine(
    private val telemetryService: DeviceTelemetryService,
    private val storageScannerService: StorageScannerService
) {

    fun buildOpportunities(
        telemetry: DeviceTelemetry,
        scanSummary: StorageScanSummary
    ): List<OptimizationOpportunity> {
        val list = mutableListOf<OptimizationOpportunity>()

        // A. Storage Optimization
        if (telemetry.appOwnCacheBytes > 64L * 1024L) {
            list += OptimizationOpportunity(
                id = "app_cache",
                category = OptimizationCategory.STORAGE,
                title = "Clear Pulse App Cache",
                description = "Safe temporary files created by Pulse Mobile previews and telemetry buffers.",
                impactSummary = "${FormatUtils.formatBytes(telemetry.appOwnCacheBytes)} reclaimable immediately",
                actionLabel = "Clear Cache",
                actionType = BoostActionType.CLEAR_APP_CACHE,
                reclaimableBytes = telemetry.appOwnCacheBytes
            )
        }

        val safReclaimable = scanSummary.duplicateExtraBytes + scanSummary.oldDownloadsBytes
        if (scanSummary.scannedFolderCount == 0) {
            list += OptimizationOpportunity(
                id = "saf_scan_prompt",
                category = OptimizationCategory.STORAGE,
                title = "Analyze User Folders (SAF)",
                description = "Grant access to Downloads or Media folders via Android Storage Access Framework to detect duplicates and large files.",
                impactSummary = "Zero-permission scoped folder audit",
                actionLabel = "Open Cleaner",
                actionType = BoostActionType.OPEN_STORAGE_CLEANER
            )
        } else if (safReclaimable > 0L || scanSummary.largeFilesBytes > 0L) {
            list += OptimizationOpportunity(
                id = "saf_clutter",
                category = OptimizationCategory.STORAGE,
                title = "Review Scanned Duplicates & Old Downloads",
                description = "Found ${scanSummary.duplicateGroupsCount} duplicate group(s) and ${scanSummary.oldDownloadsCount} aged download(s).",
                impactSummary = "${FormatUtils.formatBytes(safReclaimable)} potential recovery",
                actionLabel = "Review Files",
                actionType = BoostActionType.OPEN_STORAGE_CLEANER,
                reclaimableBytes = safReclaimable
            )
        }

        list += OptimizationOpportunity(
            id = "system_storage",
            category = OptimizationCategory.STORAGE,
            title = "System Storage & Other Apps Cache",
            description = "Android sandboxes private caches of third-party apps. Use Android's built-in Storage Manager to clear system-wide app caches safely.",
            impactSummary = "Free storage: ${FormatUtils.formatBytes(telemetry.storageAvailBytes)}",
            actionLabel = "System Storage",
            actionType = BoostActionType.OPEN_SYSTEM_STORAGE
        )

        // B. Memory Optimization (Honest Android LMK explanation)
        val ramPct = telemetry.ramPercent.toInt()
        list += OptimizationOpportunity(
            id = "memory_management",
            category = OptimizationCategory.MEMORY,
            title = "Memory Headroom & Background Activity",
            description = "RAM is ${ramPct}% utilized (${FormatUtils.formatBytes(telemetry.ramAvailBytes)} available). Android manages memory via its kernel Low Memory Killer—third-party apps cannot and should not force-clear other apps' RAM.",
            impactSummary = if (telemetry.isLowMemory) "Low-memory threshold reached" else "Manage heavy background apps in OS settings",
            actionLabel = "App Settings",
            actionType = BoostActionType.OPEN_APP_MANAGEMENT
        )

        // C. Battery Optimization
        val tempStr = telemetry.batteryTempC?.let { FormatUtils.formatTempC(it) } ?: "Unavailable"
        list += OptimizationOpportunity(
            id = "battery_thermal",
            category = OptimizationCategory.BATTERY,
            title = "Battery Thermals & Power Profile",
            description = "Battery is ${telemetry.batteryLevel}% (${telemetry.batteryStatusLabel}), temperature $tempStr. ${if (telemetry.isPowerSaveMode) "Battery Saver is currently active." else "Battery Saver is off."}",
            impactSummary = if ((telemetry.batteryTempC ?: 0f) >= 40f) "Elevated heat — reduce screen brightness" else "Healthy operating envelope",
            actionLabel = "Battery Saver",
            actionType = BoostActionType.OPEN_BATTERY_SAVER
        )

        list += OptimizationOpportunity(
            id = "battery_bg_limits",
            category = OptimizationCategory.BATTERY,
            title = "Per-App Battery Restrictions",
            description = "Restrict background battery usage for non-essential apps through Android's official Battery Optimization settings.",
            impactSummary = "Reduces background wakeups",
            actionLabel = "Battery Limits",
            actionType = BoostActionType.OPEN_BATTERY_OPTIMIZATION
        )

        // D. System Optimization
        val animScale = telemetry.animatorDurationScale
        list += OptimizationOpportunity(
            id = "system_animations",
            category = OptimizationCategory.SYSTEM,
            title = "Window & Animator Scales",
            description = if (animScale != null) {
                "Current system animator scale is ${animScale}x. Reducing scales to 0.5x in Developer Options makes UI transitions snappier."
            } else {
                "Configure system transition and animation scales in Android Developer Options."
            },
            impactSummary = "Display at ${telemetry.displayRefreshRateHz.toInt()} Hz",
            actionLabel = "Display & Scale",
            actionType = BoostActionType.OPEN_DEVELOPER_ANIMATIONS
        )

        list += OptimizationOpportunity(
            id = "display_brightness",
            category = OptimizationCategory.SYSTEM,
            title = "Display Brightness & Refresh Rate",
            description = "Current brightness: ${telemetry.screenBrightness?.let { "${(it * 100 / 255)}%" } ?: "OS-managed"}${if (telemetry.isAutoBrightness) " (Adaptive)" else ""}.",
            impactSummary = "Display is often the #1 battery consumer",
            actionLabel = "Display Settings",
            actionType = BoostActionType.OPEN_DISPLAY_SETTINGS
        )

        list += OptimizationOpportunity(
            id = "network_settings",
            category = OptimizationCategory.SYSTEM,
            title = "Network & Connectivity Audit",
            description = "Active transport: ${telemetry.networkTransport}${if (telemetry.isNetworkMetered) " (Metered)" else ""}. Review Wi-Fi or background data settings.",
            impactSummary = telemetry.linkDownKbps?.let { "Link capacity ~${it / 1000} Mbps" } ?: "Check active connections",
            actionLabel = "Network",
            actionType = BoostActionType.OPEN_NETWORK_SETTINGS
        )

        // E. Performance Analysis Bottlenecks
        for (bottleneck in telemetry.pulseScore.bottlenecks) {
            list += OptimizationOpportunity(
                id = "bottleneck_${bottleneck.id}",
                category = OptimizationCategory.PERFORMANCE,
                title = bottleneck.title,
                description = "${bottleneck.detail} ${bottleneck.recommendation}",
                impactSummary = "Severity: ${bottleneck.severity.name}",
                actionLabel = "Details",
                actionType = BoostActionType.NONE_INFO_ONLY
            )
        }

        return list
    }

    suspend fun runFullDeviceAnalysisAndSafeBoost(
        scanSummary: StorageScanSummary,
        autoClearAppCache: Boolean = true
    ): BoostReport {
        val before = telemetryService.refreshSnapshot(
            scannedClutterBytes = scanSummary.duplicateExtraBytes + scanSummary.oldDownloadsBytes
        )
        val scoreBefore = before.pulseScore.totalScore
        val executedActions = mutableListOf<String>()
        var bytesRecovered = 0L

        if (autoClearAppCache && before.appOwnCacheBytes > 0L) {
            val cleared = storageScannerService.clearAppOwnCache()
            if (cleared > 0L) {
                bytesRecovered += cleared
                executedActions += "Cleared ${FormatUtils.formatBytes(cleared)} from Pulse Mobile's app cache"
            }
        }

        val after = telemetryService.refreshSnapshot(
            scannedClutterBytes = scanSummary.duplicateExtraBytes + scanSummary.oldDownloadsBytes
        )
        val scoreAfter = after.pulseScore.totalScore
        val opportunities = buildOpportunities(after, scanSummary)

        val summaryText = buildString {
            if (bytesRecovered > 0L) {
                append("Reclaimed ${FormatUtils.formatBytes(bytesRecovered)} of safe app cache. ")
            } else {
                append("No unneeded app cache needed clearing. ")
            }
            if (scanSummary.duplicateExtraBytes + scanSummary.oldDownloadsBytes > 0L) {
                append(
                    "Found ${FormatUtils.formatBytes(scanSummary.duplicateExtraBytes + scanSummary.oldDownloadsBytes)} " +
                        "of duplicate/aged files in your scanned folders ready for manual review. "
                )
            }
            append("System RAM (${after.ramPercent.toInt()}% used) and background processes remain managed by Android OS.")
        }

        return BoostReport(
            timestampMillis = System.currentTimeMillis(),
            scoreBefore = scoreBefore,
            scoreAfter = scoreAfter,
            bytesRecovered = bytesRecovered,
            executedActions = if (executedActions.isEmpty()) {
                listOf("Completed live telemetry, thermal, RAM, and storage diagnostics")
            } else {
                executedActions
            },
            recommendations = opportunities,
            honestSummary = summaryText
        )
    }
}
