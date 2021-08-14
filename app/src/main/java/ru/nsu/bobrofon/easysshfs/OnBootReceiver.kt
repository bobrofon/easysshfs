// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList
import ru.nsu.bobrofon.easysshfs.settings.SettingsRepository
import ru.nsu.bobrofon.easysshfs.settings.settingsDataStore

private const val TAG = "OnBootReceiver"

class OnBootReceiver : BroadcastReceiver() {
    private val receiverScope = CoroutineScope(Dispatchers.Main.immediate)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED != intent?.action) {
            return
        }
        Log.d(TAG, "on boot received")

        if (context == null) {
            Log.i(TAG, "context is not available")
            return
        }

        if (!MountPointsList.instance(context).isAutoMountEnabled) {
            Log.d(TAG, "automount is not enabled")
            return
        }

        val settingsRepository = SettingsRepository(context.applicationContext.settingsDataStore)
        goAsync {
            if (settingsRepository.autoMountInForegroundService.firstOrNull() == true) {
                EasySSHFSService.start(context)
            }
        }
    }

    private fun goAsync(
        coroutineScope: CoroutineScope = receiverScope,
        dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
        block: suspend () -> Unit
    ) {
        val pendingResult = goAsync()
        coroutineScope.launch(dispatcher) {
            block()
            pendingResult.finish()
        }
    }
}
