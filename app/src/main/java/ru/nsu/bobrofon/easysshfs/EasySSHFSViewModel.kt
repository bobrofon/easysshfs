// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs

import androidx.lifecycle.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.nsu.bobrofon.easysshfs.mountpointlist.AutoMountChangeObserver
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList
import ru.nsu.bobrofon.easysshfs.settings.SettingsRepository

class EasySSHFSViewModel(
    autoMountInForegroundService: Flow<Boolean>,
    private val mountPointsList: MountPointsList
) : ViewModel() {
    private var foregroundServiceAllowed = false
    private var autoMountEnabled = false

    private val _autoMountServiceRequired = MutableLiveData<Boolean>()
    val autoMountServiceRequired: LiveData<Boolean> get() = _autoMountServiceRequired

    private val autoMountChangeObserver = object: AutoMountChangeObserver {
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
                    mountPointsList
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
