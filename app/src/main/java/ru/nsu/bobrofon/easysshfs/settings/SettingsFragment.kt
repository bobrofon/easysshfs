// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.settings

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import ru.nsu.bobrofon.easysshfs.EasySSHFSActivity
import ru.nsu.bobrofon.easysshfs.R

class SettingsFragment(viewModelFactory: SettingsViewModel.Factory) : PreferenceFragmentCompat() {

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
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as? EasySSHFSActivity)?.onSectionAttached(R.string.settings_title)
    }
}
