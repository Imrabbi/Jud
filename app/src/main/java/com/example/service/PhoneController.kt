package com.example.service

import android.Manifest
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.data.model.JarvisAction
import com.example.data.model.SystemInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class ActionResult(
    val success: Boolean,
    val message: String,
    val feedbackForTts: String
)

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val activityName: String? = null
)

class PhoneController(private val context: Context) {

    private val _isTorchActive = MutableStateFlow(false)
    val isTorchActive: StateFlow<Boolean> = _isTorchActive.asStateFlow()

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    private var cameraIdWithFlash: String? = null

    init {
        try {
            cameraManager?.cameraIdList?.forEach { id ->
                val characteristics = cameraManager?.getCameraCharacteristics(id)
                val hasFlash = characteristics?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash) {
                    cameraIdWithFlash = id
                    return@forEach
                }
            }
        } catch (_: Exception) {
            // Flash camera lookup ignored
        }
    }

    /**
     * Map of standard app names to package names and intent actions
     */
    private val knownAppPackages = mapOf(
        "whatsapp" to "com.whatsapp",
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "google" to "com.google.android.googlequicksearchbox",
        "maps" to "com.google.android.apps.maps",
        "gmail" to "com.google.android.gm",
        "email" to "com.google.android.gm",
        "calendar" to "com.google.android.calendar",
        "calculator" to "com.google.android.calculator",
        "camera" to "com.google.android.GoogleCamera",
        "photos" to "com.google.android.apps.photos",
        "gallery" to "com.google.android.apps.photos",
        "spotify" to "com.spotify.music",
        "settings" to "com.android.settings",
        "play store" to "com.android.vending",
        "clock" to "com.google.android.deskclock",
        "contacts" to "com.android.contacts",
        "phone" to "dialer",
        "messages" to "sms"
    )

    /**
     * Launches an installed application using PackageManager intents.
     * Enables Jarvis to find, resolve, and open applications based on user voice commands or direct interaction.
     *
     * Resolves applications via:
     * 1. Direct hardware/standard intent shortcuts (Camera, Settings)
     * 2. Explicit package intent if customPackage is supplied or if input is a package name
     * 3. Known app package mapping dictionary for fast-path launcher resolution
     * 4. Dynamic query of all installed launcher activities via PackageManager.queryIntentActivities()
     *    supporting exact match, prefix match, word-boundary match, and fuzzy containment.
     * 5. Google Play Store or web search fallback if the application is not installed.
     *
     * @param appNameOrPackage Name of the app requested by the user (e.g. "YouTube", "Calculator", "WhatsApp")
     *                          or an explicit package name (e.g. "com.whatsapp").
     * @param customPackage Optional explicit package name to launch directly.
     * @return ActionResult with execution status, log message, and spoken feedback for TTS.
     */
    fun launchInstalledApp(appNameOrPackage: String, customPackage: String? = null): ActionResult {
        val rawInput = appNameOrPackage.trim()
        if (rawInput.isBlank()) {
            return ActionResult(false, "No app name specified", "I didn't catch the application name, sir.")
        }

        // Clean user voice phrasing: e.g. "open WhatsApp please" -> "WhatsApp", "YouTube kholo" -> "YouTube"
        val cleanName = rawInput
            .replace(Regex("^(?:open|launch|start|run|chalao|kholo)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(?:kholo|chalao|open karo|launch karo|start karo|on karo)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(?:app|application)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(?:the)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(?:zara|please|bhai)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(?:please|sir|zara|jaldi|now)$", RegexOption.IGNORE_CASE), "")
            .trim()
        val lowerName = cleanName.lowercase()

        // 1. Direct hardware & system activity shortcuts
        if (lowerName == "camera" || lowerName == "photo" || lowerName == "photos camera") {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(cameraIntent)
                ActionResult(true, "Camera opened", "Opening camera now, sir.")
            } catch (_: Exception) {
                launchPackageWithIntent("com.google.android.GoogleCamera", "Camera")
            }
        }

        if (lowerName == "settings" || lowerName == "setting" || lowerName == "system settings") {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(settingsIntent)
                ActionResult(true, "Settings opened", "Opening device settings, sir.")
            } catch (e: Exception) {
                ActionResult(false, "Could not open Settings: ${e.message}", "Unable to open settings, sir.")
            }
        }

        // 2. Direct package lookup if provided explicitly or if rawInput is in package format (e.g. com.example.app)
        val directPackage = customPackage ?: if (cleanName.contains(".") && !cleanName.contains(" ")) cleanName else null
        if (directPackage != null) {
            return launchPackageWithIntent(directPackage, cleanName)
        }

        // 3. Known app aliases (fast-path resolution for common popular applications)
        val knownPackage = knownAppPackages[lowerName]
        if (knownPackage != null) {
            val result = launchPackageWithIntent(knownPackage, cleanName)
            if (result.success) {
                return result
            }
        }

        // 4. Query PackageManager with Intent(ACTION_MAIN, CATEGORY_LAUNCHER) to locate installed applications
        val matchedApp = findInstalledAppWithPackageManager(lowerName)
        if (matchedApp != null) {
            return launchPackageWithIntent(matchedApp.packageName, matchedApp.appName, matchedApp.activityName)
        }

        // 5. If known package was looked up but wasn't installed, offer Play Store fallback
        if (knownPackage != null) {
            return fallbackPlayStore(knownPackage, cleanName)
        }

        // 6. Generic web search fallback so Jarvis never dead-ends
        return try {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(cleanName, StandardCharsets.UTF_8.toString())}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            ActionResult(
                true,
                "App '$cleanName' not found on device; opened web search",
                "I could not locate $cleanName on your device. Searching online for you, sir."
            )
        } catch (e: Exception) {
            ActionResult(
                false,
                "Application '$cleanName' not found",
                "I could not locate $cleanName on your device, sir."
            )
        }
    }

    /**
     * Backward-compatible alias for launchInstalledApp
     */
    fun openApp(appName: String, customPackage: String? = null): ActionResult {
        return launchInstalledApp(appName, customPackage)
    }

    /**
     * Uses PackageManager to generate a launch Intent and start the target application.
     */
    fun launchPackageWithIntent(
        packageName: String,
        displayName: String,
        explicitActivityName: String? = null
    ): ActionResult {
        val pm = context.packageManager
        return try {
            // First attempt to get the canonical launch intent for the package
            val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: run {
                // If explicit activity name is known from launcher query, build component intent
                if (!explicitActivityName.isNullOrBlank()) {
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        component = ComponentName(packageName, explicitActivityName)
                    }
                } else null
            }

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(launchIntent)
                ActionResult(
                    true,
                    "$displayName launched ($packageName)",
                    "Launching $displayName now, sir."
                )
            } else {
                ActionResult(
                    false,
                    "$displayName ($packageName) is not installed",
                    "$displayName is not installed on this device, sir."
                )
            }
        } catch (e: Exception) {
            ActionResult(
                false,
                "Could not launch $displayName: ${e.message}",
                "Unable to launch $displayName: ${e.message}"
            )
        }
    }

    /**
     * Queries PackageManager for all installed launcher applications and performs
     * exact, prefix, word boundary, and fuzzy containment matching against the user query.
     */
    fun findInstalledAppWithPackageManager(query: String): InstalledAppInfo? {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveList = try {
            pm.queryIntentActivities(launcherIntent, 0)
        } catch (_: Exception) {
            emptyList()
        }

        if (resolveList.isEmpty()) {
            // Secondary fallback using getInstalledApplications
            try {
                val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                for (app in apps) {
                    val label = pm.getApplicationLabel(app).toString().trim()
                    if (label.equals(query, ignoreCase = true) || label.lowercase().contains(query)) {
                        return InstalledAppInfo(label, app.packageName)
                    }
                }
            } catch (_: Exception) {}
            return null
        }

        val cleanQuery = query.lowercase().trim()

        // 1. Exact case-insensitive label match (e.g. "calculator" == "Calculator")
        for (info in resolveList) {
            val label = info.loadLabel(pm).toString().trim()
            if (label.equals(cleanQuery, ignoreCase = true)) {
                return InstalledAppInfo(label, info.activityInfo.packageName, info.activityInfo.name)
            }
        }

        // 2. Starts with query (e.g. "chrome" starts "Google Chrome" or "Chrome Browser")
        for (info in resolveList) {
            val label = info.loadLabel(pm).toString().trim()
            if (label.lowercase().startsWith(cleanQuery)) {
                return InstalledAppInfo(label, info.activityInfo.packageName, info.activityInfo.name)
            }
        }

        // 3. Word boundary match (e.g. "photos" in "Google Photos", "drive" in "Google Drive")
        for (info in resolveList) {
            val label = info.loadLabel(pm).toString().trim()
            val words = label.lowercase().split(" ", "-", "_", ".")
            if (words.contains(cleanQuery)) {
                return InstalledAppInfo(label, info.activityInfo.packageName, info.activityInfo.name)
            }
        }

        // 4. Substring containment in display label
        for (info in resolveList) {
            val label = info.loadLabel(pm).toString().trim()
            if (label.lowercase().contains(cleanQuery)) {
                return InstalledAppInfo(label, info.activityInfo.packageName, info.activityInfo.name)
            }
        }

        // 5. Package name containment
        for (info in resolveList) {
            val pkg = info.activityInfo.packageName.lowercase()
            if (pkg.contains(cleanQuery)) {
                val label = info.loadLabel(pm).toString().trim()
                return InstalledAppInfo(label, info.activityInfo.packageName, info.activityInfo.name)
            }
        }

        return null
    }

    /**
     * Fallback to Google Play Store if an application is not installed.
     */
    fun fallbackPlayStore(packageName: String, displayName: String): ActionResult {
        return try {
            val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(storeIntent)
            ActionResult(
                true,
                "$displayName not installed; opened Play Store ($packageName)",
                "$displayName is not installed. Opening Google Play Store for you."
            )
        } catch (e: Exception) {
            ActionResult(
                false,
                "$displayName ($packageName) is not installed",
                "I could not launch $displayName as it is not installed."
            )
        }
    }

    /**
     * Returns a list of all launchable applications installed on the device using PackageManager.
     */
    fun getInstalledLaunchableApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return try {
            val resolveList = pm.queryIntentActivities(launcherIntent, 0)
            resolveList.mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: pkg
                val activity = info.activityInfo?.name
                InstalledAppInfo(appName = label, packageName = pkg, activityName = activity)
            }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun makeCall(target: String): ActionResult {
        val cleanTarget = target.trim()
        val digitsOnly = cleanTarget.filter { it.isDigit() || it == '+' }
        val numberToDial = if (digitsOnly.isNotEmpty()) digitsOnly else cleanTarget

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission && digitsOnly.length >= 3) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$numberToDial")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numberToDial")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return try {
            context.startActivity(intent)
            val actionName = if (hasCallPermission && digitsOnly.length >= 3) "Calling" else "Dialing"
            ActionResult(true, "$actionName $cleanTarget", "$actionName $cleanTarget now, sir.")
        } catch (e: Exception) {
            ActionResult(false, "Could not initiate call: ${e.message}", "Unable to place call to $cleanTarget.")
        }
    }

    fun sendMessage(recipient: String, message: String, isWhatsApp: Boolean): ActionResult {
        if (isWhatsApp) {
            val digits = recipient.filter { it.isDigit() || it == '+' }
            val url = if (digits.isNotEmpty()) {
                "https://api.whatsapp.com/send?phone=$digits&text=${URLEncoder.encode(message, StandardCharsets.UTF_8.toString())}"
            } else {
                "https://api.whatsapp.com/send?text=${URLEncoder.encode(message, StandardCharsets.UTF_8.toString())}"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(intent)
                ActionResult(true, "WhatsApp message opened for $recipient", "Opening WhatsApp message for $recipient, sir.")
            } catch (_: Exception) {
                // Try standard browser WhatsApp or fallback SMS
                try {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                    ActionResult(true, "WhatsApp web message opened", "Opening WhatsApp chat.")
                } catch (e: Exception) {
                    sendSmsMessage(recipient, message)
                }
            }
        } else {
            return sendSmsMessage(recipient, message)
        }
    }

    private fun sendSmsMessage(recipient: String, message: String): ActionResult {
        val digits = recipient.filter { it.isDigit() || it == '+' }
        val uri = if (digits.isNotEmpty()) Uri.parse("smsto:$digits") else Uri.parse("smsto:")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult(true, "SMS drafted for $recipient: \"$message\"", "Preparing SMS message for $recipient, sir.")
        } catch (e: Exception) {
            ActionResult(false, "Failed to open SMS: ${e.message}", "Unable to send message.")
        }
    }

    fun toggleFlashlight(enable: Boolean): ActionResult {
        val camManager = cameraManager
        val camId = cameraIdWithFlash
        if (camManager == null || camId == null) {
            return ActionResult(false, "Flashlight hardware unavailable", "Flashlight hardware is not available on this device.")
        }
        return try {
            camManager.setTorchMode(camId, enable)
            _isTorchActive.value = enable
            val stateText = if (enable) "ON" else "OFF"
            ActionResult(true, "Flashlight turned $stateText", "Flashlight turned $stateText, sir.")
        } catch (e: CameraAccessException) {
            ActionResult(false, "Camera hardware busy: ${e.message}", "Cannot access flashlight at this moment.")
        } catch (e: Exception) {
            ActionResult(false, "Error: ${e.message}", "Failed to toggle flashlight.")
        }
    }

    fun openSetting(settingType: JarvisAction.SettingType): ActionResult {
        val (action, name) = when (settingType) {
            JarvisAction.SettingType.WIFI -> Settings.ACTION_WIFI_SETTINGS to "Wi-Fi Settings"
            JarvisAction.SettingType.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS to "Bluetooth Settings"
            JarvisAction.SettingType.SOUND -> Settings.ACTION_SOUND_SETTINGS to "Sound & Volume Settings"
            JarvisAction.SettingType.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS to "Display & Brightness"
            JarvisAction.SettingType.BATTERY -> Settings.ACTION_BATTERY_SAVER_SETTINGS to "Battery Settings"
            JarvisAction.SettingType.GENERAL -> Settings.ACTION_SETTINGS to "General Settings"
        }

        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult(true, "Opened $name", "Opening $name, sir.")
        } catch (e: Exception) {
            ActionResult(false, "Could not open $name: ${e.message}", "Unable to open $name.")
        }
    }

    fun getSystemInfo(): SystemInfo {
        // Battery info
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        val isCharging = batteryManager?.isCharging ?: false

        // RAM info
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val totalRamMb = memoryInfo.totalMem / (1024 * 1024)
        val availRamMb = memoryInfo.availMem / (1024 * 1024)
        val usedRamMb = (totalRamMb - availRamMb).coerceAtLeast(0)

        // Storage info
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val storageFreeGb = bytesAvailable.toFloat() / (1024f * 1024f * 1024f)

        return SystemInfo(
            batteryPercentage = batteryPct,
            isCharging = isCharging,
            ramUsedMb = usedRamMb,
            ramTotalMb = totalRamMb,
            internalStorageFreeGb = storageFreeGb,
            isTorchOn = _isTorchActive.value
        )
    }
}
