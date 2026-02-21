// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs

import android.Manifest
import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ru.nsu.bobrofon.easysshfs.log.AppLog
import ru.nsu.bobrofon.easysshfs.mountpointlist.MountPointsList
import ru.nsu.bobrofon.easysshfs.settings.SettingsRepository
import ru.nsu.bobrofon.easysshfs.settings.ThemeMode
import ru.nsu.bobrofon.easysshfs.settings.settingsDataStore


private const val TAG = "EasySSHFSActivity"
private const val PERMISSION_REQUEST_CODE = 1

class EasySSHFSActivity : AppCompatActivity() {

    val shell: Shell by lazy { initNewShell() }

    private lateinit var viewModel: EasySSHFSViewModel

    private val drawerLayout: DrawerLayout by lazy { findViewById(R.id.drawer_layout) }
    private val navController: NavController by lazy {
        (supportFragmentManager.findFragmentById(R.id.main_content) as NavHostFragment).navController
    }
    private val navigationView: NavigationView by lazy { findViewById(R.id.nav_view) }

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsRepository = SettingsRepository(applicationContext.settingsDataStore)

        viewModel = ViewModelProvider(
            this,
            EasySSHFSViewModel.Factory(
                settingsRepository,
                MountPointsList.instance(applicationContext)
            )
        )[EasySSHFSViewModel::class.java]

        AppLog.instance().addMessage(
            "EasySSHFS ${BuildConfig.VERSION_NAME} " + "(${BuildConfig.VERSION_CODE}) ${BuildConfig.BUILD_TYPE}"
        )
        VersionUpdater(applicationContext).update()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // night mode is not persisted by framework, so we need to update it
            // BEFORE content view is set
            val themeMode = runBlocking { settingsRepository.themeMode.first() }
            updateThemeMode(themeMode)
        }

        setContentView(R.layout.activity_easy_sshfs)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.mountpointFragment,
                R.id.logFragment,
                R.id.settingsFragment
            ),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navigationView.setupWithNavController(navController)

        val exitMenuItem = navigationView.menu.findItem(R.id.exitAction)
        exitMenuItem.setOnMenuItemClickListener { _ ->
            finish()
            true
        }

        ensureAllPermissionsGranted()

        viewModel.autoMountServiceRequired.observe(this) {
            onAutoMountChanged(it)
        }

        viewModel.themeMode.observe(this) {
            updateThemeMode(it)
        }
    }

    private fun updateThemeMode(mode: ThemeMode) {
        Log.i(TAG, "change app theme mode to '$mode'")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
            uiModeManager.setApplicationNightMode(mode.nightMode)
        } else {
            AppCompatDelegate.setDefaultNightMode(mode.compatNightMode)
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
                applicationContext, it
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
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
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

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    companion object {

        fun showToast(message: CharSequence, context: Context?) {
            if (context != null) {
                val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
                toast.show()
            }
        }

        fun initNewShell(): Shell {
            return ShellBuilder.sharedShell()
        }

        init {
            Shell.enableVerboseLogging = BuildConfig.DEBUG
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
