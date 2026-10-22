package com.niagaraclone.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_prefs")

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val FAVORITE_KEYS_ORDERED = stringPreferencesKey("favorite_keys_ordered")
        val HIDDEN_KEYS = stringSetPreferencesKey("hidden_keys")
        val IS_INITIAL_SEEDED = booleanPreferencesKey("is_initial_seeded")
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
