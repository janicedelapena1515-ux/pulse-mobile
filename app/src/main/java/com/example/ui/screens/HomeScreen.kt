package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.OptimizationLogEntity
import com.example.domain.model.BoostReport
import com.example.domain.model.DeviceTelemetry
import com.example.ui.components.BatteryIndicator
import com.example.ui.components.GlassMetricCard
import com.example.ui.components.GlossyRedButton
import com.example.ui.components.OptimizationReport
import com.example.ui.components.PulseLogo
import com.example.ui.components.PulseScoreRing
import com.example.ui.components.ScanningRadar
import com.example.ui.components.StorageProgressBar
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CherryBorder
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.GlossyCrimson
import com.example.ui.theme.MainText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftPinkHighlight
import com.example.ui.theme.SubtleGlassBorder
import com.example.util.FormatUtils

@Composable
fun HomeScreen(
    telemetry: DeviceTelemetry,
    isScanning: Boolean,
    scanProgress: Float,
    scanStageMessage: String,
    lastReport: BoostReport?,
    recentLogs: List<OptimizationLogEntity>,
    totalBytesRecovered: Long,
    onBoostClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToBoost: () -> Unit,
    onNavigateToStorageCleaner: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showScoreDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    val bottlenecksCount = telemetry.pulseScore.bottlenecks.size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PulseLogo(showSubtitle = true)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(
                            onClick = { showNotificationsDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DeepCherry.copy(alpha = 0.45f))
                                .border(1.dp, CherryBorder, CircleShape)
                                .testTag("notifications_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = "Device Diagnostic Alerts",
                                tint = MainText
                            )
                        }
                        if (bottlenecksCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(GlossyCrimson)
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DeepCherry.copy(alpha = 0.45f))
                            .border(1.dp, CherryBorder, CircleShape)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = MainText
                        )
                    }
                }
            }
        }

        // Device Identity Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepCherry.copy(alpha = 0.28f))
                    .border(1.dp, SubtleGlassBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = telemetry.deviceName.ifBlank { "Android Device" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MainText
                )
                Text(
                    text = "Android ${telemetry.androidVersion} • ${telemetry.thermalStatusLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftPinkHighlight
                )
            }
        }

        // Hero Pulse Score Ring
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PulseScoreRing(
                    score = telemetry.pulseScore.totalScore,
                    ratingLabel = telemetry.pulseScore.ratingLabel,
                    isScanning = isScanning,
                    onClick = { showScoreDialog = true },
                    modifier = Modifier.testTag("pulse_score_ring")
                )
                Text(
                    text = "Tap gauge for live score breakdown",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
        }

        // Primary Action: BOOST MY DEVICE
        item {
            GlossyRedButton(
                text = if (isScanning) "ANALYZING DEVICE..." else "BOOST MY DEVICE",
                icon = Icons.Rounded.Bolt,
                isLoading = isScanning,
                onClick = onBoostClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .testTag("boost_my_device_button")
            )
        }

        // Animated Scan Radar when running
        item {
            AnimatedVisibility(
                visible = isScanning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                GlassMetricCard(
                    highlighted = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    ScanningRadar(
                        statusText = scanStageMessage,
                        progress = scanProgress
                    )
                }
            }
        }

        // Real Optimization Report when completed
        if (lastReport != null && !isScanning) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OptimizationReport(
                        report = lastReport,
                        initiallyExpanded = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = onNavigateToBoost,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("View All Actions", color = SoftPinkHighlight)
                        }
                        TextButton(
                            onClick = onNavigateToStorageCleaner,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open Storage Cleaner", color = SoftPinkHighlight)
                        }
                    }
                }
            }
        }

        // Live Metrics 2x2 Grid
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // RAM Card
                    GlassMetricCard(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Memory,
                                contentDescription = "RAM",
                                tint = SoftPinkHighlight,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "MEMORY (RAM)",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${telemetry.ramPercent.toInt()}%",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MainText
                        )
                        Text(
                            text = "${FormatUtils.formatBytes(telemetry.ramAvailBytes)} free of ${FormatUtils.formatBytes(telemetry.ramTotalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }

                    // CPU & Thermal Card
                    GlassMetricCard(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = "CPU",
                                tint = SoftPinkHighlight,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "CPU & THERMAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = telemetry.cpuAvgCurrentMhz?.let { "$it MHz" }
                                ?: "${telemetry.cpuCores} Cores",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MainText
                        )
                        Text(
                            text = "${telemetry.socLabel} • ${telemetry.batteryTempC?.let { FormatUtils.formatTempC(it) } ?: telemetry.thermalStatusLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }

                // Storage Card
                GlassMetricCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToStorageCleaner
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Storage,
                                contentDescription = "Storage",
                                tint = SoftPinkHighlight,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "INTERNAL STORAGE",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText
                            )
                        }
                        Text(
                            text = "Clean Storage →",
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftPinkHighlight
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    StorageProgressBar(
                        label = "Used Space",
                        usedPercent = telemetry.storagePercent,
                        usedText = FormatUtils.formatBytes(telemetry.storageUsedBytes),
                        totalText = FormatUtils.formatBytes(telemetry.storageTotalBytes)
                    )
                }

                // Battery Card
                GlassMetricCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BatteryIndicator(
                            level = telemetry.batteryLevel,
                            tempC = telemetry.batteryTempC,
                            statusLabel = "${telemetry.batteryStatusLabel} (${telemetry.batteryPluggedLabel})",
                            isCharging = telemetry.isCharging
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "HEALTH: ${telemetry.batteryHealthLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldOptimal
                            )
                            telemetry.batteryVoltageMv?.let { mv ->
                                Text(
                                    text = "${mv} mV",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SecondaryText
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cumulative Real Recovered Space Summary
        if (recentLogs.isNotEmpty() || totalBytesRecovered > 0L) {
            item {
                GlassMetricCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    onClick = onNavigateToBoost
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "VERIFIED STORAGE RECOVERED",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText
                            )
                            Text(
                                text = FormatUtils.formatBytes(totalBytesRecovered),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = SoftPinkHighlight
                            )
                        }
                        Text(
                            text = "${recentLogs.size} Audit(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = SecondaryText
                        )
                    }
                }
            }
        }
    }

    if (showScoreDialog) {
        val b = telemetry.pulseScore
        AlertDialog(
            onDismissRequest = { showScoreDialog = false },
            containerColor = CardBackground,
            title = {
                Text("Pulse Score Breakdown (${b.totalScore}/100)", color = MainText)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Calculated transparently from live Android system APIs:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    HorizontalDivider(color = SubtleGlassBorder)
                    ScoreRow("Storage Headroom", "${b.storageScore} / 30")
                    ScoreRow("Memory Headroom", "${b.memoryScore} / 25")
                    ScoreRow("Thermal & Battery Health", "${b.thermalBatteryScore} / 25")
                    ScoreRow("System Readiness", "${b.readinessScore} / 20")
                }
            },
            confirmButton = {
                TextButton(onClick = { showScoreDialog = false }) {
                    Text("Close", color = SoftPinkHighlight)
                }
            }
        )
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            containerColor = CardBackground,
            title = {
                Text("Diagnostic Alerts", color = MainText)
            },
            text = {
                val bottlenecks = telemetry.pulseScore.bottlenecks
                if (bottlenecks.isEmpty()) {
                    Text(
                        text = "No thermal, memory, or storage bottlenecks detected right now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmeraldOptimal
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        bottlenecks.forEach { item ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AmberWarm
                                )
                                Text(
                                    text = item.recommendation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("Got It", color = SoftPinkHighlight)
                }
            }
        )
    }
}

@Composable
private fun ScoreRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MainText)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = SoftPinkHighlight)
    }
}
