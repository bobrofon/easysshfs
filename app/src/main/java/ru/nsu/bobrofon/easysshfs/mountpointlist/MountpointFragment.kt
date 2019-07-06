package ru.nsu.bobrofon.easysshfs.mountpointlist

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ListAdapter

import com.topjohnwu.superuser.Shell

import ru.nsu.bobrofon.easysshfs.DrawerStatus
import ru.nsu.bobrofon.easysshfs.EasySSHFSActivity
import ru.nsu.bobrofon.easysshfs.R

import ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint.MountPointsArrayAdapter
import ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint.MountStateChangeObserver

/**
 * A fragment representing a list of Items.
 *
 *
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 *
 *
 * Activities containing this fragment MUST implement the [OnFragmentInteractionListener]
 * interface.
 */
/**
 * Mandatory empty constructor for the fragment manager to instantiate the
 * fragment (e.g. upon screen orientation changes).
 */
class MountpointFragment : Fragment(), AdapterView.OnItemClickListener, MountStateChangeObserver {

    private var mListener: OnFragmentInteractionListener? = null

    /**
     * The fragment's ListView/GridView.
     */
    private var mListView: AbsListView? = null

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private var mAdapter: ListAdapter? = null

    private var mDrawerStatus: DrawerStatus? = null

    private var mountpoints: MountPointsList? = null

    private val shell: Shell?
        get() = (activity as EasySSHFSActivity).shell

    fun setDrawerStatus(drawerStatus: DrawerStatus) {
        mDrawerStatus = drawerStatus
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.fragment_mountpoint, container, false)

        val shell = shell ?: return view

        val list = MountPointsList.getIntent(activity as EasySSHFSActivity)
        mountpoints = list
        mAdapter = MountPointsArrayAdapter(activity as EasySSHFSActivity, list.mountPoints,
                shell)

        // Set the adapter
        mListView = view.findViewById(android.R.id.list)
        mListView!!.adapter = mAdapter

        // Set OnItemClickListener so we can be notified on item clicks
        mListView!!.setOnItemClickListener(this)

        mountpoints!!.registerObserver(this)
        // mountpoints.autoMount();

        return view
    }

    override fun onDestroyView() {
        mountpoints!!.unregisterObserver(this)
        super.onDestroyView()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        (activity as EasySSHFSActivity).onSectionAttached(R.string.mount_point_list_title)
        try {
            mListener = activity as EasySSHFSActivity
        } catch (e: ClassCastException) {
            throw ClassCastException("$activity must implement OnFragmentInteractionListener")
        }

    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener!!.onFragmentInteraction(position)
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(id: Int)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        if (mDrawerStatus == null || !mDrawerStatus!!.isDrawerOpen) {
            inflater!!.inflate(R.menu.list, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId

        if (id == R.id.action_new_mount_point) {
            mListener!!.onFragmentInteraction(mAdapter!!.count)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onMountStateChanged() {
        mListView!!.invalidateViews()
    }
}
