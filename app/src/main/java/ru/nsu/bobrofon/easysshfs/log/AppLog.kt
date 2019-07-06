package ru.nsu.bobrofon.easysshfs.log

import java.lang.ref.WeakReference

import android.util.Log

private const val TAG = "AppLog"

class AppLog(private val logBuffer: StringBuffer = StringBuffer()) : LogView {

    private val observable = LogObservable(this)

    init {
        Log.i(TAG, "new instance")
    }

    override fun toString(): String = logBuffer.toString()

    fun addMessage(message: CharSequence) {
        Log.i(TAG, "new message: $message")
        logBuffer.append(">_ ").append(message).append("\n")
        observable.notifyChanged()
    }

    fun clean() {
        logBuffer.setLength(0)
        observable.notifyChanged()
    }

    fun registerObserver(logChangeObserver: LogChangeObserver) =
        observable.registerObserver(logChangeObserver)

    fun unregisterObserver(logChangeObserver: LogChangeObserver) =
        observable.unregisterObserver(logChangeObserver)

    companion object {
        private var instance = WeakReference<AppLog?>(null)

        @Synchronized
        fun instance(): AppLog {
            val oldInstance = instance.get()
            if (oldInstance == null) {
                val newInstance = AppLog()
                instance = WeakReference(newInstance)
                return newInstance
            }
            return oldInstance
        }
    }
}
