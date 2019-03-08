package ru.nsu.bobrofon.easysshfs.mountpoint_list.mountpoint

import android.content.Context
import android.database.Observable
import android.os.AsyncTask
import android.util.Log
import android.util.Pair

import com.topjohnwu.superuser.Shell

import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.LinkedList

import ru.nsu.bobrofon.easysshfs.EasySSHFSActivity
import ru.nsu.bobrofon.easysshfs.log.LogSingleton

class MountPoint {
    private var mPointName: String? = null
    var autoMount: Boolean = false
    var userName: String? = null
    var host: String? = null
    private var mPort: Int = 0
    var password: String? = null
    var storePassword: Boolean = false
    var remotePath: String? = null
    var localPath: String? = null
    var forcePermissions: Boolean = false
    var options: String? = null
    private var mRootDir: String? = null
    var identityFile: String? = null

    private val mObservable = MountObservable()
    var isMounted = false
        private set

    var pointName: String?
        get() = if (!mPointName!!.isEmpty()) {
            mPointName
        } else {
            localPath
        }
        set(name) {
            mPointName = name
        }

    var port: Int
        get() = mPort
        set(port) {
            if (port < 0 || port > 65535) {
                logMessage("Port $port is out of range [0; 65535]")
                return
            }
            mPort = port
        }

    private val hostIp: String?
        get() {
            try {
                val address = InetAddress.getByName(host)
                if (address is Inet6Address) {
                    if (!address.getHostAddress().startsWith("[")) {
                        return '['.toString() + address.getHostAddress() + ']'.toString()
                    }
                }
                return address.hostAddress

            } catch (e: UnknownHostException) {
                logMessage(e.message.orEmpty())
                return host
            }

        }

    init {
        mPointName = ""
        autoMount = false
        userName = ""
        host = ""
        mPort = 22
        password = ""
        storePassword = false
        remotePath = ""
        localPath = ""
        forcePermissions = false
        options = DEFAULT_OPTIONS
        mRootDir = ""
        identityFile = ""
    }

    fun setPort(portS: String) {
        try {
            port = Integer.parseInt(portS)
        } catch (e: NumberFormatException) {
            logMessage("'$port' is invalid port value")
        }

    }

    fun setRootDir(rootDir: String) {
        mRootDir = rootDir
    }

    fun json(): JSONObject {
        val selfJson = JSONObject()
        try {
            selfJson.put("PointName", mPointName)
            selfJson.put("AutoMount", autoMount)
            selfJson.put("UserName", userName)
            selfJson.put("Host", host)
            selfJson.put("Port", mPort)
            if (storePassword) {
                selfJson.put("Password", password)
            }
            selfJson.put("RemotePath", remotePath)
            selfJson.put("LocalPath", localPath)
            selfJson.put("ForcePermissions", forcePermissions)
            selfJson.put("Options", options)
            selfJson.put("RootDir", mRootDir)
            selfJson.put("IdentityFile", identityFile)
        } catch (e: JSONException) {
            val TAG = "MOUNT_POINT"
            Log.e(TAG, e.message)
        }

        return selfJson
    }

    fun json(selfJson: JSONObject) {
        mPointName = selfJson.optString("PointName", mPointName)
        autoMount = selfJson.optBoolean("AutoMount", autoMount)
        userName = selfJson.optString("UserName", userName)
        host = selfJson.optString("Host", host)
        mPort = selfJson.optInt("Port", mPort)
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
        mRootDir = selfJson.optString("RootDir", mRootDir)
        identityFile = selfJson.optString("IdentityFile", identityFile)
    }

    private fun logMessage(message: String) {
        LogSingleton.logModel.addMessage(message)
    }

    fun checkMount(context: Context) {
        checkMount(false, context)
    }

    private fun checkMount(verbose: Boolean, context: Context) {
        CheckMountTask(verbose, context).execute()
    }

    fun mount(context: Context, shell: Shell) {
        mount(false, context, shell)
    }

    fun mount(verbose: Boolean, context: Context, shell: Shell) {
        logMessage("mount")
        MountTask(verbose, context, shell).execute()
    }

    fun umount(context: Context, shell: Shell) {
        umount(false, context, shell)
    }

    fun umount(verbose: Boolean, context: Context, shell: Shell) {
        var umountCommand = "umount "
        if (isBusyboxAvailable(shell)) {
            umountCommand = "busybox umount -f "
        }
        logMessage(umountCommand)
        runCommand(umountCommand + localPath!!, verbose, context, shell)
    }

    fun registerObserver(observer: Observer, context: Context) {
        mObservable.registerObserver(observer)
        checkMount(context)
    }

    fun unregisterObserver(observer: Observer) {
        mObservable.unregisterObserver(observer)
    }

    interface Observer {
        fun onMountStateChanged(mountPoint: MountPoint)
    }

    private inner class MountObservable : Observable<Observer>() {
        fun notifyChanged() {
            for (observer in mObservers) {
                observer.onMountStateChanged(this@MountPoint)
            }
        }
    }

    private inner class CheckMountTask constructor(private val mVerbose: Boolean, private val mContext: Context) : AsyncTask<Void, Void, Pair<Boolean, String>>() {
        private val mMountFile = "/proc/mounts"

        override fun doInBackground(vararg params: Void): Pair<Boolean, String> {
            val mountLine = StringBuilder()
            mountLine.append(userName).append('@')
            mountLine.append(hostIp).append(':')

            var canonicalLocalPath = localPath
            try {
                canonicalLocalPath = File(canonicalLocalPath!!).canonicalPath
            } catch (e: IOException) {
                logMessage("Can't get canonical path of " + localPath + " : " + e.message)
            }

            mountLine.append(remotePath).append(' ').append(canonicalLocalPath)
            mountLine.append(' ').append("fuse.sshfs").append(' ')

            var result = false

            try {
                val fstream = FileInputStream(mMountFile)
                val br = BufferedReader(InputStreamReader(fstream))

                var line: String?
                do {
                    line = br.readLine()
                    result = line.contains(mountLine.toString())
                } while (!result && line != null)

                br.close()
            } catch (e: FileNotFoundException) {
                return Pair<Boolean, String>(result, e.message)
            } catch (e: IOException) {
                return Pair<Boolean, String>(result, e.message)
            }

            isMounted = result
            return if (result) {
                Pair(result, "Pattern $mountLine is in $mMountFile")
            } else {
                Pair(result, "Pattern $mountLine is not in $mMountFile")
            }
        }

        override fun onPostExecute(result: Pair<Boolean, String>) {
            isMounted = result.first
            logMessage(result.second)

            mObservable.notifyChanged()
            if (mVerbose) {
                EasySSHFSActivity.showToast("done", mContext)
            }
        }
    }

    private inner class MountTask constructor(private val mVerbose: Boolean, private val mContext: Context, private val mShell: Shell) : AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg params: Void): String? {
            return hostIp
        }

        override fun onPostExecute(hostIp: String) {
            val command = fixLocalPath() + "echo '" + password + "' | " +
                    mRootDir + "/sshfs" +
                    " -o 'ssh_command=" + mRootDir + "/ssh" + ','.toString() +
                    options + identity() + ",port=" + port + "' " +
                    userName + '@'.toString() + hostIp + ':'.toString() +
                    remotePath + ' '.toString() + localPath

            runCommand(command, mVerbose, mContext, mShell)
        }

        private fun identity(): String {
            return if (identityFile!!.isEmpty()) {
                ""
            } else ",IdentityFile=" + identityFile!!
        }

        private fun fixLocalPath(): String {
            return if (!forcePermissions) {
                ""
            } else "mkdir -p " + localPath + " ; " +
                    "chmod 777 " + localPath + " ; " +
                    "chown 9997:9997 " + localPath + " ; "
        }
    }

    private fun runCommand(command: String, verbose: Boolean, aContext: Context, shell: Shell) {
        Log.i("shell", command)
        val stdout = LinkedList<String>()
        val stderr = LinkedList<String>()
        shell.run(stdout, stderr, object : Shell.Async.Callback {
            override fun onTaskResult(stdout: List<String>?, stderr: List<String>?) {
                logAll(stdout)
                logAll(stderr)
                checkMount(verbose, aContext)
            }

            private fun logAll(stdio: List<String>?) {
                if (stdio == null) {
                    return
                }
                for (line in stdio) {
                    logMessage(line)
                }
            }
        }, command)
    }

    private fun isBusyboxAvailable(_shell: Shell): Boolean {
        return _shell.testCmd("busybox")
    }

    companion object {

        private val DEFAULT_OPTIONS = (
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
    }

}
