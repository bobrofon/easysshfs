package ru.nsu.bobrofon.easysshfs


import android.content.Context
import android.os.Environment
import android.util.Log

import java.io.File
import java.io.IOException

import ru.nsu.bobrofon.easysshfs.log.AppLog
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList
import ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint.MountPoint

import android.content.ContentValues.TAG

class VersionUpdater internal constructor(private val mContext: Context) {
    private val mAppLog = AppLog.instance()

    fun update() {
        val currentVersion = BuildConfig.VERSION_CODE
        val settings = mContext.getSharedPreferences("sshfs", 0)
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
        val settings = mContext.getSharedPreferences("sshfs_cmd_global", 0)
        if (!settings.contains("host")) {
            return
        }

        val mountPoint = MountPoint()
        mountPoint.rootDir = settings.getString("root_dir", mContext.filesDir.path) ?: ""
        mountPoint.options = settings.getString("sshfs_opts",
                "password_stdin,UserKnownHostsFile=/dev/null,StrictHostKeyChecking=no" + ",rw,dirsync,nosuid,nodev,noexec,umask=0702,allow_other") ?: ""
        mountPoint.userName = settings.getString("username", "") ?: ""
        mountPoint.host = settings.getString("host", "") ?: ""
        mountPoint.setPort(Integer.toString(settings.getInt("port", 22)))
        mountPoint.localPath = settings.getString("local_dir",
                Environment.getExternalStorageDirectory().path + "/mnt") ?: ""
        mountPoint.remotePath = settings.getString("remote_dir", "") ?: ""

        val list = MountPointsList.getIntent(mContext)
        list.mountPoints.add(mountPoint)
        list.save(mContext)
    }

    private fun copyAssets(assetPath: String, localPath: String, force: Boolean) {
        try {
            val home = mContext.filesDir.path
            var file = File("$home/$localPath")
            if (!file.exists() || force) {
                val `in` = mContext.assets.open(assetPath)
                val out = mContext.openFileOutput(localPath, 0)
                `in`.copyTo(out, 4096)
                out.close()
                `in`.close()

                file = File("$home/$localPath")
                if (!file.setExecutable(true)) {
                    mAppLog.addMessage("Can't set executable bit on $localPath")
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "copyAssets: ", e)
        }

    }
}
