// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import com.topjohnwu.superuser.Shell
import ru.nsu.bobrofon.easysshfs.log.LogFragment
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountpointFragment
import ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint.EditFragment
import ru.nsu.bobrofon.easysshfs.settings.SettingsFragment
import ru.nsu.bobrofon.easysshfs.settings.SettingsRepository
import ru.nsu.bobrofon.easysshfs.settings.SettingsViewModel
import ru.nsu.bobrofon.easysshfs.settings.settingsDataStore


private const val TAG = "EasySSHFSActivity"
private const val PERMISSION_REQUEST_CODE = 1

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

    val shell: Shell by lazy { initNewShell() }

    private lateinit var viewModel: EasySSHFSViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            EasySSHFSViewModel.Factory(
                SettingsRepository(applicationContext.settingsDataStore),
                MountPointsList.instance(applicationContext)
            )
        )[EasySSHFSViewModel::class.java]

        VersionUpdater(applicationContext).update()

        setContentView(R.layout.activity_easy_sshfs)

        screenTitle = title

        navigationDrawerFragment =
            supportFragmentManager.findFragmentById(R.id.navigation_drawer) as NavigationDrawerFragment
        navigationDrawerFragment.setUp(
            R.id.navigation_drawer,
            findViewById<View>(R.id.drawer_layout) as DrawerLayout
        )

        ensureAllPermissionsGranted()

        viewModel.autoMountServiceRequired.observe(this) {
            onAutoMountChanged(it)
        }
    }

    private fun ensureAllPermissionsGranted() {
        val allPermissions = arrayListOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
        )
        allPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
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
        val fragmentManager = supportFragmentManager
        var backStackCount = fragmentManager.backStackEntryCount
        while (backStackCount-- > 0) {
            fragmentManager.popBackStack()
        }

        val fragment = when (position) {
            0 -> MountpointFragment().apply { setDrawerStatus(navigationDrawerFragment) }
            1 -> LogFragment().apply { setDrawerStatus(navigationDrawerFragment) }
            2 -> SettingsFragment(SettingsViewModel.Factory(SettingsRepository(applicationContext.settingsDataStore)))
            else -> null
        }

        if (fragment != null) {
            fragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
        } else {
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
            Shell.setDefaultBuilder(ShellBuilder.create())
        }
    }

    private fun onAutoMountChanged(isAutoMountRequired: Boolean) {
        if (isAutoMountRequired) {
            EasySSHFSService.start(applicationContext)
        } else {
            EasySSHFSService.stop(applicationContext)
        }
    }
}
