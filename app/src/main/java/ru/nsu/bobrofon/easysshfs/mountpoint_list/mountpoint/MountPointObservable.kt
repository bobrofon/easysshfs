package ru.nsu.bobrofon.easysshfs.mountpoint_list.mountpoint

import android.database.Observable

interface MountStateChangeObserver {
    fun onMountStateChanged()
}

internal class MountPointObservable : Observable<MountStateChangeObserver>() {
    fun notifyChanged() {
        for (observer in mObservers) {
            observer.onMountStateChanged()
        }
    }
}
