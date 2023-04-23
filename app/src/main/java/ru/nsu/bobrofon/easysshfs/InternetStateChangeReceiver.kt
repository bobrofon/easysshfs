// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.util.Log
import com.topjohnwu.superuser.Shell

import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList

private const val TAG = "InternetStateChange"
private const val AUTO_MOUNT_DELAY_MILLIS: Long = 5000

class InternetStateChangeReceiver(
    private val handler: Handler
) : BroadcastReceiver() {

    private val shell: Shell by lazy { EasySSHFSActivity.initNewShell() }

    override fun onReceive(context: Context, intent: Intent) {
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION != intent.action) {
            return
        }

        Log.d(TAG, "network state changed")

        val info = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO) ?: return
        val mountPointsList = MountPointsList.instance(context)

        handler.removeCallbacksAndMessages(null) // ignore repeated intents
        if (info.isConnected) {
            Log.d(TAG, "network is connected")
            handler.postDelayed({autoMount(mountPointsList, shell)}, AUTO_MOUNT_DELAY_MILLIS)
        } else {
            Log.d(TAG, "unmount everything")
            handler.post { forceUmount(mountPointsList, shell) }
        }
    }

    private fun autoMount(mountPointsList: MountPointsList, shell: Shell) {
        Log.d(TAG, "check auto-mount")
        if (mountPointsList.needAutomount()) {
            Log.d(TAG, "auto-mount required")
            mountPointsList.autoMount(shell)
        }
    }

    private fun forceUmount(mountPointsList: MountPointsList, shell: Shell) {
        Log.d(TAG, "force umount everything")
        mountPointsList.umount(shell)
    }
}
