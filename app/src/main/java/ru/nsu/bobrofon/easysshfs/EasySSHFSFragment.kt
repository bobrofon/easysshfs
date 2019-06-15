package ru.nsu.bobrofon.easysshfs

import android.support.v4.app.Fragment

abstract class EasySSHFSFragment : Fragment() {
    protected val appActivity: EasySSHFSActivity?
        get() = activity as? EasySSHFSActivity
}
