package ru.nsu.bobrofon.easysshfs.log


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import kotlinx.android.synthetic.main.fragment_log.log as logTextView

import ru.nsu.bobrofon.easysshfs.EasySSHFSFragment
import ru.nsu.bobrofon.easysshfs.R

private const val TAG = "LogFragment"

class LogFragment : EasySSHFSFragment(), LogChangeObserver {

    private val appLog = AppLog.instance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "onCreateView")
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appLog.registerObserver(this)
    }

    override fun onDestroyView() {
        Log.i(TAG, "onDestroyView")
        appLog.unregisterObserver(this)
        super.onDestroyView()
    }


    override fun onLogChanged(logView: LogView) {
        val logHeader = getString(R.string.debug_log_header)
        val logBody = logView.toString()
        logTextView.text = String.format("%s%s", logHeader, logBody)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        appActivity?.onSectionAttached(R.string.debug_log_title)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        if (!drawerStatus.isDrawerOpen) {
            inflater?.inflate(R.menu.log, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item?.itemId

        if (id == R.id.action_clean) {
            appLog.clean()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}// Required empty public constructor
