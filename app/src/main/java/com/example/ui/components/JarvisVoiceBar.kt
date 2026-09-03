package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisNeonGreen
import com.example.ui.theme.JarvisRedAlert
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun JarvisVoiceBar(
    isListening: Boolean,
    isSpeaking: Boolean,
    isVoiceEnabled: Boolean,
    audioRms: Float,
    partialSpeech: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onToggleVoiceFeedback: () -> Unit,
    onSendCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }

    // Pulsing scale for Mic when listening
    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(isListening, audioRms) {
        if (isListening) {
            val target = 1.1f + (audioRms * 0.35f).coerceIn(0f, 0.4f)
            pulseScale.animateTo(target, tween(100, easing = LinearEasing))
        } else {
            pulseScale.snapTo(1f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color(0xFF030914))
            .border(1.dp, JarvisBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Voice status / live transcript feedback
        AnimatedVisibility(visible = isListening || isSpeaking || partialSpeech.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF09182E))
                    .border(0.8.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isListening) JarvisGold else JarvisCyan)
                    )
                    Text(
                        text = when {
                            partialSpeech.isNotBlank() -> "\"$partialSpeech\""
                            isListening -> "Listening to your voice..."
                            isSpeaking -> "Jarvis vocalizing..."
                            else -> "Processing..."
                        },
                        color = JarvisTextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                if (isListening) {
                    Text(
                        text = "TAP MIC TO STOP",
                        color = JarvisGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Action Input Row: Text Field + Mic Button + Send Button + Voice Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mute / Unmute Voice Toggle
            IconButton(
                onClick = onToggleVoiceFeedback,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(JarvisCardBg)
                    .border(1.dp, JarvisBorder, CircleShape)
                    .testTag("toggle_voice_button")
            ) {
                Icon(
                    imageVector = if (isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Toggle Voice Feedback",
                    tint = if (isVoiceEnabled) JarvisCyan else JarvisTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Command Text Field
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        text = "Speak or type command...",
                        color = JarvisTextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (textInput.isNotBlank()) {
                        onSendCommand(textInput.trim())
                        textInput = ""
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = JarvisTextPrimary,
                    unfocusedTextColor = JarvisTextPrimary,
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = JarvisBorder,
                    cursorColor = JarvisCyan
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("command_text_input")
            )

            // Submit Text Button (if text present) or Arc Mic Button
            if (textInput.isNotBlank()) {
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onSendCommand(textInput.trim())
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(JarvisCyan)
                        .testTag("send_command_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Execute Command",
                        tint = JarvisBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                // Glowing Arc Mic Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(pulseScale.value)
                        .clip(CircleShape)
                        .background(
                            if (isListening) Brush.radialGradient(
                                colors = listOf(JarvisGold, Color(0xFFE65100))
                            ) else Brush.radialGradient(
                                colors = listOf(JarvisCyan, JarvisCyanDark)
                            )
                        )
                        .border(
                            2.dp,
                            if (isListening) JarvisGold else JarvisCyanBright,
                            CircleShape
                        )
                        .clickable {
                            if (isListening) onStopListening() else onStartListening()
                        }
                        .testTag("mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isListening) "Stop Listening" else "Start Voice Command",
                        tint = if (isListening) Color.White else JarvisBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
