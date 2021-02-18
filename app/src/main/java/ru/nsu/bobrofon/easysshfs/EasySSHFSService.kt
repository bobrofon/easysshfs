package ru.nsu.bobrofon.easysshfs

import android.app.*
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log

private const val TAG = "EasySSHFSService"
private const val CHANNEL_ID = "Channel Mount"
private const val CHANNEL_NAME = "Mount"
private const val NOTIFICATION_ID = 1

class EasySSHFSService : Service() {

    private val handler =  Handler()
    private val internetStateChangeReceiver = InternetStateChangeReceiver(handler)
    private val internetStateChangeFilter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)

    private val notification: Notification by lazy {
        Notification.Builder(applicationContext).apply {
            setSmallIcon(R.mipmap.ic_launcher)

            val notificationIntent = Intent(applicationContext, EasySSHFSActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, notificationIntent, 0)
            setContentIntent(pendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setChannelId(CHANNEL_ID)
            }
        }.notification
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
}