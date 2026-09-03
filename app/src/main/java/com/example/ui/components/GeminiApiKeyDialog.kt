package com.example.ui.components

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisNeonGreen
import com.example.ui.theme.JarvisRedAlert
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import kotlinx.coroutines.launch

@Composable
fun GeminiApiKeyDialog(
    currentKey: String?,
    assistantName: String,
    onSaveKey: (String) -> Boolean,
    onClearKey: () -> Unit,
    onTestConnection: suspend (String?) -> Result<String>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var inputKey by remember { mutableStateOf(currentKey ?: "") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var showPasteSection by remember { mutableStateOf(!currentKey.isNullOrBlank()) }
    var testStatus by remember { mutableStateOf<TestResultState?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    val hasKey = !currentKey.isNullOrBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(JarvisCyan, JarvisBorder, JarvisCyan.copy(alpha = 0.3f))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .background(JarvisBackground)
                .testTag("gemini_api_key_dialog"),
            color = JarvisBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(JarvisCyan.copy(alpha = 0.15f))
                                .border(1.dp, JarvisCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Gemini Key",
                                tint = JarvisCyanBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Jarvis needs a Gemini API key",
                                color = JarvisTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "GOOGLE AI STUDIO INTEGRATION",
                                color = JarvisCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("dialog_close_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = JarvisTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Core Explanation Callout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(JarvisCardBg)
                        .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Jarvis's voice and brain run on Google's Gemini.",
                            color = JarvisCyanBright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Without a key she cannot hear you or answer. The key is free and takes about a minute to get.",
                            color = JarvisTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                // 4-Step Walkthrough Guide
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StepRow(
                        stepNumber = "1",
                        instruction = "Open Google AI Studio and sign in with any Google account."
                    )
                    StepRow(
                        stepNumber = "2",
                        instruction = "Tap \"Create API key\", pick any project, and copy the key - it starts with \"AIza\"."
                    )
                    StepRow(
                        stepNumber = "3",
                        instruction = "Come back here: menu → Settings → Personal → Gemini API key. Paste it and press Save."
                    )
                    StepRow(
                        stepNumber = "4",
                        instruction = "Press the mic button again - $assistantName starts talking."
                    )
                }

                // Privacy and Security Guarantee
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(JarvisNeonGreen.copy(alpha = 0.08f))
                        .border(1.dp, JarvisNeonGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = JarvisNeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "The key is free, is stored encrypted on this phone only, and goes nowhere except Google",
                            color = JarvisTextPrimary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Paste & Key Management Section
                if (!showPasteSection && !hasKey) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://aistudio.google.com/app/apikey")
                                )
                                context.startActivity(browserIntent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("get_free_key_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Get a free key",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { showPasteSection = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("where_to_paste_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = JarvisCyanBright
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(listOf(JarvisCyan, JarvisBorder))
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Where to paste",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    // Direct Paste and Save Input Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(JarvisCardBg)
                            .border(1.dp, JarvisCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PASTE GEMINI API KEY",
                                color = JarvisCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            // Quick Clipboard Paste
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(JarvisCyan.copy(alpha = 0.15f))
                                    .clickable {
                                        val text = clipboardManager.getText()?.text
                                        if (!text.isNullOrBlank()) {
                                            inputKey = text.trim()
                                            Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = JarvisCyanBright,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "PASTE",
                                    color = JarvisCyanBright,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        OutlinedTextField(
                            value = inputKey,
                            onValueChange = {
                                inputKey = it
                                testStatus = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input_field"),
                            placeholder = {
                                Text(
                                    text = "Starts with AIza...",
                                    color = JarvisTextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                    Icon(
                                        imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility",
                                        tint = JarvisTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = JarvisTextPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = JarvisBorder,
                                focusedContainerColor = JarvisBackground,
                                unfocusedContainerColor = JarvisBackground
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Key Status or Validation Check
                        if (inputKey.isNotBlank()) {
                            val isValidPrefix = inputKey.startsWith("AIza")
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isValidPrefix) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isValidPrefix) JarvisNeonGreen else JarvisAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isValidPrefix) "Valid Google API key format (AIza...)" else "Notice: Google keys typically start with AIza",
                                    color = if (isValidPrefix) JarvisNeonGreen else JarvisAmber,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Test Status Banner
                        testStatus?.let { res ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (res.isSuccess) JarvisNeonGreen.copy(alpha = 0.15f) else JarvisRedAlert.copy(alpha = 0.15f))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (res.isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (res.isSuccess) JarvisNeonGreen else JarvisRedAlert,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = res.message,
                                        color = if (res.isSuccess) JarvisNeonGreen else JarvisRedAlert,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Action buttons inside card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Test Button
                            OutlinedButton(
                                onClick = {
                                    val keyToTest = inputKey.trim().ifBlank { currentKey }
                                    if (keyToTest.isNullOrBlank()) {
                                        Toast.makeText(context, "Enter a key first", Toast.LENGTH_SHORT).show()
                                        return@OutlinedButton
                                    }
                                    isTesting = true
                                    testStatus = null
                                    scope.launch {
                                        val result = onTestConnection(keyToTest)
                                        isTesting = false
                                        testStatus = if (result.isSuccess) {
                                            TestResultState(isSuccess = true, message = result.getOrNull() ?: "Online")
                                        } else {
                                            TestResultState(isSuccess = false, message = result.exceptionOrNull()?.message ?: "Test failed")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(40.dp).testTag("test_gemini_key_btn"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyan),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.linearGradient(listOf(JarvisCyan.copy(alpha = 0.5f), JarvisBorder))
                                ),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isTesting
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = JarvisCyan,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Test", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Save Button
                            Button(
                                onClick = {
                                    val success = onSaveKey(inputKey.trim())
                                    if (success) {
                                        Toast.makeText(context, "Gemini API key saved securely!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Please enter a valid key", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(40.dp).testTag("save_api_key_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JarvisCyan,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                enabled = inputKey.isNotBlank()
                            ) {
                                Text(
                                    text = "Save Key",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            // Remove key if already saved
                            if (hasKey) {
                                OutlinedButton(
                                    onClick = {
                                        onClearKey()
                                        inputKey = ""
                                        testStatus = null
                                        Toast.makeText(context, "Key removed", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.height(40.dp).testTag("clear_api_key_btn"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisRedAlert),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = Brush.linearGradient(listOf(JarvisRedAlert, JarvisRedAlert.copy(alpha = 0.3f)))
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete key",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom actions row (Get a free key link + Close)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://aistudio.google.com/app/apikey")
                                )
                                context.startActivity(browserIntent)
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Get a free key",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_close_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisTextSecondary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(JarvisBorder, JarvisBorder))
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Close",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

private data class TestResultState(
    val isSuccess: Boolean,
    val message: String
)

@Composable
private fun StepRow(
    stepNumber: String,
    instruction: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(JarvisCyan.copy(alpha = 0.2f))
                .border(1.dp, JarvisCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = JarvisCyanBright,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Text(
            text = instruction,
            color = JarvisTextPrimary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
