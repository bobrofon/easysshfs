package ru.nsu.bobrofon.easysshfs.mountpoint_list

import java.util.concurrent.atomic.AtomicReference
import java.util.LinkedList

import android.content.Context
import android.util.Log

import org.json.JSONArray
import org.json.JSONException

import com.topjohnwu.superuser.Shell

import ru.nsu.bobrofon.easysshfs.log.AppLog
import ru.nsu.bobrofon.easysshfs.mountpoint_list.mountpoint.MountPoint

class MountPointsList private constructor() {
    private val mAppLog = AppLog.instance()

    private val mMountPoints: MutableList<MountPoint> = LinkedList()

    val mountPoints: MutableList<MountPoint>
        get() = mMountPoints

    fun checkMount(context: Context) {
        for (mountPoint in mMountPoints) {
            mountPoint.checkMount(context)
        }
    }

    fun needAutomount(): Boolean? {
        for (mountPoint in mMountPoints) {
            if (mountPoint.autoMount && !mountPoint.isMounted) {
                return true
            }
        }
        return false
    }

    fun autoMount(context: Context, shell: Shell) {
        for (item in mMountPoints) {
            if (item.autoMount/* && !item.isMounted()*/) {
                item.mount(context, shell)
            }
        }
    }

    fun umount(context: Context, shell: Shell) {
        for (item in mMountPoints) {
            item.umount(false, context, shell)
        }
    }

    fun registerObserver(observer: MountPoint.Observer, context: Context) {
        for (item in mMountPoints) {
            item.registerObserver(observer, context)
        }
    }

    fun unregisterObserver(observer: MountPoint.Observer) {
        for (item in mMountPoints) {
            item.unregisterObserver(observer)
        }
    }

    private fun load(context: Context): MountPointsList {
        val settings = context.getSharedPreferences(STORAGE_FILE, 0)
        try {
            val selfJson = JSONArray(settings.getString(STORAGE_FILE, "[]"))

            mMountPoints.clear()
            for (i in 0 until selfJson.length()) {
                val mountPoint = MountPoint()
                mountPoint.json(selfJson.getJSONObject(i))
                mMountPoints.add(mountPoint)
            }
        } catch (e: JSONException) {
            Log.e(TAG, e.message)
            log(e.message.orEmpty())
        }

        return this
    }

    fun save(context: Context) {
        val settings = context.getSharedPreferences(STORAGE_FILE, 0)

        val selJson = JSONArray()
        for (item in mMountPoints) {
            selJson.put(item.json())
        }

        val prefsEditor = settings.edit()
        prefsEditor.putString(STORAGE_FILE, selJson.toString()).apply()
    }

    private fun log(message: CharSequence) {
        mAppLog.addMessage(message)
    }

    companion object {
        private const val TAG = "MOUNT_POINTS_LIST"
        private const val STORAGE_FILE = "mountpoints"

        private val self = AtomicReference<MountPointsList>(null)

        fun getIntent(context: Context): MountPointsList {
            if (self.get() == null) {
                self.compareAndSet(null, MountPointsList().load(context))
            }
            return self.get()!!
        }
    }

}
