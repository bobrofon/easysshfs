// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.mountpointlist

import android.database.Observable

interface AutoMountChangeObserver {
    fun onAutoMountChanged(isAutoMountRequired: Boolean)
}

class AutoMountObservable : Observable<AutoMountChangeObserver>() {

    fun registerObserver(observer: AutoMountChangeObserver, isAutoMountRequired: Boolean) {
        super.registerObserver(observer)
        observer.onAutoMountChanged(isAutoMountRequired)
    }

    fun notifyChanged(isAutoMountRequired: Boolean) {
        for (observer in mObservers) {
            observer.onAutoMountChanged(isAutoMountRequired)
        }
    }
}
