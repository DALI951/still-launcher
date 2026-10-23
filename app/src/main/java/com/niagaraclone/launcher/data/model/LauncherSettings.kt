package com.niagaraclone.launcher.data.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Themes for Still Launcher. True Black (AMOLED Pitch) is the default.
 */
enum class StillThemeMode(val label: String, val bgHex: Long, val surfaceHex: Long, val textHex: Long, val textSecondaryHex: Long) {
    TRUE_BLACK("True Black", 0xFF000000, 0xFF121212, 0xFFF0F0F0, 0xFF888888),
    DARK("Dark", 0xFF121214, 0xFF1E1E22, 0xFFECECEC, 0xFF9E9EA4),
    LIGHT("Light", 0xFFF8F9FA, 0xFFFFFFFF, 0xFF1A1A1A, 0xFF666666)
}

/**
 * Icon presentation style: pure typography (Minimalist Phone style) or simple monochrome icons.
 */
enum class IconStyle(val label: String) {
    PURE_TEXT("Pure Text (Minimalist)"),
    MONOCHROME_ICONS("Monochrome Icons")
}

/**
 * Focus Profiles for intentional phone use.
 */
enum class FocusProfile(val label: String, val description: String) {
    DEFAULT("Default", "Standard intentional launcher mode"),
    STUDY("Study", "Restricts distracting social & entertainment apps during study sessions"),
    SLEEP("Sleep", "Restricts non-essential applications during bedtime hours"),
    CUSTOM("Custom", "Custom schedule and user-selected restricted applications")
}

/**
 * Per-application launch rules.
 */
enum class AppLaunchRule(val label: String, val description: String) {
    NORMAL("Normal", "Opens immediately with no delay"),
    DELAYED("Delayed", "Mindful breathing pause before opening"),
    SCHEDULED("Scheduled", "Restricted during configured focus hours"),
    LIMITED("Limited", "Subject to daily usage limits"),
    BLOCKED("Blocked", "Restricted from launching while active")
}

/**
 * Accent colors for subtle highlights.
 */
enum class StillAccent(val label: String, val hex: Long, val lightHex: Long) {
    SLATE("Slate", 0xFF8E99A4, 0xFFCFD8DC),
    BLUE("Still Blue", 0xFF4E8CFF, 0xFF99C2FF),
    MINT("Mint", 0xFF2ECC71, 0xFF82E0AA),
    AMBER("Amber", 0xFFF39C12, 0xFFFAD7A0),
    ROSE("Rose", 0xFFE74C3C, 0xFFF5B7B1),
    MONO("Pure White / Gray", 0xFFE0E0E0, 0xFFFFFFFF)
}

/**
 * YouTube playback history entry (stored strictly locally, never used for recommendations).
 */
data class YouTubeHistoryItem(
    val id: String,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("url", url)
        put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(json: JSONObject): YouTubeHistoryItem {
            return YouTubeHistoryItem(
                id = json.optString("id", ""),
                title = json.optString("title", "YouTube Video"),
                url = json.optString("url", ""),
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}

/**
 * Comprehensive settings model for Still — Minimal Launcher.
 */
data class LauncherSettings(
    // 1. Appearance
    val themeMode: StillThemeMode = StillThemeMode.TRUE_BLACK,
    val accent: StillAccent = StillAccent.SLATE,
    val showAppIcons: Boolean = false, // false = pure text (Minimalist Phone default)
    val textSizeSp: Int = 16,          // 13..22
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val clock24Hour: Boolean = false,
    val clockFontSizeSp: Int = 52,     // 36..68

    // 2. YouTube Gate
    val interceptYouTube: Boolean = true,
    val youtubeHistoryEnabled: Boolean = true,

    // 3. Focus & Restrictions
    val activeFocusProfile: FocusProfile = FocusProfile.DEFAULT,
    val breathingDelaySeconds: Int = 5,
    val studyStartHour: Int = 9,
    val studyEndHour: Int = 17,
    val sleepStartHour: Int = 22,
    val sleepEndHour: Int = 7,

    // 4. Mindful In-App Usage Timers (Minimalist Phone feature)
    val appUsageTimersEnabled: Boolean = true,
    val askTimerOnAppLaunch: Boolean = true,
    val defaultTimerMinutes: Int = 5,

    // 5. Daily Mindfulness & Screen Time Awareness
    val showScreenTimeOnHome: Boolean = true,
    val dailyScreenTimeGoalMinutes: Int = 120,
    val showDailyIntention: Boolean = true,
    val dailyIntentionText: String = "",

    // 6. Seamless Automatic Background Updates
    val autoUpdateEnabled: Boolean = true
) {
    val activeAccentHex: Long get() = accent.hex
    val activeAccentLightHex: Long get() = accent.lightHex
}
