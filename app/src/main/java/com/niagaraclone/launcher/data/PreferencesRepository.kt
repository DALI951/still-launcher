package com.niagaraclone.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.niagaraclone.launcher.data.model.AppLaunchRule
import com.niagaraclone.launcher.data.model.FocusProfile
import com.niagaraclone.launcher.data.model.IntentionRecord
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.data.model.StillAccent
import com.niagaraclone.launcher.data.model.StillThemeMode
import com.niagaraclone.launcher.data.model.YouTubeHistoryItem
import com.niagaraclone.launcher.data.model.YouTubeQueueItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_prefs")

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val FAVORITE_KEYS_ORDERED = stringPreferencesKey("favorite_keys_ordered")
        val HIDDEN_KEYS = stringSetPreferencesKey("hidden_keys")
        val IS_INITIAL_SEEDED = booleanPreferencesKey("is_initial_seeded")

        // 1. Appearance
        val SETTING_STILL_THEME_MODE = stringPreferencesKey("setting_still_theme_mode")
        val SETTING_STILL_ACCENT = stringPreferencesKey("setting_still_accent")
        val SETTING_SHOW_APP_ICONS = booleanPreferencesKey("setting_show_app_icons")
        val SETTING_TEXT_SIZE = intPreferencesKey("setting_text_size")
        val SETTING_SHOW_CLOCK = booleanPreferencesKey("setting_show_clock")
        val SETTING_SHOW_DATE = booleanPreferencesKey("setting_show_date")
        val SETTING_SHOW_BATTERY = booleanPreferencesKey("setting_show_battery")
        val SETTING_CLOCK_24_HOUR = booleanPreferencesKey("setting_clock_24_hour")
        val SETTING_CLOCK_FONT_SIZE = intPreferencesKey("setting_clock_font_size")
        val SETTING_DOUBLE_TAP_TO_LOCK = booleanPreferencesKey("setting_double_tap_to_lock")
        val SETTING_SHOW_DAILY_QUOTE = booleanPreferencesKey("setting_show_daily_quote")

        // 2. YouTube Gate
        val SETTING_INTERCEPT_YOUTUBE = booleanPreferencesKey("setting_intercept_youtube")
        val SETTING_YOUTUBE_HISTORY_ENABLED = booleanPreferencesKey("setting_youtube_history_enabled")
        val SETTING_YOUTUBE_HISTORY_JSON = stringPreferencesKey("setting_youtube_history_json")
        val SETTING_YOUTUBE_QUEUE_JSON = stringPreferencesKey("setting_youtube_queue_json")

        // 3. Focus & Restrictions
        val SETTING_ACTIVE_FOCUS_PROFILE = stringPreferencesKey("setting_active_focus_profile")
        val SETTING_BREATHING_DELAY_SECONDS = intPreferencesKey("setting_breathing_delay_seconds")
        val SETTING_STUDY_START_HOUR = intPreferencesKey("setting_study_start_hour")
        val SETTING_STUDY_END_HOUR = intPreferencesKey("setting_study_end_hour")
        val SETTING_SLEEP_START_HOUR = intPreferencesKey("setting_sleep_start_hour")
        val SETTING_SLEEP_END_HOUR = intPreferencesKey("setting_sleep_end_hour")
        val SETTING_INTENTION_PROMPT_ENABLED = booleanPreferencesKey("setting_intention_prompt_enabled")
        val INTENTIONS_HISTORY_JSON = stringPreferencesKey("intentions_history_json")

        // 4. Mindful App Usage Timers
        val SETTING_APP_USAGE_TIMERS_ENABLED = booleanPreferencesKey("setting_app_usage_timers_enabled")
        val SETTING_ASK_TIMER_ON_LAUNCH = booleanPreferencesKey("setting_ask_timer_on_launch")
        val SETTING_DEFAULT_TIMER_MINUTES = intPreferencesKey("setting_default_timer_minutes")

        // 5. Daily Screen Time & Intentions
        val SETTING_SHOW_SCREEN_TIME_ON_HOME = booleanPreferencesKey("setting_show_screen_time_on_home")
        val SETTING_DAILY_SCREEN_TIME_GOAL_MINUTES = intPreferencesKey("setting_daily_screen_time_goal_minutes")
        val SETTING_SHOW_DAILY_INTENTION = booleanPreferencesKey("setting_show_daily_intention")
        val SETTING_DAILY_INTENTION_TEXT = stringPreferencesKey("setting_daily_intention_text")

        // 6. Seamless Background Updates
        val SETTING_AUTO_UPDATE_ENABLED = booleanPreferencesKey("setting_auto_update_enabled")
        val LAST_AUTO_UPDATE_CHECK_MS = androidx.datastore.preferences.core.longPreferencesKey("last_auto_update_check_ms")

        // Serialized per-app launch rules: "pkg/comp=RULE_NAME:::pkg2/comp2=RULE_NAME"
        val APP_LAUNCH_RULES = stringPreferencesKey("app_launch_rules")
        // Serialized per-app daily usage limits: "pkg/comp=minutes:::pkg2/comp2=minutes"
        val APP_DAILY_LIMITS = stringPreferencesKey("app_daily_limits")
        // Serialized custom app labels: "uniqueKey=customName:::uniqueKey2=customName2"
        val CUSTOM_APP_NAMES = stringPreferencesKey("custom_app_names")
    }

    private val dataStore = context.launcherDataStore

    /**
     * Flow of ordered favorite app keys.
     */
    val favoriteKeysFlow: Flow<List<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val serialized = preferences[PreferencesKeys.FAVORITE_KEYS_ORDERED] ?: ""
            if (serialized.isEmpty()) emptyList() else serialized.split(":::").filter { it.isNotEmpty() }
        }

    /**
     * Flow of hidden app keys.
     */
    val hiddenKeysFlow: Flow<Set<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.HIDDEN_KEYS] ?: emptySet()
        }

    /**
     * Flag indicating if initial default favorites have been seeded.
     */
    val isInitialSeededFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_INITIAL_SEEDED] ?: false
        }

    /**
     * Flow of user customization settings.
     */
    val settingsFlow: Flow<LauncherSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val themeModeName = prefs[PreferencesKeys.SETTING_STILL_THEME_MODE] ?: StillThemeMode.TRUE_BLACK.name
            val themeMode = try { StillThemeMode.valueOf(themeModeName) } catch (e: Exception) { StillThemeMode.TRUE_BLACK }

            val accentName = prefs[PreferencesKeys.SETTING_STILL_ACCENT] ?: StillAccent.SLATE.name
            val accent = try { StillAccent.valueOf(accentName) } catch (e: Exception) { StillAccent.SLATE }

            val showAppIcons = prefs[PreferencesKeys.SETTING_SHOW_APP_ICONS] ?: false
            val textSize = prefs[PreferencesKeys.SETTING_TEXT_SIZE] ?: 16
            val showClock = prefs[PreferencesKeys.SETTING_SHOW_CLOCK] ?: true
            val showDate = prefs[PreferencesKeys.SETTING_SHOW_DATE] ?: true
            val showBattery = prefs[PreferencesKeys.SETTING_SHOW_BATTERY] ?: true
            val clock24Hour = prefs[PreferencesKeys.SETTING_CLOCK_24_HOUR] ?: false
            val clockFontSize = prefs[PreferencesKeys.SETTING_CLOCK_FONT_SIZE] ?: 52
            val doubleTapToLock = prefs[PreferencesKeys.SETTING_DOUBLE_TAP_TO_LOCK] ?: true

            val interceptYouTube = prefs[PreferencesKeys.SETTING_INTERCEPT_YOUTUBE] ?: true
            val youtubeHistoryEnabled = prefs[PreferencesKeys.SETTING_YOUTUBE_HISTORY_ENABLED] ?: true

            val focusProfileName = prefs[PreferencesKeys.SETTING_ACTIVE_FOCUS_PROFILE] ?: FocusProfile.DEFAULT.name
            val focusProfile = try { FocusProfile.valueOf(focusProfileName) } catch (e: Exception) { FocusProfile.DEFAULT }
            val breathingDelay = prefs[PreferencesKeys.SETTING_BREATHING_DELAY_SECONDS] ?: 5

            val studyStart = prefs[PreferencesKeys.SETTING_STUDY_START_HOUR] ?: 9
            val studyEnd = prefs[PreferencesKeys.SETTING_STUDY_END_HOUR] ?: 17
            val sleepStart = prefs[PreferencesKeys.SETTING_SLEEP_START_HOUR] ?: 22
            val sleepEnd = prefs[PreferencesKeys.SETTING_SLEEP_END_HOUR] ?: 7
            val intentionPromptEnabled = prefs[PreferencesKeys.SETTING_INTENTION_PROMPT_ENABLED] ?: true

            val appUsageTimersEnabled = prefs[PreferencesKeys.SETTING_APP_USAGE_TIMERS_ENABLED] ?: true
            val askTimerOnLaunch = prefs[PreferencesKeys.SETTING_ASK_TIMER_ON_LAUNCH] ?: true
            val defaultTimerMinutes = prefs[PreferencesKeys.SETTING_DEFAULT_TIMER_MINUTES] ?: 5

            val showScreenTimeOnHome = prefs[PreferencesKeys.SETTING_SHOW_SCREEN_TIME_ON_HOME] ?: true
            val dailyScreenTimeGoal = prefs[PreferencesKeys.SETTING_DAILY_SCREEN_TIME_GOAL_MINUTES] ?: 120
            val showDailyIntention = prefs[PreferencesKeys.SETTING_SHOW_DAILY_INTENTION] ?: true
            val dailyIntentionText = prefs[PreferencesKeys.SETTING_DAILY_INTENTION_TEXT] ?: ""
            val showDailyQuote = prefs[PreferencesKeys.SETTING_SHOW_DAILY_QUOTE] ?: true
            val autoUpdateEnabled = prefs[PreferencesKeys.SETTING_AUTO_UPDATE_ENABLED] ?: true

            LauncherSettings(
                themeMode = themeMode,
                accent = accent,
                showAppIcons = showAppIcons,
                textSizeSp = textSize,
                showClock = showClock,
                showDate = showDate,
                showBattery = showBattery,
                clock24Hour = clock24Hour,
                clockFontSizeSp = clockFontSize,
                doubleTapToLock = doubleTapToLock,
                interceptYouTube = interceptYouTube,
                youtubeHistoryEnabled = youtubeHistoryEnabled,
                activeFocusProfile = focusProfile,
                breathingDelaySeconds = breathingDelay,
                studyStartHour = studyStart,
                studyEndHour = studyEnd,
                sleepStartHour = sleepStart,
                sleepEndHour = sleepEnd,
                intentionPromptEnabled = intentionPromptEnabled,
                appUsageTimersEnabled = appUsageTimersEnabled,
                askTimerOnAppLaunch = askTimerOnLaunch,
                defaultTimerMinutes = defaultTimerMinutes,
                showScreenTimeOnHome = showScreenTimeOnHome,
                dailyScreenTimeGoalMinutes = dailyScreenTimeGoal,
                showDailyIntention = showDailyIntention,
                dailyIntentionText = dailyIntentionText,
                showDailyQuote = showDailyQuote,
                autoUpdateEnabled = autoUpdateEnabled
            )
        }

    /**
     * Flow of per-application launch rules: Map<AppKey, AppLaunchRule>
     */
    val appLaunchRulesFlow: Flow<Map<String, AppLaunchRule>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val raw = prefs[PreferencesKeys.APP_LAUNCH_RULES] ?: ""
            if (raw.isEmpty()) {
                emptyMap()
            } else {
                val map = mutableMapOf<String, AppLaunchRule>()
                raw.split(":::").forEach { entry ->
                    val parts = entry.split("=")
                    if (parts.size == 2) {
                        try {
                            map[parts[0]] = AppLaunchRule.valueOf(parts[1])
                        } catch (_: Exception) {}
                    }
                }
                map
            }
        }

    /**
     * Flow of per-application daily usage limits in minutes: Map<AppKey, Int>
     */
    val appDailyLimitsFlow: Flow<Map<String, Int>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val raw = prefs[PreferencesKeys.APP_DAILY_LIMITS] ?: ""
            if (raw.isEmpty()) {
                emptyMap()
            } else {
                val map = mutableMapOf<String, Int>()
                raw.split(":::").forEach { entry ->
                    val parts = entry.split("=")
                    if (parts.size == 2) {
                        parts[1].toIntOrNull()?.let { minutes ->
                            if (minutes > 0) map[parts[0]] = minutes
                        }
                    }
                }
                map
            }
        }

    /**
     * Flow of user-defined custom app labels: Map<AppKey, CustomLabel>
     */
    val customAppNamesFlow: Flow<Map<String, String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val raw = prefs[PreferencesKeys.CUSTOM_APP_NAMES] ?: ""
            if (raw.isEmpty()) {
                emptyMap()
            } else {
                val map = mutableMapOf<String, String>()
                raw.split(":::").forEach { entry ->
                    val parts = entry.split("=", limit = 2)
                    if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                        map[parts[0]] = parts[1]
                    }
                }
                map
            }
        }

    /**
     * Flow of YouTube playback history (strictly local).
     */
    val youtubeHistoryFlow: Flow<List<YouTubeHistoryItem>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val jsonStr = prefs[PreferencesKeys.SETTING_YOUTUBE_HISTORY_JSON] ?: ""
            if (jsonStr.isEmpty()) {
                emptyList()
            } else {
                try {
                    val array = JSONArray(jsonStr)
                    val list = mutableListOf<YouTubeHistoryItem>()
                    for (i in 0 until array.length()) {
                        list.add(YouTubeHistoryItem.fromJson(array.getJSONObject(i)))
                    }
                    list
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

    /**
     * Flow of YouTube intentional watch later / learning queue (strictly local).
     */
    val youtubeQueueFlow: Flow<List<YouTubeQueueItem>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val jsonStr = prefs[PreferencesKeys.SETTING_YOUTUBE_QUEUE_JSON] ?: ""
            if (jsonStr.isEmpty()) {
                emptyList()
            } else {
                try {
                    val array = JSONArray(jsonStr)
                    val list = mutableListOf<YouTubeQueueItem>()
                    for (i in 0 until array.length()) {
                        list.add(YouTubeQueueItem.fromJson(array.getJSONObject(i)))
                    }
                    list
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

    /**
     * Flow of launch intentions history (up to 50 items stored locally).
     */
    val intentionsHistoryFlow: Flow<List<IntentionRecord>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val jsonStr = prefs[PreferencesKeys.INTENTIONS_HISTORY_JSON] ?: ""
            if (jsonStr.isEmpty()) {
                emptyList()
            } else {
                try {
                    val array = JSONArray(jsonStr)
                    val list = mutableListOf<IntentionRecord>()
                    for (i in 0 until array.length()) {
                        list.add(IntentionRecord.fromJson(array.getJSONObject(i)))
                    }
                    list
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

    suspend fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        val current = settingsFlow.first()
        val updated = transform(current)
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SETTING_STILL_THEME_MODE] = updated.themeMode.name
            prefs[PreferencesKeys.SETTING_STILL_ACCENT] = updated.accent.name
            prefs[PreferencesKeys.SETTING_SHOW_APP_ICONS] = updated.showAppIcons
            prefs[PreferencesKeys.SETTING_TEXT_SIZE] = updated.textSizeSp
            prefs[PreferencesKeys.SETTING_SHOW_CLOCK] = updated.showClock
            prefs[PreferencesKeys.SETTING_SHOW_DATE] = updated.showDate
            prefs[PreferencesKeys.SETTING_SHOW_BATTERY] = updated.showBattery
            prefs[PreferencesKeys.SETTING_CLOCK_24_HOUR] = updated.clock24Hour
            prefs[PreferencesKeys.SETTING_CLOCK_FONT_SIZE] = updated.clockFontSizeSp
            prefs[PreferencesKeys.SETTING_DOUBLE_TAP_TO_LOCK] = updated.doubleTapToLock
            prefs[PreferencesKeys.SETTING_INTERCEPT_YOUTUBE] = updated.interceptYouTube
            prefs[PreferencesKeys.SETTING_YOUTUBE_HISTORY_ENABLED] = updated.youtubeHistoryEnabled
            prefs[PreferencesKeys.SETTING_ACTIVE_FOCUS_PROFILE] = updated.activeFocusProfile.name
            prefs[PreferencesKeys.SETTING_BREATHING_DELAY_SECONDS] = updated.breathingDelaySeconds
            prefs[PreferencesKeys.SETTING_STUDY_START_HOUR] = updated.studyStartHour
            prefs[PreferencesKeys.SETTING_STUDY_END_HOUR] = updated.studyEndHour
            prefs[PreferencesKeys.SETTING_SLEEP_START_HOUR] = updated.sleepStartHour
            prefs[PreferencesKeys.SETTING_SLEEP_END_HOUR] = updated.sleepEndHour
            prefs[PreferencesKeys.SETTING_INTENTION_PROMPT_ENABLED] = updated.intentionPromptEnabled

            prefs[PreferencesKeys.SETTING_APP_USAGE_TIMERS_ENABLED] = updated.appUsageTimersEnabled
            prefs[PreferencesKeys.SETTING_ASK_TIMER_ON_LAUNCH] = updated.askTimerOnAppLaunch
            prefs[PreferencesKeys.SETTING_DEFAULT_TIMER_MINUTES] = updated.defaultTimerMinutes

            prefs[PreferencesKeys.SETTING_SHOW_SCREEN_TIME_ON_HOME] = updated.showScreenTimeOnHome
            prefs[PreferencesKeys.SETTING_DAILY_SCREEN_TIME_GOAL_MINUTES] = updated.dailyScreenTimeGoalMinutes
            prefs[PreferencesKeys.SETTING_SHOW_DAILY_INTENTION] = updated.showDailyIntention
            prefs[PreferencesKeys.SETTING_DAILY_INTENTION_TEXT] = updated.dailyIntentionText
            prefs[PreferencesKeys.SETTING_SHOW_DAILY_QUOTE] = updated.showDailyQuote
            prefs[PreferencesKeys.SETTING_AUTO_UPDATE_ENABLED] = updated.autoUpdateEnabled
        }
    }

    suspend fun getLastAutoUpdateCheckMs(): Long {
        return dataStore.data.catch { emit(emptyPreferences()) }.first()[PreferencesKeys.LAST_AUTO_UPDATE_CHECK_MS] ?: 0L
    }

    suspend fun setLastAutoUpdateCheckMs(timestamp: Long) {
        dataStore.edit { it[PreferencesKeys.LAST_AUTO_UPDATE_CHECK_MS] = timestamp }
    }

    suspend fun setAppLaunchRule(key: String, rule: AppLaunchRule) {
        dataStore.edit { prefs ->
            val raw = prefs[PreferencesKeys.APP_LAUNCH_RULES] ?: ""
            val map = mutableMapOf<String, String>()
            if (raw.isNotEmpty()) {
                raw.split(":::").forEach { entry ->
                    val parts = entry.split("=")
                    if (parts.size == 2) map[parts[0]] = parts[1]
                }
            }
            if (rule == AppLaunchRule.NORMAL) {
                map.remove(key)
            } else {
                map[key] = rule.name
            }
            val serialized = map.entries.joinToString(":::") { "${it.key}=${it.value}" }
            prefs[PreferencesKeys.APP_LAUNCH_RULES] = serialized

            // If rule is LIMITED and no limit is configured yet, default to 30 minutes
            if (rule == AppLaunchRule.LIMITED) {
                val limitsRaw = prefs[PreferencesKeys.APP_DAILY_LIMITS] ?: ""
                val limitsMap = mutableMapOf<String, Int>()
                if (limitsRaw.isNotEmpty()) {
                    limitsRaw.split(":::").forEach { entry ->
                        val parts = entry.split("=")
                        if (parts.size == 2) {
                            parts[1].toIntOrNull()?.let { limitsMap[parts[0]] = it }
                        }
                    }
                }
                if (!limitsMap.containsKey(key)) {
                    limitsMap[key] = 30
                    val serializedLimits = limitsMap.entries.joinToString(":::") { "${it.key}=${it.value}" }
                    prefs[PreferencesKeys.APP_DAILY_LIMITS] = serializedLimits
                }
            }
        }
    }

    suspend fun setAppDailyLimit(key: String, minutes: Int) {
        dataStore.edit { prefs ->
            val raw = prefs[PreferencesKeys.APP_DAILY_LIMITS] ?: ""
            val map = mutableMapOf<String, Int>()
            if (raw.isNotEmpty()) {
                raw.split(":::").forEach { entry ->
                    val parts = entry.split("=")
                    if (parts.size == 2) {
                        parts[1].toIntOrNull()?.let { map[parts[0]] = it }
                    }
                }
            }
            if (minutes <= 0) {
                map.remove(key)
            } else {
                map[key] = minutes
            }
            val serialized = map.entries.joinToString(":::") { "${it.key}=${it.value}" }
            prefs[PreferencesKeys.APP_DAILY_LIMITS] = serialized
        }
    }

    suspend fun setCustomAppName(uniqueKey: String, customName: String?) {
        dataStore.edit { prefs ->
            val raw = prefs[PreferencesKeys.CUSTOM_APP_NAMES] ?: ""
            val map = mutableMapOf<String, String>()
            if (raw.isNotEmpty()) {
                raw.split(":::").forEach { entry ->
                    val parts = entry.split("=", limit = 2)
                    if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                        map[parts[0]] = parts[1]
                    }
                }
            }
            val trimmedName = customName?.trim()
            if (trimmedName.isNullOrEmpty()) {
                map.remove(uniqueKey)
            } else {
                map[uniqueKey] = trimmedName
            }
            val serialized = map.entries.joinToString(":::") { "${it.key}=${it.value}" }
            prefs[PreferencesKeys.CUSTOM_APP_NAMES] = serialized
        }
    }

    suspend fun addIntentionRecord(record: IntentionRecord) {
        dataStore.edit { prefs ->
            val jsonStr = prefs[PreferencesKeys.INTENTIONS_HISTORY_JSON] ?: ""
            val list = mutableListOf<IntentionRecord>()
            if (jsonStr.isNotEmpty()) {
                try {
                    val array = JSONArray(jsonStr)
                    for (i in 0 until array.length()) {
                        list.add(IntentionRecord.fromJson(array.getJSONObject(i)))
                    }
                } catch (_: Exception) {}
            }
            list.add(0, record)
            val trimmed = list.take(50)
            val jsonArray = JSONArray()
            trimmed.forEach { jsonArray.put(it.toJson()) }
            prefs[PreferencesKeys.INTENTIONS_HISTORY_JSON] = jsonArray.toString()
        }
    }

    suspend fun clearIntentionsHistory() {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.INTENTIONS_HISTORY_JSON] = ""
        }
    }

    suspend fun addYouTubeHistoryItem(item: YouTubeHistoryItem) {
        dataStore.edit { prefs ->
            val isEnabled = prefs[PreferencesKeys.SETTING_YOUTUBE_HISTORY_ENABLED] ?: true
            if (!isEnabled) return@edit

            val jsonStr = prefs[PreferencesKeys.SETTING_YOUTUBE_HISTORY_JSON] ?: ""
            val list = mutableListOf<YouTubeHistoryItem>()
            if (jsonStr.isNotEmpty()) {
                try {
                    val array = JSONArray(jsonStr)
                    for (i in 0 until array.length()) {
                        list.add(YouTubeHistoryItem.fromJson(array.getJSONObject(i)))
                    }
                } catch (_: Exception) {}
            }
            // Remove existing with same id to deduplicate and move to top
            list.removeAll { it.id == item.id }
            list.add(0, item)
            // Limit history to 30 items
            val trimmed = list.take(30)
            val jsonArray = JSONArray()
            trimmed.forEach { jsonArray.put(it.toJson()) }
            prefs[PreferencesKeys.SETTING_YOUTUBE_HISTORY_JSON] = jsonArray.toString()
        }
    }

    suspend fun removeYouTubeHistoryItem(id: String) {
        dataStore.edit { prefs ->
            val jsonStr = prefs[PreferencesKeys.SETTING_YOUTUBE_HISTORY_JSON] ?: ""
            if (jsonStr.isNotEmpty()) {
                try {
                    val array = JSONArray(jsonStr)
                    val list = mutableListOf<YouTubeHistoryItem>()
                    for (i in 0 until array.length()) {
                        val item = YouTubeHistoryItem.fromJson(array.getJSONObject(i))
                        if (item.id != id) list.add(item)
                    }
                    val jsonArray = JSONArray()
                    list.forEach { jsonArray.put(it.toJson()) }
                    prefs[PreferencesKeys.SETTING_YOUTUBE_HISTORY_JSON] = jsonArray.toString()
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun clearYouTubeHistory() {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SETTING_YOUTUBE_HISTORY_JSON] = ""
        }
    }

    suspend fun addToQueue(item: YouTubeQueueItem) {
        dataStore.edit { prefs ->
            val jsonStr = prefs[PreferencesKeys.SETTING_YOUTUBE_QUEUE_JSON] ?: ""
            val list = mutableListOf<YouTubeQueueItem>()
            if (jsonStr.isNotEmpty()) {
                try {
                    val array = JSONArray(jsonStr)
                    for (i in 0 until array.length()) {
                        list.add(YouTubeQueueItem.fromJson(array.getJSONObject(i)))
                    }
                } catch (_: Exception) {}
            }
            list.removeAll { it.id == item.id }
            list.add(0, item)
            val trimmed = list.take(50)
            val jsonArray = JSONArray()
            trimmed.forEach { jsonArray.put(it.toJson()) }
            prefs[PreferencesKeys.SETTING_YOUTUBE_QUEUE_JSON] = jsonArray.toString()
        }
    }

    suspend fun removeFromQueue(id: String) {
        dataStore.edit { prefs ->
            val jsonStr = prefs[PreferencesKeys.SETTING_YOUTUBE_QUEUE_JSON] ?: ""
            if (jsonStr.isNotEmpty()) {
                try {
                    val array = JSONArray(jsonStr)
                    val list = mutableListOf<YouTubeQueueItem>()
                    for (i in 0 until array.length()) {
                        val item = YouTubeQueueItem.fromJson(array.getJSONObject(i))
                        if (item.id != id) list.add(item)
                    }
                    val jsonArray = JSONArray()
                    list.forEach { jsonArray.put(it.toJson()) }
                    prefs[PreferencesKeys.SETTING_YOUTUBE_QUEUE_JSON] = jsonArray.toString()
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun clearQueue() {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SETTING_YOUTUBE_QUEUE_JSON] = ""
        }
    }

    suspend fun saveFavorites(keys: List<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FAVORITE_KEYS_ORDERED] = keys.joinToString(":::")
        }
    }

    suspend fun addFavorite(key: String) {
        val current = favoriteKeysFlow.first()
        if (!current.contains(key)) {
            val updated = current + key
            saveFavorites(updated)
        }
    }

    suspend fun removeFavorite(key: String) {
        val current = favoriteKeysFlow.first()
        if (current.contains(key)) {
            val updated = current.filter { it != key }
            saveFavorites(updated)
        }
    }

    suspend fun reorderFavorite(fromIndex: Int, toIndex: Int) {
        val current = favoriteKeysFlow.first().toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            saveFavorites(current)
        }
    }

    suspend fun toggleHidden(key: String) {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.HIDDEN_KEYS] ?: emptySet()
            preferences[PreferencesKeys.HIDDEN_KEYS] = if (current.contains(key)) {
                current - key
            } else {
                current + key
            }
        }
    }

    suspend fun setInitialSeeded(seeded: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_INITIAL_SEEDED] = seeded
        }
    }
}
