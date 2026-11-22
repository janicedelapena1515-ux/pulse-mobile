package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.service.SystemIntentsHelper
import com.example.domain.model.DeviceTelemetry
import com.example.domain.model.InspectedMediaInfo
import com.example.domain.model.StorageScanSummary
import com.example.ui.components.GlassMetricCard
import com.example.ui.components.GlossyRedButton
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CherryBorder
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.MainText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftPinkHighlight
import com.example.util.FormatUtils

@Composable
fun ToolsScreen(
    telemetry: DeviceTelemetry,
    scanSummary: StorageScanSummary,
    totalRecoveredBytes: Long,
    inspectedMedia: InspectedMediaInfo?,
    onOpenStorageCleaner: () -> Unit,
    onOpenSettings: () -> Unit,
    onInspectPhotoPickerUri: (android.net.Uri) -> Unit,
    onDismissInspectedMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onInspectPhotoPickerUri(uri)
        }
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
                    text = "OPTIMIZATION TOOLS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MainText
                )
                Text(
                    text = "Smart SAF storage suite, Photo Picker inspector, and system configuration shortcuts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
        }

        // Featured Smart Storage Cleaner Card
        item {
            GlassMetricCard(
                highlighted = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(DeepCherry)
                            .border(1.dp, CherryBorder, RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CleaningServices,
                            contentDescription = "Smart Storage Cleaner",
                            tint = SoftPinkHighlight,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart Storage Cleaner",
                            style = MaterialTheme.typography.titleLarge,
                            color = MainText
                        )
                        Text(
                            text = "${scanSummary.scannedFolderCount} SAF folder(s) • ${FormatUtils.formatBytes(totalRecoveredBytes)} recovered",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftPinkHighlight
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Inspect Images, Videos, Audio, Documents, Downloads, Large Files & SHA-256 Duplicates using Android's Storage Access Framework.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(14.dp))
                GlossyRedButton(
                    text = "OPEN STORAGE CLEANER",
                    icon = Icons.Rounded.Storage,
                    onClick = onOpenStorageCleaner,
                    modifier = Modifier.testTag("open_storage_cleaner_button")
                )
            }
        }

        // Zero-Permission Photo Picker Media Inspector
        item {
            ToolShortcutCard(
                icon = Icons.Rounded.PhotoLibrary,
                title = "Photo Picker Media Inspector",
                subtitle = "Inspect image or video size and MIME metadata with zero storage permissions",
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .testTag("photo_picker_inspector_card")
            )
        }

        item {
            Text(
                text = "SYSTEM OPTIMIZATION SHORTCUTS",
                style = MaterialTheme.typography.labelLarge,
                color = SoftPinkHighlight,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            )
        }

        item {
            val launchSetting: (Boolean, String) -> Unit = { launched, label ->
                if (!launched) {
                    android.widget.Toast.makeText(
                        context,
                        "$label settings unavailable on this Android build.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ToolShortcutCard(
                    icon = Icons.Rounded.Animation,
                    title = "Animation Scale Settings",
                    subtitle = telemetry.animatorDurationScale?.let {
                        "Current animator scale: ${it}x • Opens Developer Options"
                    } ?: "Configure Window & Animator transition scales",
                    onClick = { launchSetting(SystemIntentsHelper.openDeveloperAnimationSettings(context), "Developer / Animation") }
                )
                ToolShortcutCard(
                    icon = Icons.Rounded.DisplaySettings,
                    title = "Display & Refresh Rate",
                    subtitle = "${telemetry.displayRefreshRateHz.toInt()} Hz active • Brightness & adaptive display",
                    onClick = { launchSetting(SystemIntentsHelper.openDisplaySettings(context), "Display") }
                )
                ToolShortcutCard(
                    icon = Icons.Rounded.Apps,
                    title = "App Management & Background Limits",
                    subtitle = "Inspect installed apps, permissions, and background activity",
                    onClick = { launchSetting(SystemIntentsHelper.openAppManagementSettings(context), "App Management") }
                )
                ToolShortcutCard(
                    icon = Icons.Rounded.Storage,
                    title = "Android System Storage Manager",
                    subtitle = "Free: ${FormatUtils.formatBytes(telemetry.storageAvailBytes)} • Manage OS & third-party caches",
                    onClick = { launchSetting(SystemIntentsHelper.openStorageSettings(context), "System Storage") }
                )
                ToolShortcutCard(
                    icon = Icons.Rounded.BatterySaver,
                    title = "Battery Saver & Optimization",
                    subtitle = if (telemetry.isPowerSaveMode) "Battery Saver is ON" else "Configure power profiles & app restrictions",
                    onClick = { launchSetting(SystemIntentsHelper.openBatterySaverSettings(context), "Battery Saver") }
                )
                ToolShortcutCard(
                    icon = Icons.Rounded.Wifi,
                    title = "Network & Wi-Fi Settings",
                    subtitle = "Active: ${telemetry.networkTransport}${if (telemetry.isNetworkMetered) " (Metered)" else ""}",
                    onClick = { launchSetting(SystemIntentsHelper.openNetworkSettings(context), "Network") }
                )
                ToolShortcutCard(
                    icon = Icons.Rounded.DoNotDisturbOn,
                    title = "Do Not Disturb Policy Access",
                    subtitle = if (telemetry.hasDndPolicyAccess) "Granted for Gaming Profiles" else "Allow Pulse Mobile to toggle DND during gaming",
                    onClick = { launchSetting(SystemIntentsHelper.openDndPolicyAccessSettings(context), "DND Policy") }
                )
                ToolShortcutCard(
                    icon = Icons.Rounded.Tune,
                    title = "Modify System Settings (Brightness)",
                    subtitle = if (telemetry.hasWriteSettingsAccess) "Granted for Gaming Profiles" else "Allow brightness adjustment for Gaming Profiles",
                    onClick = { launchSetting(SystemIntentsHelper.openWriteSettingsAccess(context), "Modify System Settings") }
                )
                ToolShortcutCard(
                    icon = Icons.Rounded.Settings,
                    title = "Pulse Mobile Preferences & Diagnostics",
                    subtitle = "Thresholds, refresh rate, WorkManager checks & sensor status",
                    onClick = onOpenSettings
                )
            }
        }
    }

    if (inspectedMedia != null) {
        AlertDialog(
            onDismissRequest = onDismissInspectedMedia,
            containerColor = CardBackground,
            title = { Text("Media Metadata", color = MainText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Name: ${inspectedMedia.displayName}", color = MainText)
                    Text("Size: ${FormatUtils.formatBytes(inspectedMedia.sizeBytes)}", color = SoftPinkHighlight)
                    Text("MIME: ${inspectedMedia.mimeType}", color = SecondaryText)
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissInspectedMedia) {
                    Text("Close", color = SoftPinkHighlight)
                }
            }
        )
    }
}

@Composable
private fun ToolShortcutCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassMetricCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DeepCherry.copy(alpha = 0.55f))
                    .border(1.dp, CherryBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = SoftPinkHighlight,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = MainText)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = SecondaryText
            )
        }
    }
}
