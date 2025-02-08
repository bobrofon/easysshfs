// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.settings

import android.app.UiModeManager
import androidx.appcompat.app.AppCompatDelegate

enum class ThemeMode(
    val nightMode: Int
) {
    DEFAULT(UiModeManager.MODE_NIGHT_AUTO),
    LIGHT(UiModeManager.MODE_NIGHT_NO),
    DARK(UiModeManager.MODE_NIGHT_YES);

    val preferenceValue: String get() = name.lowercase()

    val compatNightMode: Int
        get() = when (nightMode) {
            UiModeManager.MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_NO
            UiModeManager.MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

    companion object {
        fun fromPreferenceValue(key: String): ThemeMode? =
            values().find { mode -> mode.preferenceValue == key }
    }
}
