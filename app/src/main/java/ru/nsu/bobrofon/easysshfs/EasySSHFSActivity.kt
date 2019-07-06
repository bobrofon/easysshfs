package ru.nsu.bobrofon.easysshfs

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.support.v4.widget.DrawerLayout
import android.widget.Toast

import com.topjohnwu.superuser.Shell

import ru.nsu.bobrofon.easysshfs.log.LogFragment
import ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint.EditFragment
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountpointFragment


class EasySSHFSActivity : AppCompatActivity(), NavigationDrawerFragment.NavigationDrawerCallbacks,
    MountpointFragment.OnFragmentInteractionListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private lateinit var navigationDrawerFragment: NavigationDrawerFragment

    /**
     * Used to store the last screen title. For use in [.restoreActionBar].
     */
    private lateinit var screenTitle: CharSequence
    private lateinit var fragments: Array<EasySSHFSFragment>

    lateinit var shell: Shell
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        shell = initNewShell()

        VersionUpdater(applicationContext).update()

        fragments = arrayOf(MountpointFragment(), LogFragment())

        setContentView(R.layout.activity_easy_sshfs)

        screenTitle = title

        navigationDrawerFragment =
            supportFragmentManager.findFragmentById(R.id.navigation_drawer) as NavigationDrawerFragment
        navigationDrawerFragment.setUp(
            R.id.navigation_drawer,
            findViewById<View>(R.id.drawer_layout) as DrawerLayout
        )

        fragments.forEach { it.setDrawerStatus(navigationDrawerFragment) }
    }

    override fun onNavigationDrawerItemSelected(position: Int) {
        // update the main content by replacing fragments
        try {
            val fragmentManager = supportFragmentManager
            var backStackCount = fragmentManager.backStackEntryCount
            while (backStackCount-- > 0) {
                fragmentManager.popBackStack()
            }

            fragmentManager.beginTransaction().replace(R.id.container, fragments[position]).commit()
        } catch (e: ArrayIndexOutOfBoundsException) {
            finish()
        }

    }

    fun onSectionAttached(titleId: Int) {
        screenTitle = getString(titleId)
    }

    private fun restoreActionBar() {
        val actionBar = supportActionBar!!
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.title = screenTitle
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!navigationDrawerFragment.isDrawerOpen) {
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
        editFragment.setDrawerStatus(navigationDrawerFragment)

        val fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction().replace(R.id.container, editFragment)
            .addToBackStack(null).commit()
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
