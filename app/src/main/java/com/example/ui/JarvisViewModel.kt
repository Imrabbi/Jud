package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiKeyManager
import com.example.data.db.AppDatabase
import com.example.data.model.ActionStatus
import com.example.data.model.ChatMessage
import com.example.data.model.JarvisAction
import com.example.data.model.MessageSender
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderEntity
import com.example.data.model.SystemInfo
import com.example.data.repository.ReminderRepository
import com.example.service.InstalledAppInfo
import com.example.service.JarvisBrain
import com.example.service.PhoneController
import com.example.service.SpeechManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    val apiKeyManager = ApiKeyManager(application)
    private val phoneController = PhoneController(application)
    private val speechManager = SpeechManager(application, viewModelScope)
    private val brain = JarvisBrain(apiKeyManager)
    private val reminderRepository: ReminderRepository

    val reminders: StateFlow<List<ReminderEntity>>

    val isListening: StateFlow<Boolean> = speechManager.isListening
    val isSpeaking: StateFlow<Boolean> = speechManager.isSpeaking
    val audioRms: StateFlow<Float> = speechManager.audioRms
    val partialSpeech: StateFlow<String> = speechManager.speechPartialResult

    val apiKey: StateFlow<String?> = apiKeyManager.apiKeyFlow
    val assistantName: StateFlow<String> = apiKeyManager.assistantNameFlow

    private val _isApiKeyConfigured = MutableStateFlow(apiKeyManager.hasValidApiKey())
    val isApiKeyConfigured: StateFlow<Boolean> = _isApiKeyConfigured.asStateFlow()

    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _isVoiceEnabled = MutableStateFlow(true)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()

    private val _systemInfo = MutableStateFlow(phoneController.getSystemInfo())
    val systemInfo: StateFlow<SystemInfo> = _systemInfo.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        reminderRepository = ReminderRepository(db.reminderDao())
        reminders = reminderRepository.allReminders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed initial greeting message
        val greetingName = apiKeyManager.getAssistantName()
        addMessage(
            ChatMessage(
                sender = MessageSender.JARVIS,
                text = "Greetings. I am $greetingName. Voice and brain systems are standing by. Tap the Arc Reactor to speak or open settings to manage your Gemini API credentials.",
                status = ActionStatus.INFO
            )
        )

        // Observe API key updates
        viewModelScope.launch {
            apiKeyManager.apiKeyFlow.collect {
                _isApiKeyConfigured.value = apiKeyManager.hasValidApiKey()
            }
        }

        // Collect speech recognition results
        viewModelScope.launch {
            speechManager.speechResult.collect { spokenText ->
                if (spokenText.isNotBlank()) {
                    handleUserCommand(spokenText)
                }
            }
        }

        // Collect speech recognition errors
        viewModelScope.launch {
            speechManager.speechError.collect { errorMsg ->
                addMessage(
                    ChatMessage(
                        sender = MessageSender.SYSTEM,
                        text = errorMsg,
                        status = ActionStatus.FAILED
                    )
                )
            }
        }

        // Observe torch state changes to keep system info in sync
        viewModelScope.launch {
            phoneController.isTorchActive.collect { isTorchOn ->
                _systemInfo.value = _systemInfo.value.copy(isTorchOn = isTorchOn)
            }
        }

        // Initial default reminders seed if empty
        viewModelScope.launch {
            reminderRepository.allReminders.collect { list ->
                if (list.isEmpty()) {
                    reminderRepository.insert(
                        ReminderEntity(
                            title = "Review Daily Schedule",
                            timeString = "Today 9:00 AM",
                            category = ReminderCategory.SCHEDULE
                        )
                    )
                    reminderRepository.insert(
                        ReminderEntity(
                            title = "Team Sync Meeting",
                            timeString = "Today 4:00 PM",
                            category = ReminderCategory.MEETING
                        )
                    )
                }
            }
        }

        loadInstalledApps()
    }

    fun startListening() {
        refreshSystemInfo()
        if (!apiKeyManager.hasValidApiKey()) {
            _showApiKeyDialog.value = true
            addMessage(
                ChatMessage(
                    sender = MessageSender.JARVIS,
                    text = "Jarvis needs a Gemini API key. Without a key she cannot hear you or answer. Please paste your key to activate voice reasoning.",
                    status = ActionStatus.INFO
                )
            )
            return
        }
        speechManager.startListening()
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun openApiKeyDialog() {
        _showApiKeyDialog.value = true
    }

    fun closeApiKeyDialog() {
        _showApiKeyDialog.value = false
    }

    fun openSettingsDialog() {
        _showSettingsDialog.value = true
    }

    fun closeSettingsDialog() {
        _showSettingsDialog.value = false
    }

    fun saveApiKey(key: String): Boolean {
        val success = apiKeyManager.saveApiKey(key)
        if (success) {
            _isApiKeyConfigured.value = true
            val name = apiKeyManager.getAssistantName()
            addMessage(
                ChatMessage(
                    sender = MessageSender.JARVIS,
                    text = "Gemini API key verified and stored. $name's voice and brain are now online. Press the mic button to speak!",
                    status = ActionStatus.SUCCESS
                )
            )
        }
        return success
    }

    fun clearApiKey() {
        apiKeyManager.clearApiKey()
        _isApiKeyConfigured.value = false
        addMessage(
            ChatMessage(
                sender = MessageSender.SYSTEM,
                text = "Gemini API key has been removed. Voice processing paused.",
                status = ActionStatus.INFO
            )
        )
    }

    fun setAssistantName(name: String) {
        apiKeyManager.setAssistantName(name)
    }

    suspend fun testGeminiConnection(keyToTest: String? = null): Result<String> {
        return brain.testGeminiConnection(keyToTest)
    }

    fun toggleVoiceFeedback() {
        _isVoiceEnabled.value = !_isVoiceEnabled.value
        if (!_isVoiceEnabled.value) {
            speechManager.stopSpeaking()
        }
    }

    fun refreshSystemInfo() {
        _systemInfo.value = phoneController.getSystemInfo()
    }

    fun handleUserCommand(command: String) {
        if (command.isBlank()) return

        // 1. Add user message
        addMessage(
            ChatMessage(
                sender = MessageSender.USER,
                text = command,
                status = ActionStatus.PENDING
            )
        )

        // 2. Process command with Jarvis Brain
        viewModelScope.launch {
            val brainResponse = brain.processCommand(command)
            executeAction(brainResponse.action, brainResponse.spokenResponse)
        }
    }

    private suspend fun executeAction(action: JarvisAction, spokenResponse: String) {
        val (status, resultMessage) = when (action) {
            is JarvisAction.OpenApp -> {
                val res = phoneController.launchInstalledApp(action.appName, action.packageName)
                (if (res.success) ActionStatus.SUCCESS else ActionStatus.FAILED) to res.message
            }
            is JarvisAction.MakeCall -> {
                val res = phoneController.makeCall(action.target)
                (if (res.success) ActionStatus.SUCCESS else ActionStatus.FAILED) to res.message
            }
            is JarvisAction.SendMessage -> {
                val res = phoneController.sendMessage(action.recipient, action.message, action.isWhatsApp)
                (if (res.success) ActionStatus.SUCCESS else ActionStatus.FAILED) to res.message
            }
            is JarvisAction.ToggleTorch -> {
                val res = phoneController.toggleFlashlight(action.enable)
                (if (res.success) ActionStatus.SUCCESS else ActionStatus.FAILED) to res.message
            }
            is JarvisAction.OpenSetting -> {
                val res = phoneController.openSetting(action.settingType)
                (if (res.success) ActionStatus.SUCCESS else ActionStatus.FAILED) to res.message
            }
            is JarvisAction.AddReminder -> {
                reminderRepository.insert(
                    ReminderEntity(
                        title = action.title,
                        timeString = action.timeString,
                        category = action.category
                    )
                )
                ActionStatus.SUCCESS to "Reminder saved: ${action.title} (${action.timeString})"
            }
            is JarvisAction.CheckSystemStatus -> {
                refreshSystemInfo()
                val info = _systemInfo.value
                val chargingText = if (info.isCharging) "charging" else "on battery"
                val statText = "Battery at ${info.batteryPercentage}% ($chargingText), Memory ${info.ramPercent}% used (${info.ramUsedMb} MB), Storage %.1f GB free.".format(info.internalStorageFreeGb)
                ActionStatus.SUCCESS to statText
            }
            is JarvisAction.Conversational -> {
                ActionStatus.INFO to action.message
            }
        }

        // Add Jarvis message to console feed
        addMessage(
            ChatMessage(
                sender = MessageSender.JARVIS,
                text = spokenResponse,
                status = status,
                statusMessage = resultMessage
            )
        )

        // Speak aloud if voice feedback is enabled
        if (_isVoiceEnabled.value) {
            speechManager.speak(spokenResponse)
        }

        refreshSystemInfo()
    }

    // Direct phone toggle functions for UI chips
    fun toggleTorchDirect() {
        val current = _systemInfo.value.isTorchOn
        viewModelScope.launch {
            val res = phoneController.toggleFlashlight(!current)
            val reply = if (res.success) "Torch turned ${if (!current) "ON" else "OFF"}, sir." else res.message
            addMessage(
                ChatMessage(
                    sender = MessageSender.JARVIS,
                    text = reply,
                    status = if (res.success) ActionStatus.SUCCESS else ActionStatus.FAILED,
                    statusMessage = res.message
                )
            )
            if (_isVoiceEnabled.value) speechManager.speak(reply)
            refreshSystemInfo()
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = phoneController.getInstalledLaunchableApps()
            _installedApps.value = apps
        }
    }

    fun openAppDirect(appName: String, customPackage: String? = null) {
        viewModelScope.launch {
            val res = phoneController.launchInstalledApp(appName, customPackage)
            val reply = res.feedbackForTts
            addMessage(
                ChatMessage(
                    sender = MessageSender.JARVIS,
                    text = reply,
                    status = if (res.success) ActionStatus.SUCCESS else ActionStatus.FAILED,
                    statusMessage = res.message
                )
            )
            if (_isVoiceEnabled.value) speechManager.speak(reply)
            refreshSystemInfo()
        }
    }

    fun openSettingDirect(settingName: String) {
        handleUserCommand("Open $settingName settings")
    }

    fun makeCallDirect(target: String = "phone dialer") {
        handleUserCommand("Call $target")
    }

    fun openWhatsAppDirect(target: String = "chat") {
        handleUserCommand("WhatsApp $target")
    }

    // Reminder operations
    fun toggleReminder(id: Long, completed: Boolean) {
        viewModelScope.launch {
            reminderRepository.toggleCompleted(id, completed)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderRepository.delete(reminder)
        }
    }

    fun addReminder(title: String, timeString: String, category: ReminderCategory) {
        viewModelScope.launch {
            reminderRepository.insert(
                ReminderEntity(
                    title = title,
                    timeString = timeString,
                    category = category
                )
            )
            val reply = "Scheduled '$title' for $timeString, sir."
            addMessage(
                ChatMessage(
                    sender = MessageSender.JARVIS,
                    text = reply,
                    status = ActionStatus.SUCCESS,
                    statusMessage = "Added to database"
                )
            )
            if (_isVoiceEnabled.value) speechManager.speak(reply)
        }
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
