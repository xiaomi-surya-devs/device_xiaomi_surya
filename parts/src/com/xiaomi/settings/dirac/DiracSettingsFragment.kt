/*
 * SPDX-FileCopyrightText: 2018 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.dirac

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.xiaomi.settings.R

class DiracSettingsFragment : SettingsBasePreferenceFragment(),
    Preference.OnPreferenceChangeListener {

    companion object {
        private const val PREF_ENABLE = "dirac_enable"
        private const val PREF_HEADSET = "dirac_headset_pref"
        private const val PREF_PRESET = "dirac_preset_pref"
        private const val PREF_SCENE = "scenario_selection"
    }

    private var enableSwitch: MainSwitchPreference? = null
    private var headsetPref: ListPreference? = null
    private var presetPref: ListPreference? = null
    private var scenePref: ListPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.dirac_settings, rootKey)

        val context = requireContext()
        DiracUtils.initialize(context)
        val supported = DiracUtils.isSupported()
        val enabled = supported && DiracUtils.isDiracEnabled()

        enableSwitch = findPreference<MainSwitchPreference>(PREF_ENABLE)?.apply {
            isEnabled = supported
            isChecked = enabled
            if (!supported) {
                summary = getString(R.string.dirac_unsupported)
            }
            setOnPreferenceChangeListener { _, newValue ->
                val newEnabled = newValue as Boolean
                DiracUtils.setMusic(newEnabled)
                updateDependentPreferences(newEnabled)
                true
            }
        }

        headsetPref = findPreference<ListPreference>(PREF_HEADSET)?.apply {
            isEnabled = supported && enabled
            onPreferenceChangeListener = this@DiracSettingsFragment
        }
        presetPref = findPreference<ListPreference>(PREF_PRESET)?.apply {
            isEnabled = supported && enabled
            onPreferenceChangeListener = this@DiracSettingsFragment
        }
        scenePref = findPreference<ListPreference>(PREF_SCENE)?.apply {
            isEnabled = supported && enabled
            onPreferenceChangeListener = this@DiracSettingsFragment
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        return when (preference.key) {
            PREF_HEADSET -> {
                DiracUtils.setHeadsetType(newValue.toString().toInt())
                true
            }
            PREF_PRESET -> {
                DiracUtils.setLevel(newValue.toString())
                true
            }
            PREF_SCENE -> {
                DiracUtils.setScenario(newValue.toString().toInt())
                true
            }
            else -> false
        }
    }

    private fun updateDependentPreferences(enabled: Boolean) {
        headsetPref?.isEnabled = enabled
        presetPref?.isEnabled = enabled
        scenePref?.isEnabled = enabled
    }

    override fun onDestroyView() {
        enableSwitch?.setOnPreferenceChangeListener(null)
        headsetPref?.onPreferenceChangeListener = null
        presetPref?.onPreferenceChangeListener = null
        scenePref?.onPreferenceChangeListener = null
        enableSwitch = null
        headsetPref = null
        presetPref = null
        scenePref = null
        super.onDestroyView()
    }
}
