package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisNeonGreen
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary

@Composable
fun QuickActionGrid(
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onOpenApp: (String) -> Unit,
    onOpenSetting: (String) -> Unit,
    onAddReminderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "DIRECT PHONE CONTROLS",
            color = JarvisTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Row 1: Torch, Phone Call, WhatsApp, Reminder
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_action_torch"),
                icon = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                label = if (isTorchOn) "TORCH ON" else "TORCH OFF",
                accentColor = if (isTorchOn) JarvisGold else JarvisCyan,
                isActive = isTorchOn,
                onClick = onToggleTorch
            )

            ActionTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_action_call"),
                icon = Icons.Default.Phone,
                label = "CALL",
                accentColor = JarvisNeonGreen,
                onClick = onCallClick
            )

            ActionTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_action_whatsapp"),
                icon = Icons.Default.Chat,
                label = "WHATSAPP",
                accentColor = Color(0xFF25D366),
                onClick = onWhatsAppClick
            )

            ActionTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_action_reminder"),
                icon = Icons.Default.AddAlert,
                label = "SCHEDULE",
                accentColor = JarvisGold,
                onClick = onAddReminderClick
            )
        }

        // Row 2: YouTube, Camera, Wi-Fi, Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_action_youtube"),
                icon = Icons.Default.PlayArrow,
                label = "YOUTUBE",
                accentColor = Color(0xFFFF0033),
                onClick = { onOpenApp("youtube") }
            )

            ActionTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_action_camera"),
                icon = Icons.Default.CameraAlt,
                label = "CAMERA",
                accentColor = JarvisCyanBright,
                onClick = { onOpenApp("camera") }
            )

            ActionTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_action_wifi"),
                icon = Icons.Default.Wifi,
                label = "WI-FI",
                accentColor = JarvisCyan,
                onClick = { onOpenSetting("WIFI") }
            )

            ActionTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_action_settings"),
                icon = Icons.Default.Settings,
                label = "SETTINGS",
                accentColor = JarvisTextPrimary,
                onClick = { onOpenSetting("GENERAL") }
            )
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) accentColor.copy(alpha = 0.2f) else JarvisCardBg)
            .border(
                1.dp,
                if (isActive) accentColor else JarvisBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                color = if (isActive) accentColor else JarvisTextPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}
