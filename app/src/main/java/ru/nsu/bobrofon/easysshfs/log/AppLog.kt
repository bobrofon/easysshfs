package ru.nsu.bobrofon.easysshfs.log

import java.lang.ref.WeakReference

import android.util.Log

private const val TAG = "AppLog"

class AppLog private constructor() : LogView {
    private val mLogBuffer = StringBuffer()
    private val mObservable = LogObservable(this)

    init {
        Log.i(TAG, "new instance")
    }

    override fun toString(): String = mLogBuffer.toString()

    fun addMessage(message: CharSequence) {
        Log.i(TAG, "new message: $message")
        mLogBuffer.append(">_ ").append(message).append("\n")
        mObservable.notifyChanged()
    }

    fun clean() {
        mLogBuffer.setLength(0)
        mObservable.notifyChanged()
    }

    fun registerObserver(logChangeObserver: LogChangeObserver) =
        mObservable.registerObserver(logChangeObserver)

    fun unregisterObserver(logChangeObserver: LogChangeObserver) =
        mObservable.unregisterObserver(logChangeObserver)

    companion object {
        private var mInstance = WeakReference<AppLog>(null)

        @Synchronized
        fun instance(): AppLog {
            val oldInstance = mInstance.get()
            if (oldInstance == null) {
                val newInstance = AppLog()
                mInstance = WeakReference(newInstance)
                return newInstance
            }
            return oldInstance
        }
    }
}
