// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList
import ru.nsu.bobrofon.easysshfs.settings.SettingsRepository
import ru.nsu.bobrofon.easysshfs.settings.settingsDataStore
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val TAG = "EasySSHFSService"
private const val CHANNEL_ID = "Channel Mount"
private const val CHANNEL_NAME = "Mount"
private const val NOTIFICATION_ID = 1

class EasySSHFSService : LifecycleService() {

    private val handler = Handler(Looper.getMainLooper())
    private val internetStateChangeReceiver = InternetStateChangeReceiver(handler)
    private val internetStateChangeFilter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
    private val shell: Shell by lazy { EasySSHFSActivity.initNewShell() }

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
                applicationContext, 0, notificationIntent, intentFLags
            )
            setContentIntent(pendingIntent)
        }.build()
    }
    private val notificationManager: NotificationManager by lazy {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "register NETWORK_STATE_CHANGED receiver")
        registerReceiver(internetStateChangeReceiver, internetStateChangeFilter)

        Log.d(TAG, "manage periodic ssh servers check")
        lifecycleScope.launch {
            managePeriodicServersCheck()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "unregister NETWORK_STATE_CHANGED receiver")
        unregisterReceiver(internetStateChangeReceiver)
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        Log.d(TAG, "onStartCommand")
        notificationManager.notify(NOTIFICATION_ID, notification)
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private suspend fun managePeriodicServersCheck() {
        val settingsRepository = SettingsRepository(applicationContext.settingsDataStore)
        var periodicJob: Job? = null

        settingsRepository.sshServersCheckRequired.collect { isRequired ->
            if (isRequired) {
                if (periodicJob == null) {
                    Log.d(TAG, "lunch new periodic ssh servers checker")
                    periodicJob = lifecycleScope.launch { schedulePeriodicServersChecks() }
                }
            } else {
                if (periodicJob != null) {
                    Log.d(TAG, "cancel periodic ssh servers checker")
                    periodicJob?.cancelAndJoin()
                    periodicJob = null
                }
            }
        }
    }

    private suspend fun schedulePeriodicServersChecks() {
        while (true) {
            Log.d(TAG, "waiting for the next remote servers check")
            delay(REMOTE_SERVERS_CHECK_PERIOD)

            Log.d(TAG, "check if some mountpoints are not automounted")
            withContext(Dispatchers.IO) {
                try {
                    val mountpoints = MountPointsList.instance(applicationContext).mountPoints
                        .filter { it.autoMount }
                        .filter { !it.checkIfMounted(false).first }
                        .filter { it.checkIfRemoteIsReachable(REMOTE_SERVER_CONNECTION_TIMEOUT) }

                    Log.d(TAG, "${mountpoints.size} mountpoints have to be mounted")
                    mountpoints.forEach { it.mount(shell) }
                } catch (e: Exception) {
                    Log.d(TAG, "periodic remote servers check failed", e)
                }
            }
        }
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

        private val REMOTE_SERVERS_CHECK_PERIOD = 5.minutes
        private val REMOTE_SERVER_CONNECTION_TIMEOUT = 1.seconds
    }
}
