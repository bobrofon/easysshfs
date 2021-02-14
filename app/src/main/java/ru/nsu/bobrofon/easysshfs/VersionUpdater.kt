package ru.nsu.bobrofon.easysshfs

import java.io.File
import java.io.IOException

import android.content.Context
import android.os.Environment
import android.util.Log

import ru.nsu.bobrofon.easysshfs.log.AppLog
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList
import ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint.MountPoint

private const val TAG = "VersionUpdater"

class VersionUpdater(
    private val context: Context,
    private val appLog: AppLog = AppLog.instance()
) {

    fun update() {
        val currentVersion = BuildConfig.VERSION_CODE
        val settings = context.getSharedPreferences("sshfs", 0)
        val lastVersion = settings.getInt("version", 0)

        copyAssets("ssh", "ssh", lastVersion != currentVersion)
        copyAssets("sshfs", "sshfs", lastVersion != currentVersion)

        if (lastVersion < 9) {
            update02to03()
        }

        val prefsEditor = settings.edit()
        prefsEditor.putInt("version", currentVersion)
        prefsEditor.apply()
    }

    private fun update02to03() {
        val settings = context.getSharedPreferences("sshfs_cmd_global", 0)
        if (!settings.contains("host")) {
            return
        }

        val mountPoint = MountPoint()
        mountPoint.rootDir = settings.getString("root_dir", context.filesDir.path) ?: ""
        mountPoint.options = settings.getString(
            "sshfs_opts",
            "password_stdin,UserKnownHostsFile=/dev/null,StrictHostKeyChecking=no" +
                    ",rw,dirsync,nosuid,nodev,noexec,umask=0702,allow_other"
        ) ?: ""
        mountPoint.userName = settings.getString("username", "") ?: ""
        mountPoint.host = settings.getString("host", "") ?: ""
        mountPoint.setPort(settings.getInt("port", 22).toString())
        mountPoint.localPath =
            settings.getString("local_dir", Environment.getExternalStorageDirectory().path + "/mnt")
                ?: ""
        mountPoint.remotePath = settings.getString("remote_dir", "") ?: ""

        val list = MountPointsList.instance(context)
        list.mountPoints.add(mountPoint)
        list.save(context)
    }

    private fun copyAssets(assetPath: String, localPath: String, force: Boolean) {
        try {
            val home = context.filesDir.path
            val file = File("$home/$localPath")
            if (!file.exists() || force) {
                context.assets.open(assetPath).use { inputStream ->
                    context.openFileOutput(localPath, 0).use { outputStream ->
                        inputStream.copyTo(outputStream, 4096)
                    }
                }

                if (!file.setExecutable(true)) {
                    appLog.addMessage("Can't set executable bit on $localPath")
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "copyAssets: ", e)
        }
    }
}
