// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log

import ru.nsu.bobrofon.easysshfs.log.AppLog
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList
import ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint.MountPoint

import java.io.File
import java.io.IOException

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
                val selectedAssetPath = selectAssetByDeviceABI(assetPath)
                context.assets.open(selectedAssetPath).use { inputStream ->
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

    /**
     * Returns path to assetPath preferred by current device depends on its ABI.
     */
    private fun selectAssetByDeviceABI(assetPath: String): String {
        val abi = supportedABIs.firstOrNull { abi ->
            context.assets.list(abi).orEmpty().contains(assetPath)
        }
        return if (abi != null) {
            "$abi/$assetPath"
        } else {
            // There is no asset variant for supported architectures. It may be a common file for
            // all ABIs. Even if it's not, a caller code should handle this situation anyway. So it
            // is fine to return original assetPath here even if it doesn't exist.
            assetPath
        }
    }

    companion object {
        /**
         * An ordered list of ABIs supported by this device.
         * The most preferred ABI is the first element in the list.
         */
        private val supportedABIs: Array<String>
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Build.SUPPORTED_ABIS
            } else {
                arrayOf(Deprecated.Build.CPU_ABI, Deprecated.Build.CPU_ABI2)
            }

        /**
         * Set of deprecated Android APIs which are still used in the application, because
         * minSdKVersion not allows to replace them with something else.
         */
        private object Deprecated {
            /**
             * android.os.Build
             */
            object Build {
                /**
                 * Added in API level 4
                 * Deprecated in API level 21
                 */
                val CPU_ABI: String
                    @TargetApi(android.os.Build.VERSION_CODES.KITKAT_WATCH)
                    @Suppress("DEPRECATION")
                    get() = android.os.Build.CPU_ABI

                /**
                 * Added in API level 4
                 * Deprecated in API level 21
                 */
                val CPU_ABI2: String
                    @TargetApi(android.os.Build.VERSION_CODES.KITKAT_WATCH)
                    @Suppress("DEPRECATION")
                    get() = android.os.Build.CPU_ABI2
            }
        }
    }
}
