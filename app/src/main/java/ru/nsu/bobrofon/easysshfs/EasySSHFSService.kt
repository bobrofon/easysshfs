// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

private const val TAG = "EasySSHFSService"
private const val CHANNEL_ID = "Channel Mount"
private const val CHANNEL_NAME = "Mount"
private const val NOTIFICATION_ID = 1

class EasySSHFSService : Service() {

    private val handler =  Handler(Looper.getMainLooper())
    private val internetStateChangeReceiver = InternetStateChangeReceiver(handler)
    private val internetStateChangeFilter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)

    private val notification: Notification by lazy {
        NotificationCompat.Builder(applicationContext, CHANNEL_ID).apply {
            setSmallIcon(R.mipmap.ic_launcher)

            val notificationIntent = Intent(applicationContext, EasySSHFSActivity::class.java)
            val intentFLags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, notificationIntent, intentFLags)
            setContentIntent(pendingIntent)
        }.build()
    }
    private val notificationManager: NotificationManager by lazy {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "register NETWORK_STATE_CHANGED receiver")
        registerReceiver(internetStateChangeReceiver, internetStateChangeFilter)
    }

    override fun onDestroy() {
        Log.d(TAG, "unregister NETWORK_STATE_CHANGED receiver")
        unregisterReceiver(internetStateChangeReceiver)
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        notificationManager.notify(NOTIFICATION_ID, notification)
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, EasySSHFSService::class.java))
            } else {
                context.startService(Intent(context, EasySSHFSService::class.java))
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, EasySSHFSService::class.java))
        }
    }
}