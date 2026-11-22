package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.GameAppEntity
import com.example.data.local.GamingSessionEntity
import com.example.data.service.SystemIntentsHelper
import com.example.domain.model.ActiveGamingSession
import com.example.domain.model.DeviceTelemetry
import com.example.domain.model.GamingProfile
import com.example.domain.model.InstalledAppCandidate
import com.example.ui.components.GamingProfileCard
import com.example.ui.components.GlassMetricCard
import com.example.ui.components.GlossyRedButton
import com.example.ui.components.ShimmerBox
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CherryBorder
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.GlossyCrimson
import com.example.ui.theme.MainText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftPinkHighlight
import com.example.util.FormatUtils

@Composable
fun GamesScreen(
    telemetry: DeviceTelemetry,
    games: List<GameAppEntity>,
    sessions: List<GamingSessionEntity>,
    selectedProfile: GamingProfile,
    activeSession: ActiveGamingSession?,
    installedCandidates: List<InstalledAppCandidate>,
    isLoadingApps: Boolean,
    onSelectProfile: (GamingProfile) -> Unit,
    onStartSession: (GameAppEntity?, Boolean) -> Unit,
    onStopSession: () -> Unit,
    onLoadInstalledApps: () -> Unit,
    onAddGame: (InstalledAppCandidate) -> Unit,
    onRemoveGame: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddGameDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

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
                    text = "GAMING BOOST",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MainText
                )
                Text(
                    text = "Profile presets, distraction control, thermal telemetry & session tracking.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
        }

        // Active Session Monitor Card
        item {
            GlassMetricCard(
                highlighted = activeSession != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                if (activeSession == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gaming Session Timer",
                                style = MaterialTheme.typography.titleMedium,
                                color = MainText
                            )
                            Text(
                                text = "Monitors live RAM, battery temp & network during play.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = SoftPinkHighlight,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    GlossyRedButton(
                        text = "START TIMED SESSION (${selectedProfile.title})",
                        icon = Icons.Rounded.PlayArrow,
                        onClick = { onStartSession(null, false) },
                        modifier = Modifier.testTag("start_gaming_session_button")
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ACTIVE: ${activeSession.appName}",
                                style = MaterialTheme.typography.titleSmall,
                                color = SoftPinkHighlight
                            )
                            Text(
                                text = FormatUtils.formatSessionTimer(activeSession.elapsedMillis),
                                style = MaterialTheme.typography.displayMedium,
                                color = MainText
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(EmeraldOptimal.copy(alpha = 0.16f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = activeSession.profile.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldOptimal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricMiniPill("RAM", "${telemetry.ramPercent.toInt()}%")
                        MetricMiniPill(
                            "TEMP",
                            telemetry.batteryTempC?.let { FormatUtils.formatTempC(it) } ?: "N/A"
                        )
                        MetricMiniPill("NET", telemetry.networkTransport)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    GlossyRedButton(
                        text = "STOP & SAVE SESSION",
                        icon = Icons.Rounded.Stop,
                        onClick = onStopSession,
                        modifier = Modifier.testTag("stop_gaming_session_button")
                    )
                }
            }
        }

        // System Shortcuts: Do Not Disturb & Brightness
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassMetricCard(
                    modifier = Modifier.weight(1f),
                    onClick = { SystemIntentsHelper.openDndPolicyAccessSettings(context) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DoNotDisturbOn,
                            contentDescription = "Do Not Disturb",
                            tint = SoftPinkHighlight
                        )
                        Text(
                            text = "Do Not Disturb",
                            style = MaterialTheme.typography.titleSmall,
                            color = MainText
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (telemetry.hasDndPolicyAccess) "Access granted" else "Tap to grant access",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (telemetry.hasDndPolicyAccess) EmeraldOptimal else SecondaryText
                    )
                }

                GlassMetricCard(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (telemetry.hasWriteSettingsAccess) {
                            SystemIntentsHelper.openDisplaySettings(context)
                        } else {
                            SystemIntentsHelper.openWriteSettingsAccess(context)
                        }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Brightness6,
                            contentDescription = "Brightness",
                            tint = SoftPinkHighlight
                        )
                        Text(
                            text = "Brightness",
                            style = MaterialTheme.typography.titleSmall,
                            color = MainText
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val brightText = telemetry.screenBrightness?.let { "${(it * 100 / 255)}%" } ?: "Display Settings"
                    Text(
                        text = if (telemetry.hasWriteSettingsAccess) "$brightText • Auto-tune ready" else "$brightText • Tap to allow",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (telemetry.hasWriteSettingsAccess) EmeraldOptimal else SecondaryText
                    )
                }
            }
        }

        // Three Gaming Profiles
        item {
            Text(
                text = "GAMING PROFILES",
                style = MaterialTheme.typography.labelLarge,
                color = SoftPinkHighlight,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            )
        }

        items(GamingProfile.entries) { profile ->
            GamingProfileCard(
                profile = profile,
                isSelected = selectedProfile == profile,
                onSelect = { onSelectProfile(profile) },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            )
        }

        // Game Library Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GAME LIBRARY (${games.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftPinkHighlight
                )
                FilledTonalButton(
                    onClick = {
                        onLoadInstalledApps()
                        showAddGameDialog = true
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = GlossyCrimson.copy(alpha = 0.30f),
                        contentColor = MainText
                    ),
                    modifier = Modifier.testTag("add_game_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Game",
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(18.dp)
                    )
                    Text("Add Game")
                }
            }
        }

        if (games.isEmpty()) {
            item {
                GlassMetricCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = SoftPinkHighlight,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No games added yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = MainText
                        )
                        Text(
                            text = "Tap Add Game to pick installed games or apps from your device for one-tap boost & launch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }
            }
        } else {
            items(games, key = { it.packageName }) { game ->
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = game.appName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MainText
                            )
                            Text(
                                text = "Profile: ${game.preferredProfile} • Played: ${FormatUtils.formatUptime(game.totalPlayMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(
                                onClick = { onStartSession(game, true) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = GlossyCrimson.copy(alpha = 0.32f),
                                    contentColor = MainText
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Launch ${game.appName}",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Launch")
                            }
                            IconButton(
                                onClick = { onRemoveGame(game.packageName) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove ${game.appName}",
                                    tint = SecondaryText
                                )
                            }
                        }
                    }
                }
            }
        }

        // Session Statistics
        item {
            GlassMetricCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                val totalPlayMs = sessions.sumOf { it.durationMillis }
                val peakTemp = sessions.mapNotNull { it.peakBatteryTempC }.maxOrNull()
                Text(
                    text = "GAMING SESSION STATISTICS",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftPinkHighlight
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricMiniPill("SESSIONS", "${sessions.size}")
                    MetricMiniPill("TOTAL TIME", FormatUtils.formatUptime(totalPlayMs))
                    MetricMiniPill("PEAK TEMP", peakTemp?.let { FormatUtils.formatTempC(it) } ?: "N/A")
                }

                if (sessions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    sessions.take(5).forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${s.appName} (${s.profileUsed})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MainText,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${FormatUtils.formatSessionTimer(s.durationMillis)} • ${s.avgRamPercent.toInt()}% RAM",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftPinkHighlight
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddGameDialog) {
        val addedPkgs = remember(games) { games.map { it.packageName }.toSet() }
        val filteredApps = remember(installedCandidates, searchQuery, addedPkgs) {
            installedCandidates.filter {
                it.packageName !in addedPkgs &&
                    (searchQuery.isBlank() || it.appName.contains(searchQuery, ignoreCase = true))
            }
        }

        AlertDialog(
            onDismissRequest = { showAddGameDialog = false },
            containerColor = CardBackground,
            title = { Text("Add Installed Game / App", color = MainText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Filter installed apps") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isLoadingApps) {
                        ShimmerBox(height = 48.dp)
                        ShimmerBox(height = 48.dp)
                    } else if (filteredApps.isEmpty()) {
                        Text(
                            text = "No matching launchable apps found.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DeepCherry.copy(alpha = 0.35f))
                                        .clickable {
                                            onAddGame(app)
                                            showAddGameDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MainText
                                        )
                                        if (app.isCategoryGame) {
                                            Text(
                                                text = "Detected Android Game",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldOptimal
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Add",
                                        tint = SoftPinkHighlight
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddGameDialog = false }) {
                    Text("Close", color = SoftPinkHighlight)
                }
            }
        )
    }
}

@Composable
private fun MetricMiniPill(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DeepCherry.copy(alpha = 0.38f))
            .border(1.dp, CherryBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = SecondaryText)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = MainText)
    }
}
