package ru.nsu.bobrofon.easysshfs.log

import android.database.Observable

interface LogView {
    override fun toString(): String
}

interface LogChangeObserver {
    fun onLogChanged(logView: LogView)
}

internal class LogObservable(private val logView: LogView) :
    Observable<LogChangeObserver>() {

    override fun registerObserver(observer: LogChangeObserver) {
        super.registerObserver(observer)
        observer.onLogChanged(logView)
    }

    fun notifyChanged() {
        for (observer in mObservers) {
            observer.onLogChanged(logView)
        }
    }
}
