package com.example.domain.model

enum class OptimizationCategory(val label: String) {
    STORAGE("Storage"),
    MEMORY("Memory"),
    BATTERY("Battery"),
    SYSTEM("System"),
    PERFORMANCE("Performance")
}

enum class BoostActionType {
    CLEAR_APP_CACHE,
    OPEN_STORAGE_CLEANER,
    OPEN_SYSTEM_STORAGE,
    OPEN_APP_MANAGEMENT,
    OPEN_BATTERY_SAVER,
    OPEN_BATTERY_OPTIMIZATION,
    OPEN_DISPLAY_SETTINGS,
    OPEN_DEVELOPER_ANIMATIONS,
    OPEN_NETWORK_SETTINGS,
    NONE_INFO_ONLY
}

data class OptimizationOpportunity(
    val id: String,
    val category: OptimizationCategory,
    val title: String,
    val description: String,
    val impactSummary: String,
    val actionLabel: String,
    val actionType: BoostActionType,
    val reclaimableBytes: Long = 0L,
    val isResolved: Boolean = false
)

data class BoostReport(
    val timestampMillis: Long,
    val scoreBefore: Int,
    val scoreAfter: Int,
    val bytesRecovered: Long,
    val executedActions: List<String>,
    val recommendations: List<OptimizationOpportunity>,
    val honestSummary: String
)
