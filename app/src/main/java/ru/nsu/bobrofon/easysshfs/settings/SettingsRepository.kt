// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
}
