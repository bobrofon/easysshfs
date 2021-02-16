package ru.nsu.bobrofon.easysshfs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList

private const val TAG = "OnBootReceiver"

class OnBootReceiver : BroadcastReceiver() {

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(Intent(context, EasySSHFSService::class.java))
        } else {
            context.startService(Intent(context, EasySSHFSService::class.java))
        }
    }
}
