package ru.nsu.bobrofon.easysshfs

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.ActionBar
import android.support.v4.app.Fragment
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.support.v4.widget.DrawerLayout
import android.widget.Toast

import com.topjohnwu.superuser.Shell

import ru.nsu.bobrofon.easysshfs.log.LogFragment
import ru.nsu.bobrofon.easysshfs.mountpoint_list.mountpoint.EditFragment
import ru.nsu.bobrofon.easysshfs.mountpoint_list.MountpointFragment


class EasySSHFSActivity : AppCompatActivity(), NavigationDrawerFragment.NavigationDrawerCallbacks, MountpointFragment.OnFragmentInteractionListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private var mNavigationDrawerFragment: NavigationDrawerFragment? = null

    /**
     * Used to store the last screen title. For use in [.restoreActionBar].
     */
    private var mTitle: CharSequence? = null
    private var mFragments: Array<Fragment>? = null
    var shell: Shell? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        shell = initNewShell()

        VersionUpdater(applicationContext).update()

        mFragments = arrayOf(MountpointFragment(), LogFragment())

        setContentView(R.layout.activity_easy_sshfs)

        mNavigationDrawerFragment = supportFragmentManager.findFragmentById(R.id.navigation_drawer) as NavigationDrawerFragment
        mTitle = title

        // Set up the drawer.
        mNavigationDrawerFragment!!.setUp(
                R.id.navigation_drawer,
                findViewById<View>(R.id.drawer_layout) as DrawerLayout)

        mNavigationDrawerFragment?.let((mFragments!![0] as MountpointFragment)::setDrawerStatus)
        mNavigationDrawerFragment?.let((mFragments!![1] as LogFragment)::setDrawerStatus)
    }

    public override fun onResume() {
        super.onResume()
    }

    public override fun onPause() {
        super.onPause()
    }

    override fun onNavigationDrawerItemSelected(position: Int) {
        // update the main content by replacing fragments
        try {
            val fragmentManager = supportFragmentManager
            var backStackCount = fragmentManager.backStackEntryCount
            while (backStackCount-- > 0) {
                fragmentManager.popBackStack()
            }

            fragmentManager.beginTransaction()
                    .replace(R.id.container, mFragments!![position])
                    .commit()
        } catch (e: ArrayIndexOutOfBoundsException) {
            finish()
        }

    }

    fun onSectionAttached(titleId: Int) {
        mTitle = getString(titleId)
    }

    private fun restoreActionBar() {
        val actionBar = supportActionBar
        actionBar!!.navigationMode = ActionBar.NAVIGATION_MODE_STANDARD
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.title = mTitle
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!mNavigationDrawerFragment!!.isDrawerOpen) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            restoreActionBar()
            return true
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onFragmentInteraction(id: Int) {
        val editFragment = EditFragment.newInstance(id)
        mNavigationDrawerFragment?.let(editFragment::setDrawerStatus)

        val fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction()
                .replace(R.id.container, editFragment)
                .addToBackStack(null)
                .commit()
    }

    companion object {

        fun showToast(message: CharSequence, context: Context?) {
            if (context != null) {
                val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
                toast.show()
            }
        }

        fun initNewShell(): Shell {
            Shell.setFlags(Shell.FLAG_MOUNT_MASTER)
            return Shell.getShell()
        }
    }
}
