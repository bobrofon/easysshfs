package ru.nsu.bobrofon.easysshfs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiManager

import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList

class InternetStateChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION != intent.action) {
            return
        }

        val info = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO) ?: return
        val mountPointsList = MountPointsList.instance(context)
        val shell = EasySSHFSActivity.initNewShell()

        if (info.isConnected) {
            mountPointsList.checkMount()
            if (mountPointsList.needAutomount()) {
                mountPointsList.autoMount(shell)
            }
        } else {
            mountPointsList.umount(shell)
        }
    }
}
