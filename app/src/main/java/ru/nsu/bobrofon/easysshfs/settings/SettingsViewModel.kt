// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.settings

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val _autoMountInForegroundService = MutableLiveData<Boolean>()
    val autoMountInForegroundService: LiveData<Boolean> get() = _autoMountInForegroundService

    private val _checkSshServersPeriodically = MutableLiveData<Boolean>()
    val checkSshServersPeriodically: LiveData<Boolean> get() = _checkSshServersPeriodically

    init {
        viewModelScope.launch {
            repository.autoMountInForegroundService.collect {
                _autoMountInForegroundService.value = it
            }
        }

        viewModelScope.launch {
            repository.checkSshServersPeriodically.collect {
                _checkSshServersPeriodically.value = it
            }
        }
    }

    fun setAutoMountInForegroundService(value: Boolean) {
        viewModelScope.launch {
            repository.setAutoMountInForegroundService(value)
        }
    }

    fun setCheckSshServersPeriodically(value: Boolean) {
        viewModelScope.launch {
            repository.setCheckSshServersPeriodically(value)
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
