package ru.nsu.bobrofon.easysshfs

import android.content.Context
import android.support.v7.app.ActionBar
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 */
class NavigationDrawerFragment : EasySSHFSFragment(), DrawerStatus {

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private var navigationDrawerCallbacks: NavigationDrawerCallbacks? = null

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerListView: ListView
    private lateinit var fragmentContainerView: View

    private var currentSelectedPosition = 0
    private var fromSavedInstanceState: Boolean = false
    private var userLearnedDrawer: Boolean = false

    override val isDrawerOpen: Boolean
        get() = drawerLayout.isDrawerOpen(fragmentContainerView)

    private val actionBar: ActionBar?
        get() = appActivity?.supportActionBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer.
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        userLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false)

        if (savedInstanceState != null) {
            currentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION)
            fromSavedInstanceState = true
        }

        // Select either the default item (0) or the last selected item.
        selectItem(currentSelectedPosition)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        drawerListView =
            inflater.inflate(R.layout.fragment_navigation_drawer, container, false) as ListView
        drawerListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> selectItem(position) }
        drawerListView.adapter = ArrayAdapter(
            actionBar!!.themedContext,
            android.R.layout.simple_list_item_1,
            android.R.id.text1,
            arrayOf(
                getString(R.string.title_section1),
                getString(R.string.title_section2),
                getString(R.string.title_section3)
            )
        )
        drawerListView.setItemChecked(currentSelectedPosition, true)
        return drawerListView
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param layout The DrawerLayout containing this fragment's UI.
     */
    fun setUp(fragmentId: Int, layout: DrawerLayout) {
        fragmentContainerView = activity?.findViewById(fragmentId)!!
        drawerLayout = layout

        // set a custom shadow that overlays the main content when the drawer opens
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
        // set up the drawer's list view with items and click listener

        val actionBar = actionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeButtonEnabled(true)

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        drawerToggle = object : ActionBarDrawerToggle(
            activity, /* host Activity */
            drawerLayout, /* DrawerLayout object */
            R.string.navigation_drawer_open, /* "open drawer" description for accessibility */
            R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                if (!isAdded) {
                    return
                }

                activity?.invalidateOptionsMenu() // calls onPrepareOptionsMenu()
            }

            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                if (!isAdded) {
                    return
                }

                if (!userLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    userLearnedDrawer = true
                    val sp = PreferenceManager.getDefaultSharedPreferences(context)
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply()
                }

                activity?.invalidateOptionsMenu() // calls onPrepareOptionsMenu()
            }
        }

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!userLearnedDrawer && !fromSavedInstanceState) {
            drawerLayout.openDrawer(fragmentContainerView)
        }

        // Defer code dependent on restoration of previous instance state.
        drawerLayout.post { drawerToggle.syncState() }
        drawerLayout.addDrawerListener(drawerToggle)
    }

    private fun selectItem(position: Int) {
        currentSelectedPosition = position
        drawerListView.setItemChecked(position, true)
        drawerLayout.closeDrawer(fragmentContainerView)
        navigationDrawerCallbacks?.onNavigationDrawerItemSelected(position)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        navigationDrawerCallbacks = appActivity!!
    }

    override fun onDetach() {
        navigationDrawerCallbacks = null
        super.onDetach()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_SELECTED_POSITION, currentSelectedPosition)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        // Forward the new configuration the drawer toggle component.
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        // If the drawer is open, show the global app actions in the action bar. See also
        // showGlobalContextActionBar, which controls the top-left area of the action bar.
        if (isDrawerOpen) {
            showGlobalContextActionBar()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private fun showGlobalContextActionBar() {
        val actionBar = actionBar!!
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setTitle(R.string.app_name)
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        fun onNavigationDrawerItemSelected(position: Int)
    }

    companion object {

        /**
         * Remember the position of the selected item.
         */
        private const val STATE_SELECTED_POSITION = "selected_navigation_drawer_position"

        private const val PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned"
    }
}
