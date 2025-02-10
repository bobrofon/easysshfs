// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.settings

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import ru.nsu.bobrofon.easysshfs.EasySSHFSActivity
import ru.nsu.bobrofon.easysshfs.R

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var viewModelFactory: SettingsViewModel.Factory

    private val viewModel: SettingsViewModel by viewModels { viewModelFactory }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val autoMountInForegroundServiceSwitch =
            findPreference<SwitchPreferenceCompat>(Settings.autoMountInForegroundService.name)?.apply {
                setOnPreferenceChangeListener { _, value ->
                    viewModel.setAutoMountInForegroundService(value as Boolean)
                    true
                }
            }

        viewModel.autoMountInForegroundService.observe(this) {
            autoMountInForegroundServiceSwitch?.isChecked = it
        }

        val themeModeList = findPreference<ListPreference>(Settings.themeMode.name)?.apply {
            setOnPreferenceChangeListener { _, value ->
                viewModel.setThemeMode(value as String)
                true
            }
        }

        viewModel.themeMode.observe(this) {
            themeModeList?.value = it.preferenceValue
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        viewModelFactory = SettingsViewModel.Factory(SettingsRepository(context.settingsDataStore))

        (activity as? EasySSHFSActivity)?.onSectionAttached(R.string.settings_title)
    }
}
