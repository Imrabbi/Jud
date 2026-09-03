package com.example.service

import com.example.BuildConfig
import com.example.data.ApiKeyManager
import com.example.data.model.JarvisAction
import com.example.data.model.ReminderCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class BrainResponse(
    val action: JarvisAction,
    val spokenResponse: String
)

class JarvisBrain(
    private val apiKeyManager: ApiKeyManager? = null
) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun testGeminiConnection(keyToTest: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val key = keyToTest?.trim() ?: apiKeyManager?.getEffectiveKey() ?: BuildConfig.GEMINI_API_KEY
        if (key.isNullOrBlank() || key == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("No API key provided"))
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$key"
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Ping test. Reply with 'ONLINE' only.")
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success("Connection verified. Gemini 3.5 Flash is active and responding.")
                } else {
                    val errorMsg = try {
                        val json = JSONObject(body)
                        json.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                    } catch (_: Exception) {
                        "HTTP ${response.code} error from Gemini service"
                    }
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun processCommand(input: String): BrainResponse = withContext(Dispatchers.IO) {
        val cleanInput = input.trim()
        val apiKey = apiKeyManager?.getEffectiveKey() ?: BuildConfig.GEMINI_API_KEY

        // If Gemini API Key is available, try Gemini 3.5 Flash for advanced understanding
        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val geminiResult = callGeminiApi(cleanInput, apiKey)
                if (geminiResult != null) {
                    return@withContext geminiResult
                }
            } catch (_: Exception) {
                // Fall back to offline rule-based parser on any network or API issue
            }
        }

        // Instant offline rule-based NLP parser
        parseCommandOffline(cleanInput)
    }

    private fun callGeminiApi(userInput: String, apiKey: String): BrainResponse? {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val systemPrompt = """
            You are J.A.R.V.I.S., the ultimate personal AI assistant for Tony Stark running on Android.
            The user communicates in English, Hindi, or Hinglish.
            You must parse the user's intent to control the phone and return a JSON response with:
            - "action_type": one of ["open_app", "make_call", "send_message", "toggle_torch", "open_setting", "add_reminder", "check_system", "chat"]
            - "app_name": string (e.g. "whatsapp", "youtube", "camera", "chrome", "settings", "calculator", "spotify", "maps")
            - "target": string (contact name or phone number for calls)
            - "recipient": string (for message/whatsapp)
            - "message_text": string (content to send)
            - "is_whatsapp": boolean
            - "torch_enable": boolean
            - "setting_type": one of ["WIFI", "BLUETOOTH", "SOUND", "DISPLAY", "BATTERY", "GENERAL"]
            - "reminder_title": string
            - "reminder_time": string
            - "spoken_reply": sophisticated, brief, loyal Jarvis response (1-2 sentences) in the same language style as the user (e.g. "Opening WhatsApp now, sir", "Calling Rahul right away", "Reminder set for 5 PM, sir").
            
            Return ONLY raw valid JSON, no markdown codeblocks, no ticks.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "User Command: $userInput")
                        })
                    })
                })
            }
            put("contents", contentsArr)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemPrompt)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseString = response.body?.string() ?: return null
        val rootJson = JSONObject(responseString)
        val candidates = rootJson.optJSONArray("candidates") ?: return null
        val candidate = candidates.optJSONObject(0) ?: return null
        val content = candidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        val textPart = parts.optJSONObject(0)?.optString("text") ?: return null

        val cleanJsonText = textPart.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val parsed = JSONObject(cleanJsonText)

        val spokenReply = parsed.optString("spoken_reply", "At your service, sir.")
        val actionType = parsed.optString("action_type", "chat")

        val action: JarvisAction = when (actionType) {
            "open_app" -> JarvisAction.OpenApp(parsed.optString("app_name", "settings"))
            "make_call" -> JarvisAction.MakeCall(parsed.optString("target", ""))
            "send_message" -> JarvisAction.SendMessage(
                recipient = parsed.optString("recipient", ""),
                message = parsed.optString("message_text", ""),
                isWhatsApp = parsed.optBoolean("is_whatsapp", false)
            )
            "toggle_torch" -> JarvisAction.ToggleTorch(parsed.optBoolean("torch_enable", true))
            "open_setting" -> {
                val st = try {
                    JarvisAction.SettingType.valueOf(parsed.optString("setting_type", "GENERAL"))
                } catch (_: Exception) {
                    JarvisAction.SettingType.GENERAL
                }
                JarvisAction.OpenSetting(st)
            }
            "add_reminder" -> JarvisAction.AddReminder(
                title = parsed.optString("reminder_title", userInput),
                timeString = parsed.optString("reminder_time", "Today"),
                category = ReminderCategory.REMINDER
            )
            "check_system" -> JarvisAction.CheckSystemStatus
            else -> JarvisAction.Conversational(spokenReply)
        }

        return BrainResponse(action, spokenReply)
    }

    /**
     * High-speed offline Natural Language Intent Parser
     * Handles Hindi, Hinglish, and English commands seamlessly
     */
    fun parseCommandOffline(input: String): BrainResponse {
        val lower = input.lowercase().trim()

        // 1. Flashlight / Torch controls
        if (containsAny(lower, "torch on", "flashlight on", "torch chalu", "flash on", "light on", "batti jalao", "torch jalao")) {
            return BrainResponse(JarvisAction.ToggleTorch(true), "Flashlight activated, sir.")
        }
        if (containsAny(lower, "torch off", "flashlight off", "torch band", "flash off", "light off", "batti bujhao", "torch bujhao")) {
            return BrainResponse(JarvisAction.ToggleTorch(false), "Flashlight deactivated, sir.")
        }

        // 2. Phone Calls ("Call Rahul", "Call 9876543210", "Rahul ko call karo", "Mummy ko phone lagao")
        val callRegex = Pattern.compile("(?:call|dial|phone lagao|ko phone lagao|ko call karo)\\s+(.+)|(.+)\\s+(?:ko call lagao|ko call karo|ko phone lagao)", Pattern.CASE_INSENSITIVE)
        val callMatcher = callRegex.matcher(lower)
        if (callMatcher.find()) {
            val rawTarget = (callMatcher.group(1) ?: callMatcher.group(2))
                ?.replace("ko", "")
                ?.replace("karo", "")
                ?.replace("lagao", "")
                ?.replace("please", "")
                ?.trim() ?: ""
            if (rawTarget.isNotEmpty()) {
                val target = rawTarget.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                return BrainResponse(JarvisAction.MakeCall(target), "Initiating call to $target, sir.")
            }
        }
        if (lower.startsWith("call ") || lower.startsWith("dial ")) {
            val rawTarget = lower.removePrefix("call ").removePrefix("dial ").trim()
            val target = rawTarget.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            return BrainResponse(JarvisAction.MakeCall(target), "Calling $target now, sir.")
        }

        // 3. Check App Launching first if open/kholo keyword is present (e.g. "WhatsApp kholo", "Open YouTube")
        val appMatcher = findAppInInput(lower)
        if (appMatcher != null && (containsAny(lower, "open", "launch", "kholo", "start", "chalao") || !containsAny(lower, "bhejo", "send", "message"))) {
            return BrainResponse(JarvisAction.OpenApp(appMatcher), "Opening $appMatcher now, sir.")
        }

        // 4. WhatsApp messages ("WhatsApp Rahul hello", "Rahul ko whatsapp bhejo hi")
        if (lower.contains("whatsapp") && containsAny(lower, "bhejo", "send", "message", "text")) {
            val recipient = extractRecipient(lower, "whatsapp")
            val message = extractMessage(lower)
            return BrainResponse(
                JarvisAction.SendMessage(recipient, message, isWhatsApp = true),
                "Opening WhatsApp message for $recipient, sir."
            )
        }

        // 5. SMS / Messages ("Message Rahul hello", "Rahul ko message bhejo hello", "Send message to 98765...")
        if (containsAny(lower, "message", "sms", "sandesh")) {
            val recipient = extractRecipient(lower, "message")
            val message = extractMessage(lower)
            return BrainResponse(
                JarvisAction.SendMessage(recipient, message, isWhatsApp = false),
                "Drafting message for $recipient, sir."
            )
        }

        // 5. Device Settings ("Open Wi-Fi", "Bluetooth settings", "Sound badhao", "Battery settings")
        if (containsAny(lower, "wifi", "wi-fi", "internet setting")) {
            return BrainResponse(JarvisAction.OpenSetting(JarvisAction.SettingType.WIFI), "Opening Wi-Fi configuration, sir.")
        }
        if (containsAny(lower, "bluetooth")) {
            return BrainResponse(JarvisAction.OpenSetting(JarvisAction.SettingType.BLUETOOTH), "Opening Bluetooth settings, sir.")
        }
        if (containsAny(lower, "sound", "volume", "awaz", "ringtone")) {
            return BrainResponse(JarvisAction.OpenSetting(JarvisAction.SettingType.SOUND), "Opening Sound and Volume settings, sir.")
        }
        if (containsAny(lower, "display", "brightness", "screen")) {
            return BrainResponse(JarvisAction.OpenSetting(JarvisAction.SettingType.DISPLAY), "Opening Display settings, sir.")
        }
        if (containsAny(lower, "battery", "charge", "power saver")) {
            return BrainResponse(JarvisAction.OpenSetting(JarvisAction.SettingType.BATTERY), "Opening Battery status and settings, sir.")
        }

        // 6. Reminders & Daily Schedule ("Remind me to call John at 5 PM", "Meeting reminder", "5 baje gym jana yaad dilana")
        if (containsAny(lower, "remind", "reminder", "schedule", "yaad dilana", "yaad rakhna", "meeting", "task")) {
            val (title, time) = parseReminderDetails(lower)
            val category = if (title.contains("meeting") || title.contains("interview")) ReminderCategory.MEETING
                           else if (title.contains("call") || title.contains("phone")) ReminderCategory.CALL
                           else if (lower.contains("schedule")) ReminderCategory.SCHEDULE
                           else ReminderCategory.REMINDER

            return BrainResponse(
                JarvisAction.AddReminder(title, time, category),
                "I have added '$title' to your schedule for $time, sir."
            )
        }

        // 7. System Diagnostics ("System status", "Battery kitni hai", "Device diagnostic", "RAM status")
        if (containsAny(lower, "status", "diagnostic", "system", "ram", "storage", "phone status")) {
            return BrainResponse(JarvisAction.CheckSystemStatus, "Running full system diagnostics now, sir.")
        }

        // 8. Conversational responses
        val conversational = when {
            containsAny(lower, "hello", "hi", "hey", "namaste", "suno") ->
                "Greetings, sir. All systems are fully operational. How may I assist you today?"
            containsAny(lower, "who are you", "kaun ho", "tumhara naam") ->
                "I am J.A.R.V.I.S., your Just A Rather Very Intelligent System. I am at your command."
            containsAny(lower, "thank you", "thanks", "shukriya", "dhanyawad") ->
                "Always a pleasure, sir. Let me know if you need anything else."
            containsAny(lower, "kya kar sakte ho", "help", "features", "commands") ->
                "I can launch apps, make phone calls, send messages, manage device settings, toggle your flashlight, and manage your daily schedule and reminders."
            else ->
                "Command received: \"$input\". Executing Jarvis protocol, sir."
        }

        return BrainResponse(JarvisAction.Conversational(conversational), conversational)
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }

    private fun findAppInInput(input: String): String? {
        val cleanInput = input.trim().lowercase()

        // 1. Explicit open/launch command regex patterns
        // Matches: "open youtube", "launch spotify", "start camera", "kholo whatsapp", "run chrome"
        val openPrefixPattern = Pattern.compile("^(?:open|launch|start|run|chalao|kholo|show)\\s+(?:the\\s+)?([a-zA-Z0-9_\\-\\s]+?)(?:\\s+app|\\s+application)?$", Pattern.CASE_INSENSITIVE)
        val prefixMatcher = openPrefixPattern.matcher(cleanInput)
        if (prefixMatcher.find()) {
            val app = prefixMatcher.group(1)?.trim()
            if (!app.isNullOrBlank() && !containsAny(app, "wifi", "wi-fi", "bluetooth", "torch", "flashlight", "setting", "sound", "volume", "battery", "display")) {
                val cleaned = cleanAppName(app)
                if (cleaned.isNotBlank()) return cleaned
            }
        }

        // Matches Hindi/Hinglish suffix: "whatsapp kholo", "youtube open karo", "spotify chalao", "instagram launch karo"
        val suffixPattern = Pattern.compile("^([a-zA-Z0-9_\\-\\s]+?)\\s+(?:kholo|chalao|open karo|launch karo|start karo|on karo)$", Pattern.CASE_INSENSITIVE)
        val suffixMatcher = suffixPattern.matcher(cleanInput)
        if (suffixMatcher.find()) {
            val app = suffixMatcher.group(1)?.trim()
            if (!app.isNullOrBlank() && !containsAny(app, "torch", "light", "flash", "wifi", "bluetooth", "volume", "sound")) {
                val cleaned = cleanAppName(app)
                if (cleaned.isNotBlank()) return cleaned
            }
        }

        // 2. Check for popular known apps in input if accompanied by an open keyword
        val isOpenCommand = containsAny(cleanInput, "open", "launch", "kholo", "start", "chalao", "dekho", "run")
        val appList = listOf(
            "whatsapp", "youtube", "chrome", "google", "maps", "camera", "gallery",
            "photos", "calculator", "spotify", "settings", "calendar", "gmail",
            "play store", "clock", "contacts", "netflix", "instagram", "twitter",
            "facebook", "telegram", "reddit", "linkedin", "snapchat", "amazon"
        )
        for (app in appList) {
            if (cleanInput.contains(app)) {
                if (isOpenCommand || cleanInput == app || cleanInput == "$app app") {
                    return app
                }
            }
        }

        // 3. If input starts with "open " or "launch "
        if (cleanInput.startsWith("open ") || cleanInput.startsWith("launch ")) {
            val candidate = cleanInput.removePrefix("open ").removePrefix("launch ").removeSuffix(" app").removeSuffix(" application").trim()
            if (candidate.isNotBlank() && !containsAny(candidate, "wifi", "setting", "torch", "light")) {
                val cleaned = cleanAppName(candidate)
                if (cleaned.isNotBlank()) return cleaned
            }
        }

        return null
    }

    private fun cleanAppName(name: String): String {
        return name
            .replace(Regex("\\b(?:please|sir|bhai|zara|jaldi|now|app|application)\\b", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun extractRecipient(input: String, prefix: String): String {
        val cleaned = input
            .replace("send", "")
            .replace("message", "")
            .replace("sms", "")
            .replace("whatsapp", "")
            .replace("bhejo", "")
            .replace("karo", "")
            .replace("to", "")
            .replace("ko", "")
            .trim()
        val tokens = cleaned.split(" ")
        return tokens.firstOrNull()?.trim() ?: "Contact"
    }

    private fun extractMessage(input: String): String {
        val tokens = input.split(" ")
        return if (tokens.size > 2) {
            tokens.drop(2).joinToString(" ").replace("ko", "").replace("bhejo", "").trim()
        } else {
            "Hello from Jarvis"
        }
    }

    private fun parseReminderDetails(input: String): Pair<String, String> {
        var title = input
            .replace("remind me to", "")
            .replace("remind me", "")
            .replace("reminder for", "")
            .replace("yaad dilana", "")
            .replace("ka reminder", "")
            .replace("lagao", "")
            .trim()

        var timeString = "Today"
        if (title.contains("at ")) {
            val parts = title.split("at ")
            title = parts[0].trim()
            timeString = parts.getOrNull(1)?.trim() ?: "Today"
        } else if (title.contains("baje")) {
            val bajeRegex = Pattern.compile("(\\d+)\\s*baje")
            val matcher = bajeRegex.matcher(title)
            if (matcher.find()) {
                val hour = matcher.group(1)
                timeString = "Today $hour:00"
                title = title.replace(matcher.group(0) ?: "", "").trim()
            }
        } else if (title.contains("tomorrow") || title.contains("kal")) {
            timeString = "Tomorrow"
            title = title.replace("tomorrow", "").replace("kal", "").trim()
        }

        if (title.isBlank()) title = "General Reminder"
        return Pair(title.replaceFirstChar { it.uppercase() }, timeString)
    }
}
