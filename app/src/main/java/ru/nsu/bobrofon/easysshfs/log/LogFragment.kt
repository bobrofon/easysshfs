package ru.nsu.bobrofon.easysshfs.log


import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import ru.nsu.bobrofon.easysshfs.DrawerStatus
import ru.nsu.bobrofon.easysshfs.EasySSHFSActivity
import ru.nsu.bobrofon.easysshfs.R

class LogFragment : Fragment(), LogModel.Observer {

    private var mLogTextView: TextView? = null
    private var mLogModel: LogModel? = null
    private var mDrawerStatus: DrawerStatus? = null

    fun setDrawerStatus(drawerStatus: DrawerStatus) {
        mDrawerStatus = drawerStatus
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.i(TAG, "onCreateView")
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)

        // Inflate the layout for this fragment
        val selfView = inflater.inflate(R.layout.fragment_log, container, false)

        mLogTextView = selfView.findViewById(R.id.log)

        mLogModel = LogSingleton.logModel

        mLogModel!!.registerObserver(this)

        return selfView
    }

    override fun onDestroyView() {
        Log.i(TAG, "onDestroyView")
        super.onDestroyView()
        mLogModel!!.unregisterObserver(this)
    }


    override fun onLogChanged(logModel: LogModel) {
        val logHeader = getString(R.string.debug_log_header)
        val logBody = logModel.log
        mLogTextView!!.text = String.format("%s%s", logHeader, logBody)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        (activity as EasySSHFSActivity).onSectionAttached(R.string.debug_log_title)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        if (mDrawerStatus == null || !mDrawerStatus!!.isDrawerOpen) {
            inflater!!.inflate(R.menu.log, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId

        if (id == R.id.action_clean) {
            mLogModel!!.clean()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        private val TAG = "LogFragment"
    }
}// Required empty public constructor
