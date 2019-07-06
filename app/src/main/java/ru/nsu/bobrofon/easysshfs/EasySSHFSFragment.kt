package ru.nsu.bobrofon.easysshfs

import android.support.v4.app.Fragment

import com.topjohnwu.superuser.Shell

abstract class EasySSHFSFragment : Fragment() {
    protected val appActivity: EasySSHFSActivity?
        get() = activity as? EasySSHFSActivity

    protected val shell: Shell?
        get() = appActivity?.shell
}
