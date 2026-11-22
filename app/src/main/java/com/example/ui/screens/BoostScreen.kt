package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.example.data.local.OptimizationLogEntity
import com.example.domain.model.BoostActionType
import com.example.domain.model.BoostReport
import com.example.domain.model.DeviceTelemetry
import com.example.domain.model.OptimizationCategory
import com.example.domain.model.OptimizationOpportunity
import com.example.ui.components.GlassMetricCard
import com.example.ui.components.GlossyRedButton
import com.example.ui.components.OptimizationReport
import com.example.ui.components.ScanningRadar
import com.example.ui.theme.CherryBorder
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.GlossyCrimson
import com.example.ui.theme.MainText
import com.example.ui.theme.RubyRed
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftPinkHighlight
import com.example.util.FormatUtils

@Composable
fun BoostScreen(
    telemetry: DeviceTelemetry,
    opportunities: List<OptimizationOpportunity>,
    isScanning: Boolean,
    scanProgress: Float,
    scanStageMessage: String,
    lastReport: BoostReport?,
    logs: List<OptimizationLogEntity>,
    onRunFullBoost: () -> Unit,
    onExecuteOpportunity: (OptimizationOpportunity) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<OptimizationCategory?>(null) }

    val filtered = remember(opportunities, selectedCategory) {
        if (selectedCategory == null) opportunities
        else opportunities.filter { it.category == selectedCategory }
    }

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
                    text = "REAL BOOST ENGINE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MainText
                )
                Text(
                    text = "Genuine Android-supported storage, memory, battery, system & bottleneck optimization.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
        }

        item {
            GlossyRedButton(
                text = if (isScanning) "RUNNING DIAGNOSTICS..." else "RUN FULL SYSTEM AUDIT",
                icon = Icons.Rounded.Bolt,
                isLoading = isScanning,
                onClick = onRunFullBoost,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .testTag("boost_screen_audit_button")
            )
        }

        if (isScanning) {
            item {
                GlassMetricCard(
                    highlighted = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    ScanningRadar(statusText = scanStageMessage, progress = scanProgress)
                }
            }
        } else if (lastReport != null) {
            item {
                OptimizationReport(
                    report = lastReport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    initiallyExpanded = false
                )
            }
        }

        // Category Filter Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All (${opportunities.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RubyRed,
                        selectedLabelColor = MainText,
                        containerColor = DeepCherry.copy(alpha = 0.35f),
                        labelColor = SecondaryText
                    )
                )
                OptimizationCategory.entries.forEach { cat ->
                    val count = opportunities.count { it.category == cat }
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text("${cat.label} ($count)") },
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

        // Honest Android Memory Card
        if (selectedCategory == null || selectedCategory == OptimizationCategory.MEMORY) {
            item {
                GlassMetricCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = SoftPinkHighlight
                        )
                        Text(
                            text = "How Android Manages RAM",
                            style = MaterialTheme.typography.titleMedium,
                            color = MainText
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Used: ${FormatUtils.formatBytes(telemetry.ramUsedBytes)} • Available: ${FormatUtils.formatBytes(telemetry.ramAvailBytes)} • Low-Memory Threshold: ${FormatUtils.formatBytes(telemetry.ramThresholdBytes)}. Android keeps recently used apps cached in RAM so they reopen quickly without extra battery drain. Pulse Mobile never force-kills other apps or displays fake \"RAM freed\" counters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
            }
        }

        // Opportunities List
        items(filtered, key = { it.id }) { item ->
            OpportunityCard(
                item = item,
                onAction = { onExecuteOpportunity(item) },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            )
        }

        // History Section
        item {
            GlassMetricCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
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
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = SoftPinkHighlight
                        )
                        Text(
                            text = "Optimization Audit Log",
                            style = MaterialTheme.typography.titleMedium,
                            color = MainText
                        )
                    }
                    if (logs.isNotEmpty()) {
                        TextButton(onClick = onClearLogs) {
                            Text("Clear", color = SoftPinkHighlight)
                        }
                    }
                }

                if (logs.isEmpty()) {
                    Text(
                        text = "Run a full system audit or clean files in Smart Storage Cleaner to record verified optimization history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        logs.take(8).forEach { log ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DeepCherry.copy(alpha = 0.25f))
                                    .border(1.dp, CherryBorder, RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = FormatUtils.formatDateTime(log.timestampMillis),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SoftPinkHighlight
                                    )
                                    Text(
                                        text = if (log.bytesRecovered > 0) {
                                            "+${FormatUtils.formatBytes(log.bytesRecovered)} freed"
                                        } else {
                                            "Score ${log.scoreAfter}/100"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MainText
                                    )
                                }
                                Text(
                                    text = log.actionsSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MainText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpportunityCard(
    item: OptimizationOpportunity,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember(item.id) { mutableStateOf(false) }

    val categoryIcon = when (item.category) {
        OptimizationCategory.STORAGE -> Icons.Rounded.Storage
        OptimizationCategory.MEMORY -> Icons.Rounded.Memory
        OptimizationCategory.BATTERY -> Icons.Rounded.BatteryChargingFull
        OptimizationCategory.SYSTEM -> Icons.Rounded.SettingsSuggest
        OptimizationCategory.PERFORMANCE -> Icons.Rounded.Speed
    }

    GlassMetricCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeepCherry.copy(alpha = 0.6f))
                        .border(1.dp, CherryBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = item.category.label,
                        tint = SoftPinkHighlight,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MainText
                    )
                    Text(
                        text = item.impactSummary,
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftPinkHighlight
                    )
                }
            }

            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = SecondaryText
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )

                if (item.actionType != BoostActionType.NONE_INFO_ONLY) {
                    FilledTonalButton(
                        onClick = onAction,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = GlossyCrimson.copy(alpha = 0.28f),
                            contentColor = MainText
                        ),
                        modifier = Modifier.testTag("opp_action_${item.id}")
                    ) {
                        Icon(
                            imageVector = if (item.actionType == BoostActionType.CLEAR_APP_CACHE) {
                                Icons.Rounded.DeleteSweep
                            } else {
                                Icons.AutoMirrored.Rounded.OpenInNew
                            },
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(17.dp)
                        )
                        Text(item.actionLabel)
                    }
                }
            }
        }
    }
}
