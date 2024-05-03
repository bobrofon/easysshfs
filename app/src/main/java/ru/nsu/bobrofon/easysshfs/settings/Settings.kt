// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Global application settings.
 */
object Settings {
    /**
     * Start foreground service to automatically mount/umount mountpoints on network changes.
     */
    val autoMountInForegroundService = booleanPreferencesKey("autoMountInForegroundService")

    /**
     * Check remote ssh services periodically to automatically mount mountpoints.
     */
    val checkSshServersPeriodically = booleanPreferencesKey("checkSshServersPeriodically")
}
