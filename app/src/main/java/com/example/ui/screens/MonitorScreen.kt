package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.domain.model.DeviceTelemetry
import com.example.domain.model.TelemetryHistory
import com.example.ui.components.AnimatedLineChart
import com.example.ui.components.DeviceStatusCard
import com.example.ui.components.GlassMetricCard
import com.example.ui.components.StorageProgressBar
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CherryBorder
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.LipstickHighlight
import com.example.ui.theme.MainText
import com.example.ui.theme.RubyRed
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftPinkHighlight
import com.example.util.FormatUtils

@Composable
fun MonitorScreen(
    telemetry: DeviceTelemetry,
    history: TelemetryHistory,
    refreshIntervalMs: Long,
    onSelectRefreshInterval: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val intervals = listOf(500L to "0.5s", 1000L to "1s", 2000L to "2s", 5000L to "5s")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "REAL-TIME MONITOR",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MainText
                )
                Text(
                    text = "Live hardware telemetry sampled directly from Android system services.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
        }

        // Refresh Interval Picker
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REFRESH:",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText
                )
                intervals.forEach { (ms, label) ->
                    FilterChip(
                        selected = refreshIntervalMs == ms,
                        onClick = { onSelectRefreshInterval(ms) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RubyRed,
                            selectedLabelColor = MainText,
                            containerColor = DeepCherry.copy(alpha = 0.35f),
                            labelColor = SecondaryText
                        )
                    )
                }
            }
        }

        item {
            DeviceStatusCard(
                telemetry = telemetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            )
        }

        // Live RAM Chart
        item {
            AnimatedLineChart(
                title = "RAM Utilization (${FormatUtils.formatBytes(telemetry.ramUsedBytes)} / ${FormatUtils.formatBytes(telemetry.ramTotalBytes)})",
                currentValueText = "${telemetry.ramPercent.toInt()}%",
                samples = history.ramPercentHistory,
                maxScale = 100f,
                lineColor = LipstickHighlight,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            )
        }

        // CPU Info & Core Frequencies
        item {
            AnimatedLineChart(
                title = "CPU Frequency Scaling (${telemetry.cpuCores} Cores)",
                currentValueText = telemetry.cpuAvgCurrentMhz?.let { "$it MHz avg" } ?: "OS Restricted",
                samples = history.cpuFreqNormalizedHistory,
                maxScale = 100f,
                lineColor = SoftPinkHighlight,
                unavailableMessage = telemetry.cpuAccessNote,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            )
        }

        item {
            GlassMetricCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                Text(
                    text = "CPU Core Frequencies & Architecture",
                    style = MaterialTheme.typography.titleSmall,
                    color = MainText
                )
                Text(
                    text = telemetry.cpuAccessNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(10.dp))
                val cores = telemetry.cpuFrequenciesMhz
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    cores.chunked(2).forEachIndexed { rowIdx, pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEachIndexed { colIdx, mhz ->
                                val coreIdx = rowIdx * 2 + colIdx
                                val maxMhz = telemetry.cpuMaxFrequenciesMhz.getOrNull(coreIdx)
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DeepCherry.copy(alpha = 0.35f))
                                        .border(1.dp, CherryBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Core $coreIdx",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SecondaryText
                                    )
                                    Text(
                                        text = mhz?.let {
                                            if (maxMhz != null) "$it / $maxMhz MHz" else "$it MHz"
                                        } ?: "Restricted",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (mhz != null) SoftPinkHighlight else SecondaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Network Throughput Chart
        item {
            AnimatedLineChart(
                title = "Network Download (${telemetry.networkTransport}${if (telemetry.isNetworkMetered) " • Metered" else ""})",
                currentValueText = FormatUtils.formatRateBytesPerSec(telemetry.rxBytesPerSec),
                samples = history.netRxKbpsHistory,
                maxScale = 512f,
                lineColor = EmeraldOptimal,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            )
        }

        item {
            GlassMetricCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                Text(
                    text = "Network Counters & Link Capacity",
                    style = MaterialTheme.typography.titleSmall,
                    color = MainText
                )
                Spacer(modifier = Modifier.height(8.dp))
                MonitorKeyValueRow("Upload Speed", FormatUtils.formatRateBytesPerSec(telemetry.txBytesPerSec))
                MonitorKeyValueRow(
                    "Session Rx / Tx",
                    "${telemetry.totalRxBytes?.let { FormatUtils.formatBytes(it) } ?: "Unavailable"} / ${telemetry.totalTxBytes?.let { FormatUtils.formatBytes(it) } ?: "Unavailable"}"
                )
                MonitorKeyValueRow(
                    "Reported Link Down / Up",
                    "${telemetry.linkDownKbps?.let { "${it / 1000} Mbps" } ?: "Unavailable"} / ${telemetry.linkUpKbps?.let { "${it / 1000} Mbps" } ?: "Unavailable"}"
                )
            }
        }

        // Battery Temperature Chart
        item {
            AnimatedLineChart(
                title = "Battery Temperature Trend",
                currentValueText = telemetry.batteryTempC?.let { "${String.format("%.1f", it)}°C" } ?: "Unavailable",
                samples = history.batteryTempHistory,
                minScale = 20f,
                maxScale = 50f,
                lineColor = AmberWarm,
                unavailableMessage = "Battery temperature sensor unavailable on this device",
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            )
        }

        // Storage & Uptime & Sensors
        item {
            GlassMetricCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                StorageProgressBar(
                    label = "Available Storage",
                    usedPercent = telemetry.storagePercent,
                    usedText = FormatUtils.formatBytes(telemetry.storageUsedBytes),
                    totalText = FormatUtils.formatBytes(telemetry.storageTotalBytes)
                )
                Spacer(modifier = Modifier.height(12.dp))
                MonitorKeyValueRow("Free Internal Storage", FormatUtils.formatBytes(telemetry.storageAvailBytes))
                MonitorKeyValueRow("Elapsed Uptime (incl. sleep)", FormatUtils.formatUptime(telemetry.uptimeMillis))
                MonitorKeyValueRow("Awake Uptime", FormatUtils.formatUptime(telemetry.awakeMillis))
                MonitorKeyValueRow(
                    "Ambient Temp Sensor",
                    telemetry.ambientTempC?.let { "${String.format("%.1f", it)}°C" } ?: "Not present on hardware"
                )
            }
        }

        item {
            GlassMetricCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                Text(
                    text = "Detected Hardware Sensors (${telemetry.sensorNames.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MainText
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (telemetry.sensorNames.isEmpty()) {
                    Text(
                        text = "No hardware sensors reported.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                } else {
                    Text(
                        text = telemetry.sensorNames.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun MonitorKeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        Text(text = value, style = MaterialTheme.typography.labelMedium, color = MainText)
    }
}
