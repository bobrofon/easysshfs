// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.mountpointlist

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.Button
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

    /**
     * buttonOnKeyListener is used when a mount button is focused, to implement custom D-pad
     * navigation between the button and an enclosing ListView's items.
     *
     * The listener expects the ListView item to look like this:
     * |----------------------|
     * |            +--------+|
     * | Item view  | Button ||
     * |            +--------+|
     * |----------------------|
     */
    private val buttonOnKeyListener = View.OnKeyListener { _, keycode, event ->
        if (event.action != KeyEvent.ACTION_DOWN) {
            return@OnKeyListener false
        }

        val dpadNavigationKeys = arrayOf(
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN
        )
        if (!dpadNavigationKeys.contains(keycode)) {
            return@OnKeyListener false
        }

        // We are leaving the button, so we need to pass the focus back.
        listView.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        listView.requestFocus()

        // The button is on the right side of the ListView item.
        // Moving left means "go back to the item containing this button".
        if (keycode == KeyEvent.KEYCODE_DPAD_LEFT) {
            // Focus is already passed to the original ListView item.
            true
        } else {
            // Allow ListView to select the next item. Otherwise, the currently selected item will
            // remain selected after moving out of the button.
            listView.onKeyDown(keycode, event)
        }
    }

    /**
     * buttonOnFocusChangeListener implements custom focus logic for buttons inside a ListView.
     */
    private val buttonOnFocusChangeListener = View.OnFocusChangeListener { buttonView, hasFocus ->
        if (hasFocus) {
            return@OnFocusChangeListener
        }

        // We should keep ButtonView unfocusable to allow ListView/GridView to handle onItemClick
        // properly.
        buttonView.isFocusable = false

        // Restore the original state in case the button lost the focus not via buttonOnKeyListener.
        listView.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
    }

    /**
     * listViewOnKeyListener is used when a mount button is not focused, to implement custom D-pad
     * navigation between the selected ListView item and the button inside it.
     *
     * The listener expects the ListView item to look like this:
     * |----------------------|
     * |            +--------+|
     * | Item view  | Button ||
     * |            +--------+|
     * |----------------------|
     */
    private val listViewOnKeyListener = View.OnKeyListener { _, keycode, event ->
        val selectedItemView = listView.selectedView ?: return@OnKeyListener false

        if (event.action != KeyEvent.ACTION_DOWN) {
            return@OnKeyListener false
        }
        if (keycode != KeyEvent.KEYCODE_DPAD_RIGHT) {
            return@OnKeyListener false
        }
        // The button is on the right side of the item view.
        // Moving right means "moving to the button".

        listView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

        val button = selectedItemView.findViewById<Button>(R.id.mountButton)
        button.isFocusable = true
        button.requestFocus()

        true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val context = requireContext()
        val shell = shell!!

        val view = inflater.inflate(R.layout.fragment_mountpoint, container, false)

        mountPointsList = MountPointsList.instance(context)
        listAdapter = MountPointsArrayAdapter(
            context,
            mountPointsList.mountPoints,
            shell,
            buttonOnFocusChangeListener,
            buttonOnKeyListener
        )

        listView = view.findViewById(android.R.id.list)
        listView.adapter = listAdapter
        listView.onItemClickListener = this
        listView.setOnKeyListener(listViewOnKeyListener)

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
