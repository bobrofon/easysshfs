// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.mountpointlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ListAdapter
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import ru.nsu.bobrofon.easysshfs.EasySSHFSFragment
import ru.nsu.bobrofon.easysshfs.R
import ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint.EditFragment
import ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint.MountPointsArrayAdapter
import ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint.MountStateChangeObserver

class MountpointFragment : EasySSHFSFragment(), AdapterView.OnItemClickListener,
    MountStateChangeObserver {

    private lateinit var listView: AbsListView
    private lateinit var listAdapter: ListAdapter
    private lateinit var mountPointsList: MountPointsList

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        goToEditFragment(position)
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.list, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            if (menuItem.itemId == R.id.action_new_mount_point) {
                goToEditFragment(listAdapter.count)
                return true
            }

            return false
        }
    }

    private fun goToEditFragment(id: Int) {
        findNavController().navigate(R.id.editFragment, EditFragment.createArgs(id))
    }

    override fun onMountStateChanged() {
        listView.invalidateViews()
    }
}
