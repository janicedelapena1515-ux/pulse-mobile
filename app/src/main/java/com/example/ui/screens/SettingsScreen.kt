package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.UserPreferences
import com.example.data.service.SystemIntentsHelper
import com.example.domain.model.DeviceTelemetry
import com.example.ui.components.GlassMetricCard
import com.example.ui.theme.CherryBorder
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.GlossyCrimson
import com.example.ui.theme.MainText
import com.example.ui.theme.RubyRed
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftPinkHighlight

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    telemetry: DeviceTelemetry,
    safFoldersCount: Int,
    onBack: () -> Unit,
    onSetRefreshMs: (Long) -> Unit,
    onSetReducedAnimations: (Boolean) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetLargeFileMb: (Int) -> Unit,
    onSetOldDownloadDays: (Int) -> Unit,
    onSetScheduledHealthCheck: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Schedule worker regardless; worker checks permission before posting notifications
        onSetScheduledHealthCheck(true)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DeepCherry.copy(alpha = 0.45f))
                        .border(1.dp, CherryBorder, CircleShape)
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MainText
                    )
                }
                Column {
                    Text(
                        text = "PREFERENCES & DIAGNOSTICS",
                        style = MaterialTheme.typography.titleLarge,
                        color = MainText
                    )
                    Text(
                        text = "Configure telemetry intervals, storage rules & permissions",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
            }
        }

        // Motion & Haptics
        item {
            GlassMetricCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                Text(
                    text = "EXPERIENCE & ACCESSIBILITY",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftPinkHighlight
                )
                Spacer(modifier = Modifier.height(10.dp))

                ToggleSettingRow(
                    title = "Reduced Animations",
                    subtitle = if (telemetry.systemReducedAnimations) {
                        "OS reduced-motion or Battery Saver is active (auto-respected)"
                    } else {
                        "Minimize animated sweeps and background drift"
                    },
                    checked = preferences.reducedAnimations,
                    onCheckedChange = onSetReducedAnimations
                )

                ToggleSettingRow(
                    title = "Haptic Feedback",
                    subtitle = "Tactile response on buttons and navigation tabs",
                    checked = preferences.hapticsEnabled,
                    onCheckedChange = onSetHaptics
                )

                ToggleSettingRow(
                    title = "Scheduled Background Health Check",
                    subtitle = "WorkManager periodic audit every 12h for low storage or overheating",
                    checked = preferences.scheduledHealthCheckEnabled,
                    onCheckedChange = { enable ->
                        if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !telemetry.hasPostNotificationsAccess) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onSetScheduledHealthCheck(enable)
                        }
                    }
                )
            }
        }

        // Refresh Interval & Storage Thresholds
        item {
            GlassMetricCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                Text(
                    text = "MONITORING & CLEANER THRESHOLDS",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftPinkHighlight
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text("Default Telemetry Refresh", style = MaterialTheme.typography.titleSmall, color = MainText)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(500L to "0.5s", 1000L to "1s", 2000L to "2s", 5000L to "5s").forEach { (ms, label) ->
                        FilterChip(
                            selected = preferences.monitorRefreshMs == ms,
                            onClick = { onSetRefreshMs(ms) },
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

                Spacer(modifier = Modifier.height(10.dp))
                Text("Large File Threshold", style = MaterialTheme.typography.titleSmall, color = MainText)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 25, 50, 100).forEach { mb ->
                        FilterChip(
                            selected = preferences.largeFileThresholdMb == mb,
                            onClick = { onSetLargeFileMb(mb) },
                            label = { Text("$mb MB") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RubyRed,
                                selectedLabelColor = MainText,
                                containerColor = DeepCherry.copy(alpha = 0.35f),
                                labelColor = SecondaryText
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Old Downloads Threshold", style = MaterialTheme.typography.titleSmall, color = MainText)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(14, 30, 60, 90).forEach { days ->
                        FilterChip(
                            selected = preferences.oldDownloadDays == days,
                            onClick = { onSetOldDownloadDays(days) },
                            label = { Text("$days days") },
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
        }

        // Permissions & Hardware Capabilities Diagnostics
        item {
            GlassMetricCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                Text(
                    text = "OS CAPABILITIES & PERMISSIONS",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftPinkHighlight
                )
                Spacer(modifier = Modifier.height(10.dp))

                CapabilityStatusRow(
                    title = "Do Not Disturb Policy Access",
                    granted = telemetry.hasDndPolicyAccess,
                    actionLabel = "Configure",
                    onAction = {
                        if (!SystemIntentsHelper.openDndPolicyAccessSettings(context)) {
                            android.widget.Toast.makeText(
                                context,
                                "DND Policy settings unavailable on this Android build.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                CapabilityStatusRow(
                    title = "Modify System Settings (Brightness)",
                    granted = telemetry.hasWriteSettingsAccess,
                    actionLabel = "Configure",
                    onAction = {
                        if (!SystemIntentsHelper.openWriteSettingsAccess(context)) {
                            android.widget.Toast.makeText(
                                context,
                                "Modify System Settings screen unavailable on this Android build.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                CapabilityStatusRow(
                    title = "SAF Scoped Folders Granted",
                    granted = safFoldersCount > 0,
                    statusOverride = "$safFoldersCount folder(s)"
                )
                CapabilityStatusRow(
                    title = "CPU Sysfs Frequency Access",
                    granted = telemetry.cpuAvgCurrentMhz != null,
                    statusOverride = if (telemetry.cpuAvgCurrentMhz != null) "Readable" else "OS-Restricted"
                )
                CapabilityStatusRow(
                    title = "Ambient Temperature Sensor",
                    granted = telemetry.ambientTempC != null,
                    statusOverride = if (telemetry.ambientTempC != null) "Available" else "Not on Hardware"
                )
            }
        }
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = MainText)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MainText,
                checkedTrackColor = GlossyCrimson,
                uncheckedThumbColor = SecondaryText,
                uncheckedTrackColor = DeepCherry.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun CapabilityStatusRow(
    title: String,
    granted: Boolean,
    statusOverride: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MainText)
            Text(
                text = statusOverride ?: if (granted) "Granted" else "Not Granted",
                style = MaterialTheme.typography.labelSmall,
                color = if (granted) EmeraldOptimal else SecondaryText
            )
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = SoftPinkHighlight)
            }
        }
    }
}
