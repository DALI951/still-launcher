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
import com.niagaraclone.launcher.data.model.ClockStyle
import com.niagaraclone.launcher.data.model.IconShape
import com.niagaraclone.launcher.data.model.IconStyle
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.data.model.ThemePreset
import com.niagaraclone.launcher.data.model.WaveVisibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_prefs")

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val FAVORITE_KEYS_ORDERED = stringPreferencesKey("favorite_keys_ordered")
        val HIDDEN_KEYS = stringSetPreferencesKey("hidden_keys")
        val IS_INITIAL_SEEDED = booleanPreferencesKey("is_initial_seeded")

        // Customization & Sizing Settings
        val SETTING_ICON_SIZE = intPreferencesKey("setting_icon_size")
        val SETTING_ICON_STYLE = stringPreferencesKey("setting_icon_style")
        val SETTING_ICON_SHAPE = stringPreferencesKey("setting_icon_shape")
        val SETTING_THEME_PRESET = stringPreferencesKey("setting_theme_preset")
        val SETTING_EDGE_PADDING = intPreferencesKey("setting_edge_padding")
        val SETTING_ROW_HEIGHT = intPreferencesKey("setting_row_height")
        val SETTING_TEXT_SIZE = intPreferencesKey("setting_text_size")
        val SETTING_FAV_TOP_OFFSET = intPreferencesKey("setting_fav_top_offset")
        val SETTING_SCRIM_OPACITY = intPreferencesKey("setting_scrim_opacity")
        val SETTING_SHOW_CLOCK = booleanPreferencesKey("setting_show_clock")
        val SETTING_SHOW_DATE = booleanPreferencesKey("setting_show_date")
        val SETTING_SHOW_BATTERY_ALARM = booleanPreferencesKey("setting_show_battery_alarm")
        val SETTING_WAVE_LEFT_SIDE = booleanPreferencesKey("setting_wave_left_side")

        // Phase 6 Clock & Wave Ergonomics
        val SETTING_CLOCK_STYLE = stringPreferencesKey("setting_clock_style")
        val SETTING_CLOCK_FONT_SIZE = intPreferencesKey("setting_clock_font_size")
        val SETTING_WAVE_VISIBILITY = stringPreferencesKey("setting_wave_visibility")
        val SETTING_WAVE_HEIGHT_PERCENT = intPreferencesKey("setting_wave_height_percent")
        val SETTING_WAVE_LETTER_SIZE = intPreferencesKey("setting_wave_letter_size")
    }

    private val dataStore = context.launcherDataStore

    /**
     * Flow of ordered favorite app keys.
     */
    val favoriteKeysFlow: Flow<List<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val serialized = preferences[PreferencesKeys.FAVORITE_KEYS_ORDERED] ?: ""
            if (serialized.isEmpty()) {
                emptyList()
            } else {
                serialized.split(":::").filter { it.isNotEmpty() }
            }
        }

    /**
     * Flow of hidden app keys.
     */
    val hiddenKeysFlow: Flow<Set<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.HIDDEN_KEYS] ?: emptySet()
        }

    /**
     * Flag indicating if initial default favorites have been seeded.
     */
    val isInitialSeededFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_INITIAL_SEEDED] ?: false
        }

    /**
     * Flow of user customization settings.
     */
    val settingsFlow: Flow<LauncherSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val iconSize = prefs[PreferencesKeys.SETTING_ICON_SIZE] ?: 26
            val iconStyleName = prefs[PreferencesKeys.SETTING_ICON_STYLE] ?: IconStyle.THEMED_MONOCHROME.name
            val iconStyle = try { IconStyle.valueOf(iconStyleName) } catch (e: Exception) { IconStyle.THEMED_MONOCHROME }
            val iconShapeName = prefs[PreferencesKeys.SETTING_ICON_SHAPE] ?: IconShape.CIRCLE.name
            val iconShape = try { IconShape.valueOf(iconShapeName) } catch (e: Exception) { IconShape.CIRCLE }
            val themePresetName = prefs[PreferencesKeys.SETTING_THEME_PRESET] ?: ThemePreset.NIAGARA_BLUE.name
            val themePreset = try { ThemePreset.valueOf(themePresetName) } catch (e: Exception) { ThemePreset.NIAGARA_BLUE }
            val edgePadding = prefs[PreferencesKeys.SETTING_EDGE_PADDING] ?: 44
            val rowHeight = prefs[PreferencesKeys.SETTING_ROW_HEIGHT] ?: 46
            val textSize = prefs[PreferencesKeys.SETTING_TEXT_SIZE] ?: 15
            val favTopOffset = prefs[PreferencesKeys.SETTING_FAV_TOP_OFFSET] ?: 56
            val scrimOpacity = prefs[PreferencesKeys.SETTING_SCRIM_OPACITY] ?: 55
            val showClock = prefs[PreferencesKeys.SETTING_SHOW_CLOCK] ?: true
            val showDate = prefs[PreferencesKeys.SETTING_SHOW_DATE] ?: true
            val showBatteryAlarm = prefs[PreferencesKeys.SETTING_SHOW_BATTERY_ALARM] ?: true
            val waveLeftSide = prefs[PreferencesKeys.SETTING_WAVE_LEFT_SIDE] ?: false

            val clockStyleName = prefs[PreferencesKeys.SETTING_CLOCK_STYLE] ?: ClockStyle.NIAGARA_CLEAN.name
            val clockStyle = try { ClockStyle.valueOf(clockStyleName) } catch (e: Exception) { ClockStyle.NIAGARA_CLEAN }
            val clockFontSize = prefs[PreferencesKeys.SETTING_CLOCK_FONT_SIZE] ?: 54
            val waveVisibilityName = prefs[PreferencesKeys.SETTING_WAVE_VISIBILITY] ?: WaveVisibility.ALWAYS_VISIBLE.name
            val waveVisibility = try { WaveVisibility.valueOf(waveVisibilityName) } catch (e: Exception) { WaveVisibility.ALWAYS_VISIBLE }
            val waveHeightPercent = prefs[PreferencesKeys.SETTING_WAVE_HEIGHT_PERCENT] ?: 68
            val waveLetterSize = prefs[PreferencesKeys.SETTING_WAVE_LETTER_SIZE] ?: 10

            LauncherSettings(
                iconSizeDp = iconSize,
                iconStyle = iconStyle,
                iconShape = iconShape,
                themePreset = themePreset,
                edgePaddingDp = edgePadding,
                rowHeightDp = rowHeight,
                textSizeSp = textSize,
                favoritesTopOffsetDp = favTopOffset,
                scrimOpacityPercent = scrimOpacity,
                showClock = showClock,
                showDate = showDate,
                showBatteryAndAlarm = showBatteryAlarm,
                waveOnLeftSide = waveLeftSide,
                clockStyle = clockStyle,
                clockFontSizeSp = clockFontSize,
                waveVisibility = waveVisibility,
                waveHeightPercent = waveHeightPercent,
                waveLetterSizeSp = waveLetterSize
            )
        }

    /**
     * Updates launcher settings via a transform lambda.
     */
    suspend fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        val current = settingsFlow.first()
        val updated = transform(current)

        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SETTING_ICON_SIZE] = updated.iconSizeDp
            prefs[PreferencesKeys.SETTING_ICON_STYLE] = updated.iconStyle.name
            prefs[PreferencesKeys.SETTING_ICON_SHAPE] = updated.iconShape.name
            prefs[PreferencesKeys.SETTING_THEME_PRESET] = updated.themePreset.name
            prefs[PreferencesKeys.SETTING_EDGE_PADDING] = updated.edgePaddingDp
            prefs[PreferencesKeys.SETTING_ROW_HEIGHT] = updated.rowHeightDp
            prefs[PreferencesKeys.SETTING_TEXT_SIZE] = updated.textSizeSp
            prefs[PreferencesKeys.SETTING_FAV_TOP_OFFSET] = updated.favoritesTopOffsetDp
            prefs[PreferencesKeys.SETTING_SCRIM_OPACITY] = updated.scrimOpacityPercent
            prefs[PreferencesKeys.SETTING_SHOW_CLOCK] = updated.showClock
            prefs[PreferencesKeys.SETTING_SHOW_DATE] = updated.showDate
            prefs[PreferencesKeys.SETTING_SHOW_BATTERY_ALARM] = updated.showBatteryAndAlarm
            prefs[PreferencesKeys.SETTING_WAVE_LEFT_SIDE] = updated.waveOnLeftSide
            prefs[PreferencesKeys.SETTING_CLOCK_STYLE] = updated.clockStyle.name
            prefs[PreferencesKeys.SETTING_CLOCK_FONT_SIZE] = updated.clockFontSizeSp
            prefs[PreferencesKeys.SETTING_WAVE_VISIBILITY] = updated.waveVisibility.name
            prefs[PreferencesKeys.SETTING_WAVE_HEIGHT_PERCENT] = updated.waveHeightPercent
            prefs[PreferencesKeys.SETTING_WAVE_LETTER_SIZE] = updated.waveLetterSizeSp
        }
    }

    /**
     * Resets all visual and ergonomic settings to default Niagara layout.
     */
    suspend fun resetSettingsToDefault() {
        dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.SETTING_ICON_SIZE)
            prefs.remove(PreferencesKeys.SETTING_ICON_STYLE)
            prefs.remove(PreferencesKeys.SETTING_ICON_SHAPE)
            prefs.remove(PreferencesKeys.SETTING_THEME_PRESET)
            prefs.remove(PreferencesKeys.SETTING_EDGE_PADDING)
            prefs.remove(PreferencesKeys.SETTING_ROW_HEIGHT)
            prefs.remove(PreferencesKeys.SETTING_TEXT_SIZE)
            prefs.remove(PreferencesKeys.SETTING_FAV_TOP_OFFSET)
            prefs.remove(PreferencesKeys.SETTING_SCRIM_OPACITY)
            prefs.remove(PreferencesKeys.SETTING_SHOW_CLOCK)
            prefs.remove(PreferencesKeys.SETTING_SHOW_DATE)
            prefs.remove(PreferencesKeys.SETTING_SHOW_BATTERY_ALARM)
            prefs.remove(PreferencesKeys.SETTING_WAVE_LEFT_SIDE)
            prefs.remove(PreferencesKeys.SETTING_CLOCK_STYLE)
            prefs.remove(PreferencesKeys.SETTING_CLOCK_FONT_SIZE)
            prefs.remove(PreferencesKeys.SETTING_WAVE_VISIBILITY)
            prefs.remove(PreferencesKeys.SETTING_WAVE_HEIGHT_PERCENT)
            prefs.remove(PreferencesKeys.SETTING_WAVE_LETTER_SIZE)
        }
    }

    /**
     * Sets or overrides the complete list of favorites in exact order.
     */
    suspend fun setFavorites(keys: List<String>) {
        val serialized = keys.filter { it.isNotEmpty() }.joinToString(":::")
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FAVORITE_KEYS_ORDERED] = serialized
        }
    }

    /**
     * Adds an app key to the favorites list.
     */
    suspend fun addFavorite(key: String) {
        dataStore.edit { preferences ->
            val serialized = preferences[PreferencesKeys.FAVORITE_KEYS_ORDERED] ?: ""
            val list = if (serialized.isEmpty()) {
                mutableListOf()
            } else {
                serialized.split(":::").filter { it.isNotEmpty() }.toMutableList()
            }
            if (!list.contains(key)) {
                list.add(key)
                preferences[PreferencesKeys.FAVORITE_KEYS_ORDERED] = list.joinToString(":::")
            }
        }
    }

    /**
     * Removes an app key from favorites.
     */
    suspend fun removeFavorite(key: String) {
        dataStore.edit { preferences ->
            val serialized = preferences[PreferencesKeys.FAVORITE_KEYS_ORDERED] ?: ""
            val list = serialized.split(":::").filter { it.isNotEmpty() }.toMutableList()
            if (list.remove(key)) {
                preferences[PreferencesKeys.FAVORITE_KEYS_ORDERED] = list.joinToString(":::")
            }
        }
    }

    /**
     * Reorders a favorite item up (-1) or down (+1).
     */
    suspend fun moveFavorite(key: String, delta: Int) {
        dataStore.edit { preferences ->
            val serialized = preferences[PreferencesKeys.FAVORITE_KEYS_ORDERED] ?: ""
            val list = serialized.split(":::").filter { it.isNotEmpty() }.toMutableList()
            val currentIndex = list.indexOf(key)
            if (currentIndex != -1) {
                val newIndex = (currentIndex + delta).coerceIn(0, list.lastIndex)
                if (newIndex != currentIndex) {
                    list.removeAt(currentIndex)
                    list.add(newIndex, key)
                    preferences[PreferencesKeys.FAVORITE_KEYS_ORDERED] = list.joinToString(":::")
                }
            }
        }
    }

    /**
     * Marks an app as hidden. Also removes it from favorites if pinned.
     */
    suspend fun hideApp(key: String) {
        dataStore.edit { preferences ->
            val currentHidden = preferences[PreferencesKeys.HIDDEN_KEYS]?.toMutableSet() ?: mutableSetOf()
            currentHidden.add(key)
            preferences[PreferencesKeys.HIDDEN_KEYS] = currentHidden

            // Remove from favorites if present
            val serialized = preferences[PreferencesKeys.FAVORITE_KEYS_ORDERED] ?: ""
            val list = serialized.split(":::").filter { it.isNotEmpty() }.toMutableList()
            if (list.remove(key)) {
                preferences[PreferencesKeys.FAVORITE_KEYS_ORDERED] = list.joinToString(":::")
            }
        }
    }

    /**
     * Unhides an app.
     */
    suspend fun unhideApp(key: String) {
        dataStore.edit { preferences ->
            val currentHidden = preferences[PreferencesKeys.HIDDEN_KEYS]?.toMutableSet() ?: mutableSetOf()
            if (currentHidden.remove(key)) {
                preferences[PreferencesKeys.HIDDEN_KEYS] = currentHidden
            }
        }
    }

    /**
     * Marks that default favorites have been seeded.
     */
    suspend fun markInitialSeeded() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_INITIAL_SEEDED] = true
        }
    }
}
