package ru.nsu.bobrofon.easysshfs.mountpointlist

import java.lang.ref.WeakReference
import java.util.LinkedList

import android.content.Context
import android.util.Log

import org.json.JSONArray
import org.json.JSONException

import com.topjohnwu.superuser.Shell

import ru.nsu.bobrofon.easysshfs.log.AppLog
import ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint.MountPoint
import ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint.MountStateChangeObserver

class MountPointsList(
    mountPoints: MutableList<MountPoint> = LinkedList(),
    private val appLog: AppLog = AppLog.instance()
) {
    var mountPoints: MutableList<MountPoint> = mountPoints
        private set

    private val autoMountObservable = AutoMountObservable()

    fun needAutomount(): Boolean = mountPoints.any {
        it.autoMount && !it.isMounted
    }

    fun autoMount(shell: Shell) = mountPoints.forEach {
        if (it.autoMount && !it.isMounted) {
            it.mount(shell)
        }
    }

    val isAutoMountEnabled get() = mountPoints.any { it.autoMount }

    fun umount(shell: Shell) = mountPoints.forEach { it.umount(shell) }

    fun registerMountObserver(observer: MountStateChangeObserver) = mountPoints.forEach {
        it.registerObserver(observer)
    }

    fun unregisterMountObserver(observer: MountStateChangeObserver) = mountPoints.forEach {
        it.unregisterObserver(observer)
    }

    fun registerAutoMountObserver(observer: AutoMountChangeObserver) {
        autoMountObservable.registerObserver(observer, isAutoMountEnabled)
    }

    fun unregisterAutoMountObserver(observer: AutoMountChangeObserver) {
        autoMountObservable.unregisterObserver(observer)
    }

    private fun load(context: Context) {
        val settings = context.getSharedPreferences(STORAGE_FILE, 0)
        try {
            val selfJson = JSONArray(settings.getString(STORAGE_FILE, "[]"))

            mountPoints.clear()
            for (i in 0 until selfJson.length()) {
                val mountPoint = MountPoint()
                mountPoint.json(selfJson.getJSONObject(i))
                mountPoints.add(mountPoint)
            }
        } catch (e: JSONException) {
            Log.e(TAG, e.message ?: "")
            log(e.message.orEmpty())
        }
    }

    fun save(context: Context) {
        val settings = context.getSharedPreferences(STORAGE_FILE, 0)

        val selJson = JSONArray()
        for (item in mountPoints) {
            selJson.put(item.json())
        }

        val prefsEditor = settings.edit()
        prefsEditor.putString(STORAGE_FILE, selJson.toString()).apply()
        autoMountObservable.notifyChanged(isAutoMountEnabled)
    }

    private fun log(message: CharSequence) {
        appLog.addMessage(message)
    }

    companion object {
        private const val TAG = "MOUNT_POINTS_LIST"
        private const val STORAGE_FILE = "mountpoints"

        private var instance = WeakReference<MountPointsList?>(null)

        @Synchronized
        fun instance(context: Context): MountPointsList {
            val oldInstance = instance.get()
            if (oldInstance == null) {
                val newInstance = MountPointsList().apply { load(context) }
                instance = WeakReference(newInstance)
                return newInstance
            }
            return oldInstance
        }
    }

}
