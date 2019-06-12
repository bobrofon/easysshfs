package ru.nsu.bobrofon.easysshfs.log

import android.database.Observable
import android.util.Log

class LogModel internal constructor() {

    private val mObservable = LogObservable()
    private var mLogBuffer = StringBuffer()

    val log: String
        get() = mLogBuffer.toString()

    init {
        Log.i(TAG, "new Instance")
    }

    fun addMessage(message: CharSequence) {
        Log.i(TAG, "new message: $message")
        mLogBuffer.append(">_ ").append(message).append("\n")
        mObservable.notifyChanged()
    }

    fun clean() {
        mLogBuffer = StringBuffer()
        mObservable.notifyChanged()
    }

    fun registerObserver(observer: Observer) {
        mObservable.registerObserver(observer)
        observer.onLogChanged(this)
    }

    fun unregisterObserver(observer: Observer) {
        mObservable.unregisterObserver(observer)
    }

    interface Observer {
        fun onLogChanged(logModel: LogModel)
    }

    private inner class LogObservable : Observable<Observer>() {
        internal fun notifyChanged() {
            for (observer in mObservers) {
                observer.onLogChanged(this@LogModel)
            }
        }
    }

    companion object {
        private val TAG = "LogModel"
    }
}
