// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.nsu.bobrofon.easysshfs.mountpointlist.AutoMountChangeObserver
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList
import ru.nsu.bobrofon.easysshfs.settings.SettingsRepository
import ru.nsu.bobrofon.easysshfs.settings.ThemeMode

class EasySSHFSViewModel(
    autoMountInForegroundService: Flow<Boolean>,
    themeMode: Flow<ThemeMode>,
    private val mountPointsList: MountPointsList
) : ViewModel() {
    private var foregroundServiceAllowed = false
    private var autoMountEnabled = false

    private val _autoMountServiceRequired = MutableLiveData<Boolean>()
    val autoMountServiceRequired: LiveData<Boolean> get() = _autoMountServiceRequired

    private val _themeMode = MutableLiveData<ThemeMode>()
    val themeMode: LiveData<ThemeMode> get() = _themeMode

    private val autoMountChangeObserver = object : AutoMountChangeObserver {
        override fun onAutoMountChanged(isAutoMountRequired: Boolean) {
            autoMountEnabled = isAutoMountRequired
            updateAutoMountServiceRequired()
        }
    }

    init {
        viewModelScope.launch {
            autoMountInForegroundService.collect {
                foregroundServiceAllowed = it
                updateAutoMountServiceRequired()
            }
            autoMountInForegroundService.first()
        }

        viewModelScope.launch {
            themeMode.collect {
                _themeMode.value = it
            }
        }

        mountPointsList.registerAutoMountObserver(autoMountChangeObserver)
    }

    override fun onCleared() {
        mountPointsList.unregisterAutoMountObserver(autoMountChangeObserver)
        super.onCleared()
    }

    private fun updateAutoMountServiceRequired() {
        _autoMountServiceRequired.value = autoMountEnabled && foregroundServiceAllowed
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val mountPointsList: MountPointsList
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EasySSHFSViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return EasySSHFSViewModel(
                    settingsRepository.autoMountInForegroundService,
                    settingsRepository.themeMode,
                    mountPointsList
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
