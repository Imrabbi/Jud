package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SystemInfo
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisNeonGreen
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun SystemHud(
    systemInfo: SystemInfo,
    isListening: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(JarvisCardBg.copy(alpha = 0.85f))
            .border(1.dp, JarvisBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Protocol status & time indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isListening || isSpeaking) JarvisGold else JarvisNeonGreen)
                    )
                    Text(
                        text = if (isListening) "LISTENING FOR COMMANDS" else if (isSpeaking) "JARVIS VOCALIZING" else "JARVIS PROTOCOL ACTIVE",
                        color = if (isListening || isSpeaking) JarvisGold else JarvisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Network",
                        tint = JarvisCyan.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "SECURE",
                        color = JarvisTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Metrics row: Battery, RAM, Storage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Battery metric
                MetricBox(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Bolt,
                    label = "BATTERY",
                    value = "${systemInfo.batteryPercentage}%",
                    extra = if (systemInfo.isCharging) "CHARGING" else "DRAIN STABLE",
                    accentColor = if (systemInfo.batteryPercentage > 20) JarvisCyan else JarvisGold,
                    progress = (systemInfo.batteryPercentage / 100f).coerceIn(0f, 1f)
                )

                // RAM metric
                MetricBox(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Memory,
                    label = "MEMORY",
                    value = "${systemInfo.ramPercent}%",
                    extra = "${systemInfo.ramUsedMb} MB",
                    accentColor = JarvisNeonGreen,
                    progress = (systemInfo.ramPercent / 100f).coerceIn(0f, 1f)
                )

                // Storage metric
                MetricBox(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Storage,
                    label = "STORAGE",
                    value = "%.1f GB".format(systemInfo.internalStorageFreeGb),
                    extra = "AVAILABLE",
                    accentColor = JarvisGold,
                    progress = 0.65f
                )
            }
        }
    }
}

@Composable
private fun MetricBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    extra: String,
    accentColor: Color,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF071220))
            .border(0.8.dp, JarvisBorder.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = label,
                    color = JarvisTextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                color = JarvisTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = extra,
                color = accentColor.copy(alpha = 0.8f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accentColor,
                trackColor = Color(0xFF132338)
            )
        }
    }
}
