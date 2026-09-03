package com.example.data.model

sealed class JarvisAction {
    data class OpenApp(
        val appName: String,
        val packageName: String? = null
    ) : JarvisAction()

    data class MakeCall(
        val target: String
    ) : JarvisAction()

    data class SendMessage(
        val recipient: String,
        val message: String,
        val isWhatsApp: Boolean = false
    ) : JarvisAction()

    data class ToggleTorch(
        val enable: Boolean
    ) : JarvisAction()

    enum class SettingType {
        WIFI,
        BLUETOOTH,
        SOUND,
        DISPLAY,
        BATTERY,
        GENERAL
    }

    data class OpenSetting(
        val settingType: SettingType
    ) : JarvisAction()

    data class AddReminder(
        val title: String,
        val timeString: String = "",
        val category: ReminderCategory = ReminderCategory.REMINDER
    ) : JarvisAction()

    data object CheckSystemStatus : JarvisAction()

    data class Conversational(
        val message: String
    ) : JarvisAction()
}
