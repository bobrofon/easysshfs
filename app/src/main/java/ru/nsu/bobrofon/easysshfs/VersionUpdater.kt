// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Environment
import android.system.Os
import android.util.Log

import ru.nsu.bobrofon.easysshfs.log.AppLog
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList
import ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint.MountPoint

import java.io.File
import java.io.IOException
import java.lang.Exception

private const val TAG = "VersionUpdater"

class VersionUpdater(
    private val context: Context,
    private val appLog: AppLog = AppLog.instance()
) {

    fun update() {
        val currentVersion = BuildConfig.VERSION_CODE
        val settings = context.getSharedPreferences("sshfs", 0)
        val lastVersion = settings.getInt("version", 0)

        // The only reason of those symlinks is to make executable files paths compatible with
        // legacy code. TODO(bobrofon): Use real paths in old code and remove symlinks.
        makeLibrarySymlink("ssh", "ssh")
        makeLibrarySymlink("sshfs", "sshfs")

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

    /**
     * @param libName source library name (without 'lib' prefix and '.so' extension)
     * @param exeName destination executable file name (relative to the application's 'file' dir)
     * @param forceUpdate override symlink if already exists
     */
    private fun makeLibrarySymlink(libName: String, exeName: String) {
        val library = File(context.applicationInfo.nativeLibraryDir, "lib$libName.so")
        val link = File(context.filesDir.path, exeName)

        try {
            // There is no way to check if symlink exist before API 26. Always delete it.
            if (!link.delete()) {
                appLog.addMessage("Cannot delete old $exeName")
            }
            makeSymlink(library.path, link.path)
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
         * Set of Android APIs which can be accessible only via reflection, because
         * minSdKVersion not allows to use them directly.
         */
        private object Reflected {
            object Os {
                /**
                 * Added in API level 21
                 */
                @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
                fun symlink(oldPath: String, newPath: String) {
                    val libCoreClass = Class.forName("libcore.io.Libcore")
                    val osField = libCoreClass.getDeclaredField("os")
                    osField.isAccessible = true
                    val os = osField.get(null)
                    val method =
                        os.javaClass.getMethod("symlink", String::class.java, String::class.java)
                    method.invoke(os, oldPath, newPath)
                }
            }
        }
    }
}
