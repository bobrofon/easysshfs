// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.util.Pair
import com.topjohnwu.superuser.Shell
import org.json.JSONException
import org.json.JSONObject
import ru.nsu.bobrofon.easysshfs.EasySSHFSActivity
import ru.nsu.bobrofon.easysshfs.IpResolver
import ru.nsu.bobrofon.easysshfs.log.AppLog
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*


class MountPoint(
    var pointName: String = "",
    var autoMount: Boolean = false,
    var userName: String = "",
    var host: String = "",
    port: Int = 22,
    var password: String = "",
    var storePassword: Boolean = false,
    var remotePath: String = "",
    var localPath: String = "",
    var forcePermissions: Boolean = false,
    var options: String = DEFAULT_OPTIONS,
    var rootDir: String = "",
    var identityFile: String = "",
    isMounted: Boolean = false,
    private val appLog: AppLog = AppLog.instance()
) {
    private val observable = MountPointObservable()

    var isMounted = isMounted
        private set

    val visiblePointName: String
        get() = pointName.ifEmpty { localPath }

    var port: Int = port
        set(value) {
            if (value < 0 || value > 65535) {
                logMessage("Port $value is out of range [0; 65535]")
                return
            }
            field = value
        }

    private fun hostIp(foreground: Boolean = true) = IpResolver.resolve(host, foreground)

    fun setPort(value: String) {
        try {
            port = Integer.parseInt(value)
        } catch (e: NumberFormatException) {
            logMessage("'$port' is invalid port value")
        }

    }

    fun json(): JSONObject {
        val selfJson = JSONObject()
        try {
            selfJson.put("PointName", pointName)
            selfJson.put("AutoMount", autoMount)
            selfJson.put("UserName", userName)
            selfJson.put("Host", host)
            selfJson.put("Port", port)
            if (storePassword) {
                selfJson.put("Password", password)
            }
            selfJson.put("RemotePath", remotePath)
            selfJson.put("LocalPath", localPath)
            selfJson.put("ForcePermissions", forcePermissions)
            selfJson.put("Options", options)
            selfJson.put("RootDir", rootDir)
            selfJson.put("IdentityFile", identityFile)
        } catch (e: JSONException) {
            Log.e(TAG, e.message ?: "")
        }

        return selfJson
    }

    fun json(selfJson: JSONObject) {
        pointName = selfJson.optString("PointName", pointName)
        autoMount = selfJson.optBoolean("AutoMount", autoMount)
        userName = selfJson.optString("UserName", userName)
        host = selfJson.optString("Host", host)
        port = selfJson.optInt("Port", port)
        storePassword = selfJson.optBoolean("StorePassword", storePassword)
        if (selfJson.has("Password")) {
            storePassword = true
            password = selfJson.optString("Password", password)
        } else {
            storePassword = false
            password = ""
        }
        remotePath = selfJson.optString("RemotePath", remotePath)
        localPath = selfJson.optString("LocalPath", localPath)
        forcePermissions = selfJson.optBoolean("ForcePermissions", forcePermissions)
        options = selfJson.optString("Options", options)
        rootDir = selfJson.optString("RootDir", rootDir)
        identityFile = selfJson.optString("IdentityFile", identityFile)
    }

    private fun logMessage(message: String) {
        appLog.addMessage(message)
    }

    private fun checkMount(context: Context? = null) {
        CheckMountTask(this, context).execute()
    }

    fun mount(shell: Shell, context: Context? = null) {
        logMessage("mount")
        MountTask(this, shell, context).execute()
    }

    fun umount(shell: Shell, context: Context? = null) {
        if (context == null && !isMounted) {
            Log.d(TAG, "skip umount because we are in the background and it is not mounted")
            return
        }

        val umountCommand = "umount -l -v $localPath"
        logMessage(umountCommand)
        runCommand(umountCommand, shell, context)
    }

    fun registerObserver(observer: MountStateChangeObserver) {
        observable.registerObserver(observer)
        checkMount()
    }

    fun unregisterObserver(observer: MountStateChangeObserver) {
        observable.unregisterObserver(observer)
    }

    private fun runCommand(command: String, shell: Shell, context: Context? = null) {
        Log.i("shell", command)
        shell.newJob().add(command).to(LinkedList(), LinkedList()).submit { result ->
            logAll(result.out)
            logAll(result.err)
            logMessage("exit code: ${result.code}")
            checkMount(context)
        }
    }

    private fun logAll(stdio: List<String>?) {
        if (stdio == null) {
            return
        }
        for (line in stdio) {
            logMessage(line)
        }
    }

    companion object {

        private const val DEFAULT_OPTIONS = (
                "password_stdin,"
                        + "UserKnownHostsFile=/dev/null,"
                        + "StrictHostKeyChecking=no,"
                        + "rw,"
                        + "dirsync,"
                        + "nosuid,"
                        + "nodev,"
                        + "noexec,"
                        + "umask=0,"
                        + "allow_other,"
                        + "uid=9997,"
                        + "gid=9997")

        private const val TAG = "MOUNT_POINT"

        private class CheckMountTask constructor(
            private val mountPoint: MountPoint,
            private val context: WeakReference<Context?> = WeakReference(null)
        ) :
            AsyncTask<Void, Void, Pair<Boolean?, String>>() {

            private val mountFile = "/proc/mounts"

            constructor(mountPoint: MountPoint, context: Context?) :
                    this(mountPoint, WeakReference(context))

            @Deprecated("Deprecated in Java")
            override fun doInBackground(vararg params: Void): Pair<Boolean?, String> {
                val fromForeground = context.get() != null
                val mountLine = StringBuilder()
                mountLine.append("${mountPoint.userName}@${mountPoint.hostIp(fromForeground)}:")

                val canonicalLocalPath: String
                try {
                    canonicalLocalPath = File(mountPoint.localPath).canonicalPath
                } catch (e: IOException) {
                    return Pair(
                        null,
                        "Can't get canonical path of ${mountPoint.localPath}: ${e.message}"
                    )
                }

                mountLine.append("${mountPoint.remotePath} $canonicalLocalPath fuse.sshfs ")

                val result: Boolean
                try {
                    val procmount = File(mountFile)
                    result = procmount.useLines { lines ->
                        lines.any { line -> line.contains(mountLine.toString()) }
                    }
                } catch (e: FileNotFoundException) {
                    return Pair(null, e.message ?: "")
                } catch (e: IOException) {
                    return Pair(null, e.message ?: "")
                }

                return if (result) {
                    Pair(true, "Pattern $mountLine is in $mountFile")
                } else {
                    Pair(false, "Pattern $mountLine is not in $mountFile")
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onPostExecute(result: Pair<Boolean?, String>) {
                mountPoint.isMounted = result.first ?: mountPoint.isMounted
                mountPoint.logMessage(result.second)

                mountPoint.observable.notifyChanged()
                context.get()?.let {
                    EasySSHFSActivity.showToast(
                        "done",
                        it
                    )
                }
            }
        }

        private class MountTask constructor(
            private val mountPoint: MountPoint,
            private val shell: Shell,
            private val context: WeakReference<Context?> = WeakReference(null)
        ) : AsyncTask<Void, Void, String>() {

            constructor(mountPoint: MountPoint, shell: Shell, context: Context?) :
                    this(mountPoint, shell, WeakReference(context))

            @Deprecated("Deprecated in Java")
            override fun doInBackground(vararg params: Void): String {
                val fromForeground = context.get() != null
                return mountPoint.hostIp(fromForeground)
            }

            @Deprecated("Deprecated in Java")
            override fun onPostExecute(hostIp: String) {
                val command = "${ensureLocalPath()} echo '${mountPoint.password}' | " +
                        "${mountPoint.rootDir}/sshfs" +
                        " -o 'ssh_command=${mountPoint.rootDir}/ssh," +
                        "${mountPoint.options}${identity()},port=${mountPoint.port}' " +
                        "${mountPoint.userName}@$hostIp:" +
                        "${mountPoint.remotePath} ${mountPoint.localPath}"

                mountPoint.runCommand(command, shell, context.get())
            }

            private fun identity(): String {
                return if (mountPoint.identityFile.isEmpty()) {
                    ""
                } else ",IdentityFile=${mountPoint.identityFile}"
            }

            private fun ensureLocalPath(): String {
                if (context.get() == null) {
                    return "ls ${mountPoint.localPath} && "
                }
                if (mountPoint.forcePermissions) {
                    return "mkdir -p ${mountPoint.localPath} ; " +
                            "chmod 777 ${mountPoint.localPath} ; " +
                            "chown 9997:9997 ${mountPoint.localPath} ; "
                }
                return ""
            }
        }
    }
}
