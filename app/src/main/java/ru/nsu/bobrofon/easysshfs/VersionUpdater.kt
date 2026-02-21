// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.system.Os
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.edit
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

        // End user can replace those symlinks with third-party executables. If application will
        // directly use .so files, it will be not possible.
        makeLibrarySymlink("ssh", "ssh", lastVersion != currentVersion)
        makeLibrarySymlink("sshfs", "sshfs", lastVersion != currentVersion)

        if (lastVersion < 9) {
            update02to03()
        }

        settings.edit {
            putInt("version", currentVersion)
        }
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
        mountPoint.localPath = settings.getString(
            "local_dir",
            DeprecatedApi.Environment.getExternalStorageDirectory().path + "/mnt"
        ) ?: ""
        mountPoint.remotePath = settings.getString("remote_dir", "") ?: ""

        val list = MountPointsList.instance(context)
        list.mountPoints.add(mountPoint)
        list.save(context)
    }

    /**
     * @param libName source library name (without 'lib' prefix and '.so' extension)
     * @param exeName destination executable file name (relative to the application's 'file' dir)
     * @param forceUpdate override symlink if already exists
     */
    private fun makeLibrarySymlink(libName: String, exeName: String, forceUpdate: Boolean) {
        val soName = "lib$libName.so"
        val library = File(context.applicationInfo.nativeLibraryDir, soName)
        val localLibrary = File(context.filesDir.path, soName)
        val link = File(context.filesDir.path, exeName)

        try {
            Log.d(TAG, "delete local .so link")
            if (fileExists(localLibrary.path) && !localLibrary.delete()) {
                appLog.addMessage("Cannot delete $soName")
            }
            Log.d(TAG, "update .so link")
            makeSymlink(library.path, localLibrary.path)
            if (fileExists(link.path)) {
                if (forceUpdate) {
                    Log.i(TAG, "deleting old $exeName")
                    if (!link.delete()) {
                        appLog.addMessage("Cannot delete old $exeName")
                    }
                } else {
                    Log.d(TAG, "using old $exeName")
                    return
                }
            }
            Log.i(TAG, "installing new $exeName")
            makeSymlink(localLibrary.name, link.path)
        } catch (e: IOException) {
            Log.w(TAG, "symlink update failed", e)
        }
    }

    companion object {
        private fun makeSymlink(originalPath: String, linkPath: String) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Os.symlink(originalPath, linkPath)
                } else {
                    Reflected.Os.symlink(originalPath, linkPath)
                }
            } catch (e: Exception) {
                // ErrnoException is only available since API 21
                throw IOException(e)
            }
        }

        /**
         * @return true if file or directory or symlink exists.
         */
        private fun fileExists(path: String): Boolean {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Os.lstat(path) // ignore result, check exceptions instead
                } else {
                    Reflected.Os.lstat(path)
                }
            } catch (_: Exception) {
                // ErrnoException is only available since API 21
                return false
            }
            return true
        }

        /**
         * Set of Android APIs which can be accessible only via reflection, because
         * minSdKVersion not allows to use them directly.
         */
        private object Reflected {
            object Os {
                private val self: Any
                    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
                    @SuppressLint("ObsoleteSdkInt", "DiscouragedPrivateApi")
                    @RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                    get() {
                        val libCoreClass = Class.forName("libcore.io.Libcore")
                        val osField = libCoreClass.getDeclaredField("os")
                        osField.isAccessible = true
                        return osField.get(null)!!
                    }

                @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
                @SuppressLint("ObsoleteSdkInt")
                @RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                fun symlink(oldPath: String, newPath: String) {
                    val method =
                        self.javaClass.getMethod("symlink", String::class.java, String::class.java)
                    method.invoke(self, oldPath, newPath)
                }

                @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
                @SuppressLint("ObsoleteSdkInt")
                @RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                fun lstat(path: String): Any {
                    val method = self.javaClass.getMethod("lstat", String::class.java)
                    return method.invoke(self, path)!!
                }
            }
        }
    }
}
