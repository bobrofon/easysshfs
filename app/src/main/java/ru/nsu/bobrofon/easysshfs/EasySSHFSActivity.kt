// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.topjohnwu.superuser.BusyBoxInstaller
import com.topjohnwu.superuser.Shell

import ru.nsu.bobrofon.easysshfs.log.LogFragment
import ru.nsu.bobrofon.easysshfs.mountpointlist.AutoMountChangeObserver
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList
import ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint.EditFragment
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountpointFragment


private const val TAG = "EasySSHFSActivity"
private const val PERMISSION_REQUEST_CODE = 1

class EasySSHFSActivity : AppCompatActivity(), NavigationDrawerFragment.NavigationDrawerCallbacks,
    MountpointFragment.OnFragmentInteractionListener, AutoMountChangeObserver {

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

        ensureAllPermissionsGranted()
        MountPointsList.instance(applicationContext).registerAutoMountObserver(this)
    }

    override fun onDestroy() {
        MountPointsList.instance(applicationContext).unregisterAutoMountObserver(this)
        super.onDestroy()
    }

    private fun ensureAllPermissionsGranted() {
        val allPermissions = arrayListOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            allPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            allPermissions.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        val permissions = allPermissions.filter {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (permissions.isEmpty()) {
            Log.d(TAG, "all permissions are granted")
        } else {
            permissions.forEach {
                Log.i(TAG, "$it permission is missed")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (i in permissions.indices) {
                Log.d(
                    TAG,
                    "${permissions[i]} is ${if (grantResults[i] != PackageManager.PERMISSION_GRANTED) "not " else ""}granted"
                )
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
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
            return Shell.getShell()
        }

        init {
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setInitializers(BusyBoxInstaller::class::java.get())
            )
        }
    }

    override fun onAutoMountChanged(isAutoMountRequired: Boolean) {
        if (isAutoMountRequired) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(
                    Intent(applicationContext, EasySSHFSService::class.java))
            } else {
                applicationContext.startService(
                    Intent(applicationContext, EasySSHFSService::class.java))
            }
        } else {
            stopService(Intent(applicationContext, EasySSHFSService::class.java))
        }
    }
}
