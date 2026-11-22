package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.domain.model.DeviceTelemetry
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CherryBorder
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.MainText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftPinkHighlight
import com.example.util.FormatUtils

@Composable
fun DeviceStatusCard(
    telemetry: DeviceTelemetry,
    modifier: Modifier = Modifier
) {
    GlassMetricCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DeepCherry.copy(alpha = 0.7f))
                        .border(1.dp, CherryBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhoneAndroid,
                        contentDescription = "Device Hardware",
                        tint = SoftPinkHighlight,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = telemetry.deviceName.ifBlank { "Android Device" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MainText
                    )
                    Text(
                        text = "Android ${telemetry.androidVersion} • ${telemetry.primaryAbi} • ${telemetry.displayRefreshRateHz.toInt()} Hz",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
            }

            val thermalColor = when {
                telemetry.thermalStatusLabel.contains("Severe", true) ||
                    telemetry.thermalStatusLabel.contains("Critical", true) -> AmberWarm
                telemetry.thermalStatusLabel.contains("Warm", true) ||
                    telemetry.thermalStatusLabel.contains("Moderate", true) -> AmberWarm
                else -> EmeraldOptimal
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(thermalColor.copy(alpha = 0.14f))
                    .border(1.dp, thermalColor.copy(alpha = 0.45f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = telemetry.thermalStatusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = thermalColor
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Memory,
                    contentDescription = null,
                    tint = SecondaryText,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "SoC: ${telemetry.socLabel} (${telemetry.cpuCores} cores)",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = SecondaryText,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = FormatUtils.formatUptime(telemetry.uptimeMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftPinkHighlight
                )
            }
        }
    }
}
