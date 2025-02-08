// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.settings

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

private const val TAG = "SettingsViewModel"

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val _autoMountInForegroundService = MutableLiveData<Boolean>()
    val autoMountInForegroundService: LiveData<Boolean> get() = _autoMountInForegroundService

    private val _themeMode = MutableLiveData<ThemeMode>()
    val themeMode: LiveData<ThemeMode> get() = _themeMode

    init {
        viewModelScope.launch {
            repository.autoMountInForegroundService.collect {
                _autoMountInForegroundService.value = it
            }
        }

        viewModelScope.launch {
            repository.themeMode.collect { mode ->
                _themeMode.value = mode
            }
        }
    }

    fun setAutoMountInForegroundService(value: Boolean) {
        viewModelScope.launch {
            repository.setAutoMountInForegroundService(value)
        }
    }

    private fun setThemeMode(value: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(value)
        }
    }

    fun setThemeMode(value: String) {
        val mode = ThemeMode.fromPreferenceValue(value)
        if (mode != null) {
            setThemeMode(mode)
        } else {
            Log.w(TAG, "unexpected theme mode '$value'")
        }
    }

    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
