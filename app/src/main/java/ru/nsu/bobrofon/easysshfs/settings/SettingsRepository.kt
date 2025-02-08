// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.settings

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "SettingsRepository"

class SettingsRepository(private val settingsDataStore: DataStore<Preferences>) {
    val autoMountInForegroundService: Flow<Boolean>
        get() = settingsDataStore.data.map { settings ->
            settings[Settings.autoMountInForegroundService] ?: false
        }

    suspend fun setAutoMountInForegroundService(value: Boolean) {
        settingsDataStore.edit { settings ->
            settings[Settings.autoMountInForegroundService] = value
        }
    }

    val themeMode: Flow<ThemeMode>
        get() = settingsDataStore.data.map { settings ->
            val modeValue = settings[Settings.themeMode] ?: return@map ThemeMode.DEFAULT

            val mode = ThemeMode.fromPreferenceValue(modeValue)
            if (mode != null) {
                mode
            } else {
                Log.w(TAG, "unknown theme mode '$modeValue', use default mode instead")
                ThemeMode.DEFAULT
            }
        }

    suspend fun setThemeMode(value: ThemeMode) {
        settingsDataStore.edit { settings ->
            settings[Settings.themeMode] = value.preferenceValue
        }
    }
}
