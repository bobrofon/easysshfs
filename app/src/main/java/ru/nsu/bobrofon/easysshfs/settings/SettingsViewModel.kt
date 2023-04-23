// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.settings

import androidx.lifecycle.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val _autoMountInForegroundService = MutableLiveData<Boolean>()
    val autoMountInForegroundService: LiveData<Boolean> get() = _autoMountInForegroundService

    init {
        viewModelScope.launch {
            repository.autoMountInForegroundService.collect {
                _autoMountInForegroundService.value = it
            }
        }
    }

    fun setAutoMountInForegroundService(value: Boolean) {
        viewModelScope.launch {
            repository.setAutoMountInForegroundService(value)
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
