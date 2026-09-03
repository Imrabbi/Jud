package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.InstalledAppInfo
import com.example.ui.components.ArcReactorVisualizer
import com.example.ui.components.GeminiApiKeyDialog
import com.example.ui.components.JarvisConsole
import com.example.ui.components.JarvisVoiceBar
import com.example.ui.components.QuickActionGrid
import com.example.ui.components.ReminderList
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SystemHud
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisNeonGreen
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun JarvisScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val partialSpeech by viewModel.partialSpeech.collectAsState()
    val isVoiceEnabled by viewModel.isVoiceEnabled.collectAsState()
    val systemInfo by viewModel.systemInfo.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val messages by viewModel.messages.collectAsState()

    val isApiKeyConfigured by viewModel.isApiKeyConfigured.collectAsState()
    val currentKey by viewModel.apiKey.collectAsState()
    val assistantName by viewModel.assistantName.collectAsState()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Permission launcher for microphone & calling
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordAudioGranted) {
            viewModel.startListening()
        }
    }

    val requestPermissionsAndListen: () -> Unit = {
        if (!isApiKeyConfigured) {
            viewModel.openApiKeyDialog()
        } else {
            val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            val hasCall = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

            if (hasAudio && hasCall) {
                viewModel.startListening()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CALL_PHONE
                    )
                )
            }
        }
    }

    // Render Gemini API Key Setup Dialog
    if (showApiKeyDialog) {
        GeminiApiKeyDialog(
            currentKey = currentKey,
            assistantName = assistantName,
            onSaveKey = { viewModel.saveApiKey(it) },
            onClearKey = { viewModel.clearApiKey() },
            onTestConnection = { viewModel.testGeminiConnection(it) },
            onDismiss = { viewModel.closeApiKeyDialog() }
        )
    }

    // Render Settings Modal Dialog (menu → Settings → Personal → Gemini API key)
    if (showSettingsDialog) {
        SettingsDialog(
            isApiKeyConfigured = isApiKeyConfigured,
            maskedApiKey = viewModel.apiKeyManager.getMaskedKey(),
            assistantName = assistantName,
            isVoiceEnabled = isVoiceEnabled,
            onToggleVoiceFeedback = { viewModel.toggleVoiceFeedback() },
            onSelectAssistantName = { viewModel.setAssistantName(it) },
            onOpenApiKeySetup = {
                viewModel.closeSettingsDialog()
                viewModel.openApiKeyDialog()
            },
            onDismiss = { viewModel.closeSettingsDialog() }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground),
        containerColor = JarvisBackground,
        topBar = {
            JarvisTopHeader(
                isApiKeyConfigured = isApiKeyConfigured,
                onOpenApiKeyDialog = { viewModel.openApiKeyDialog() },
                onOpenSettings = { viewModel.openSettingsDialog() },
                onRefresh = { viewModel.refreshSystemInfo() }
            )
        },
        bottomBar = {
            JarvisVoiceBar(
                isListening = isListening,
                isSpeaking = isSpeaking,
                isVoiceEnabled = isVoiceEnabled,
                audioRms = audioRms,
                partialSpeech = partialSpeech,
                onStartListening = requestPermissionsAndListen,
                onStopListening = { viewModel.stopListening() },
                onToggleVoiceFeedback = { viewModel.toggleVoiceFeedback() },
                onSendCommand = { viewModel.handleUserCommand(it) },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sci-Fi Mode Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF030914),
                contentColor = JarvisCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = JarvisCyan,
                        height = 2.5.dp
                    )
                },
                divider = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(JarvisBorder)
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "CORE HUD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.6.sp
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Core HUD",
                            modifier = Modifier.size(15.dp)
                        )
                    }
                )

                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "SCHEDULE (${reminders.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.6.sp
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Schedule",
                            modifier = Modifier.size(15.dp)
                        )
                    }
                )

                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Text(
                            text = "CONTROLS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.6.sp
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Phone Controls",
                            modifier = Modifier.size(15.dp)
                        )
                    }
                )

                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    text = {
                        Text(
                            text = "SETTINGS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.6.sp
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(15.dp)
                        )
                    }
                )
            }

            // Scrollable Content Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // TAB 1: CORE HUD
                        // Arc Reactor Centerpiece
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF040F1E))
                                .border(1.dp, JarvisBorder, RoundedCornerShape(20.dp))
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ArcReactorVisualizer(
                                    isListening = isListening,
                                    isSpeaking = isSpeaking,
                                    audioRms = audioRms,
                                    size = 180.dp
                                )

                                Text(
                                    text = if (isListening) "LISTENING // VOCAL COMMAND"
                                           else if (isSpeaking) "$assistantName RESPONDING..."
                                           else "ARC REACTOR CORE ONLINE",
                                    color = if (isListening) JarvisGold else if (isSpeaking) JarvisCyanBright else JarvisCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Gemini API Key Notice Banner if not configured
                        if (!isApiKeyConfigured) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(JarvisAmber.copy(alpha = 0.12f))
                                    .border(1.dp, JarvisAmber.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .clickable { viewModel.openApiKeyDialog() }
                                    .padding(12.dp)
                                    .testTag("gemini_key_required_banner")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(JarvisAmber.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Key,
                                                contentDescription = null,
                                                tint = JarvisAmber,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "Jarvis needs a Gemini API key",
                                                color = JarvisGold,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "Without a key she cannot hear you or answer. Tap here to setup.",
                                                color = JarvisTextPrimary,
                                                fontSize = 11.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(JarvisAmber)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "SETUP",
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Live Telemetry & System Status HUD
                        SystemHud(
                            systemInfo = systemInfo,
                            isListening = isListening,
                            isSpeaking = isSpeaking
                        )

                        // Quick Direct Phone Controls
                        QuickActionGrid(
                            isTorchOn = systemInfo.isTorchOn,
                            onToggleTorch = { viewModel.toggleTorchDirect() },
                            onCallClick = { viewModel.makeCallDirect() },
                            onWhatsAppClick = { viewModel.openWhatsAppDirect() },
                            onOpenApp = { viewModel.openAppDirect(it) },
                            onOpenSetting = { viewModel.openSettingDirect(it) },
                            onAddReminderClick = { selectedTabIndex = 1 }
                        )

                        // Interactive Command Feed
                        JarvisConsole(
                            messages = messages,
                            onSuggestionClick = { viewModel.handleUserCommand(it) }
                        )
                    }

                    1 -> {
                        // TAB 2: SCHEDULE & REMINDERS
                        ReminderList(
                            reminders = reminders,
                            onToggleCompleted = { id, comp -> viewModel.toggleReminder(id, comp) },
                            onDeleteReminder = { viewModel.deleteReminder(it) },
                            onAddReminder = { title, time, cat -> viewModel.addReminder(title, time, cat) }
                        )
                    }

                    2 -> {
                        // TAB 3: PHONE CONTROLS & DIAGNOSTICS
                        SystemHud(
                            systemInfo = systemInfo,
                            isListening = isListening,
                            isSpeaking = isSpeaking
                        )

                        QuickActionGrid(
                            isTorchOn = systemInfo.isTorchOn,
                            onToggleTorch = { viewModel.toggleTorchDirect() },
                            onCallClick = { viewModel.makeCallDirect() },
                            onWhatsAppClick = { viewModel.openWhatsAppDirect() },
                            onOpenApp = { viewModel.openAppDirect(it) },
                            onOpenSetting = { viewModel.openSettingDirect(it) },
                            onAddReminderClick = { selectedTabIndex = 1 }
                        )

                        // Extended Phone Management Panel
                        PhoneManagementPanel(
                            installedApps = installedApps,
                            isTorchOn = systemInfo.isTorchOn,
                            onToggleTorch = { viewModel.toggleTorchDirect() },
                            onOpenApp = { name, pkg -> viewModel.openAppDirect(name, pkg) },
                            onOpenSetting = { viewModel.openSettingDirect(it) }
                        )
                    }

                    3 -> {
                        // TAB 4: SETTINGS & PERSONAL (menu → Settings → Personal → Gemini API key)
                        SettingsPanelContent(
                            isApiKeyConfigured = isApiKeyConfigured,
                            maskedApiKey = viewModel.apiKeyManager.getMaskedKey(),
                            assistantName = assistantName,
                            isVoiceEnabled = isVoiceEnabled,
                            onToggleVoiceFeedback = { viewModel.toggleVoiceFeedback() },
                            onSelectAssistantName = { viewModel.setAssistantName(it) },
                            onOpenApiKeySetup = { viewModel.openApiKeyDialog() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JarvisTopHeader(
    isApiKeyConfigured: Boolean,
    onOpenApiKeyDialog: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(Color(0xFF020711))
            .border(1.dp, JarvisBorder)
            .padding(horizontal = 14.dp, vertical = 10.dp)
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
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isApiKeyConfigured) JarvisCyanBright else JarvisAmber)
                )
                Column {
                    Text(
                        text = "J.A.R.V.I.S. // OS",
                        color = JarvisCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "AUTONOMOUS MOBILE ASSISTANT",
                        color = JarvisTextMuted,
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Key Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isApiKeyConfigured) JarvisNeonGreen.copy(alpha = 0.15f) else JarvisAmber.copy(alpha = 0.18f))
                        .border(
                            1.dp,
                            if (isApiKeyConfigured) JarvisNeonGreen.copy(alpha = 0.4f) else JarvisAmber,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onOpenApiKeyDialog() }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("gemini_key_header_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Gemini Key Status",
                            tint = if (isApiKeyConfigured) JarvisNeonGreen else JarvisAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isApiKeyConfigured) "GEMINI ON" else "KEY NEEDED",
                            color = if (isApiKeyConfigured) JarvisNeonGreen else JarvisAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(32.dp).testTag("header_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = JarvisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp).testTag("header_refresh_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Telemetry",
                        tint = JarvisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsPanelContent(
    isApiKeyConfigured: Boolean,
    maskedApiKey: String,
    assistantName: String,
    isVoiceEnabled: Boolean,
    onToggleVoiceFeedback: () -> Unit,
    onSelectAssistantName: (String) -> Unit,
    onOpenApiKeySetup: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "PERSONAL PREFERENCES",
            color = JarvisGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
        )

        // Gemini API Key Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisCardBg)
                .border(
                    1.dp,
                    if (isApiKeyConfigured) JarvisBorder else JarvisAmber.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                )
                .clickable { onOpenApiKeySetup() }
                .padding(14.dp)
                .testTag("tab_gemini_key_card")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isApiKeyConfigured) JarvisCyan.copy(alpha = 0.15f) else JarvisAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = if (isApiKeyConfigured) JarvisCyanBright else JarvisAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Gemini API key",
                                color = JarvisTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isApiKeyConfigured) "Configured: $maskedApiKey" else "Without a key she cannot hear or answer",
                                color = if (isApiKeyConfigured) JarvisTextSecondary else JarvisAmber,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isApiKeyConfigured) JarvisCyan else JarvisAmber)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isApiKeyConfigured) "MANAGE" else "SETUP",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Assistant Persona
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisCardBg)
                .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Assistant Persona: $assistantName",
                    color = JarvisTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Maya", "Jarvis").forEach { name ->
                        val isSelected = assistantName.equals(name, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) JarvisCyan.copy(alpha = 0.2f) else JarvisBackground)
                                .border(
                                    1.dp,
                                    if (isSelected) JarvisCyanBright else JarvisBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectAssistantName(name) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                color = if (isSelected) JarvisCyanBright else JarvisTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Voice Feedback Setting
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisCardBg)
                .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp))
                .clickable { onToggleVoiceFeedback() }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Voice Vocalization (TTS)",
                        color = JarvisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isVoiceEnabled) "Spoken responses enabled" else "Silent screen output only",
                        color = JarvisTextMuted,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isVoiceEnabled) JarvisNeonGreen.copy(alpha = 0.2f) else JarvisBackground)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isVoiceEnabled) "ENABLED" else "MUTED",
                        color = if (isVoiceEnabled) JarvisNeonGreen else JarvisTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneManagementPanel(
    installedApps: List<InstalledAppInfo>,
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    onOpenApp: (String, String?) -> Unit,
    onOpenSetting: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INSTALLED APPS // PACKAGE MANAGER",
                color = JarvisTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "VOICE: \"OPEN <APP>\"",
                color = JarvisCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        val appsToDisplay: List<Pair<String, String?>> = if (installedApps.isNotEmpty()) {
            installedApps.take(12).map { it.appName to it.packageName }
        } else {
            listOf(
                "WhatsApp" to "com.whatsapp",
                "YouTube" to "com.google.android.youtube",
                "Chrome" to "com.android.chrome",
                "Maps" to "com.google.android.apps.maps",
                "Camera" to null,
                "Settings" to null,
                "Calculator" to "com.google.android.calculator",
                "Spotify" to "com.spotify.music",
                "Gmail" to "com.google.android.gm",
                "Photos" to "com.google.android.apps.photos"
            )
        }

        appsToDisplay.chunked(4).forEach { rowApps ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowApps.forEach { (label, key) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(JarvisCardBg)
                            .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
                            .clickable { onOpenApp(label, key) }
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = JarvisTextPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
                repeat(4 - rowApps.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "HARDWARE & SYSTEM TOGGLES",
            color = JarvisTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Wi-Fi" to "WIFI",
                "Bluetooth" to "BLUETOOTH",
                "Sound" to "SOUND",
                "Display" to "DISPLAY",
                "Battery" to "BATTERY"
            ).forEach { (label, key) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF091629))
                        .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
                        .clickable { onOpenSetting(key) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = JarvisCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
