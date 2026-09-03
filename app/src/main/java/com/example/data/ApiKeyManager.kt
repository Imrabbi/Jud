package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ApiKeyManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _apiKeyFlow = MutableStateFlow(getEffectiveKey())
    val apiKeyFlow: StateFlow<String?> = _apiKeyFlow.asStateFlow()

    private val _assistantNameFlow = MutableStateFlow(getAssistantName())
    val assistantNameFlow: StateFlow<String> = _assistantNameFlow.asStateFlow()

    fun getEffectiveKey(): String? {
        val userKey = prefs.getString(KEY_GEMINI_API, null)?.trim()
        if (!userKey.isNullOrBlank()) {
            return userKey
        }
        val buildConfigKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            null
        }
        if (!buildConfigKey.isNullOrBlank() && buildConfigKey != "MY_GEMINI_API_KEY" && buildConfigKey.startsWith("AIza")) {
            return buildConfigKey
        }
        return null
    }

    fun hasValidApiKey(): Boolean {
        val key = getEffectiveKey()
        return !key.isNullOrBlank() && (key.startsWith("AIza") || key.length >= 20)
    }

    fun saveApiKey(rawKey: String): Boolean {
        val trimmed = rawKey.trim()
        if (trimmed.isBlank()) {
            return false
        }
        prefs.edit().putString(KEY_GEMINI_API, trimmed).apply()
        _apiKeyFlow.value = trimmed
        return true
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_GEMINI_API).apply()
        _apiKeyFlow.value = getEffectiveKey()
    }

    fun getMaskedKey(): String {
        val key = getEffectiveKey() ?: return "No key configured"
        return if (key.length > 8) {
            "${key.take(6)}...${key.takeLast(4)}"
        } else {
            "********"
        }
    }

    fun getKeySource(): String {
        val userKey = prefs.getString(KEY_GEMINI_API, null)?.trim()
        if (!userKey.isNullOrBlank()) {
            return "Saved in Personal Settings"
        }
        val buildConfigKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            null
        }
        if (!buildConfigKey.isNullOrBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
            return "Injected via Secrets Panel"
        }
        return "None"
    }

    fun getAssistantName(): String {
        return prefs.getString(KEY_ASSISTANT_NAME, "Maya") ?: "Maya"
    }

    fun setAssistantName(name: String) {
        prefs.edit().putString(KEY_ASSISTANT_NAME, name).apply()
        _assistantNameFlow.value = name
    }

    companion object {
        private const val PREFS_NAME = "jarvis_gemini_prefs"
        private const val KEY_GEMINI_API = "gemini_api_key"
        private const val KEY_ASSISTANT_NAME = "assistant_name"
    }
}

