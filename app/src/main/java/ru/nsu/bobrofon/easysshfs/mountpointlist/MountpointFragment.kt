// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.mountpointlist

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ListAdapter
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle

import ru.nsu.bobrofon.easysshfs.EasySSHFSFragment
import ru.nsu.bobrofon.easysshfs.R
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountpointFragment.OnFragmentInteractionListener
import ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint.MountPointsArrayAdapter
import ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint.MountStateChangeObserver

/**
 * Activities containing this fragment MUST implement the [OnFragmentInteractionListener]
 * interface.
 */
class MountpointFragment : EasySSHFSFragment(), AdapterView.OnItemClickListener,
    MountStateChangeObserver {

    private var onFragmentInteractionListener: OnFragmentInteractionListener? = null
    private lateinit var listView: AbsListView
    private lateinit var listAdapter: ListAdapter
    private lateinit var mountPointsList: MountPointsList

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val context = requireContext()
        val shell = shell!!

        val view = inflater.inflate(R.layout.fragment_mountpoint, container, false)

        mountPointsList = MountPointsList.instance(context)
        listAdapter = MountPointsArrayAdapter(context, mountPointsList.mountPoints, shell)

        listView = view.findViewById(android.R.id.list)
        listView.adapter = listAdapter
        listView.onItemClickListener = this

        mountPointsList.registerMountObserver(this)
        // mountPointsList.autoMount();

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        mountPointsList.unregisterMountObserver(this)
        super.onDestroyView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appActivity?.onSectionAttached(R.string.mount_point_list_title)
        onFragmentInteractionListener = appActivity
    }

    override fun onDetach() {
        onFragmentInteractionListener = null
        super.onDetach()

    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        onFragmentInteractionListener?.onFragmentInteraction(position)
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(id: Int)
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            if (!drawerStatus.isDrawerOpen) {
                menuInflater.inflate(R.menu.list, menu)
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            if (menuItem.itemId == R.id.action_new_mount_point) {
                onFragmentInteractionListener?.onFragmentInteraction(listAdapter.count)
                return true
            }

            return false
        }
    }

    override fun onMountStateChanged() {
        listView.invalidateViews()
    }
}
