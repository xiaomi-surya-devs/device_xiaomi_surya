/*
 * SPDX-FileCopyrightText: 2023 Paranoid Android
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.display

import android.os.Bundle
import android.os.SystemProperties
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.xiaomi.settings.R

class LcdFeaturesPreferenceFragment : SettingsBasePreferenceFragment(),
    Preference.OnPreferenceChangeListener {

    companion object {
        const val HBM_PROP = "persist.lcd.hbm_mode"
        const val CABC_PROP = "persist.lcd.cabc_mode"

        private const val KEY_HBM = "pref_hbm"
        private const val KEY_CABC = "pref_cabc"
    }

    private var hbmPref: ListPreference? = null
    private var cabcPref: ListPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.lcd_features_settings, rootKey)

        hbmPref = findPreference(KEY_HBM)?.apply {
            onPreferenceChangeListener = this@LcdFeaturesPreferenceFragment
        }
        cabcPref = findPreference(KEY_CABC)?.apply {
            onPreferenceChangeListener = this@LcdFeaturesPreferenceFragment
        }
    }

    override fun onResume() {
        super.onResume()
        updateValues()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val value = newValue.toString()
        when (preference.key) {
            KEY_HBM -> {
                SystemProperties.set(HBM_PROP, value)
                hbmPref?.value = value
                hbmPref?.summary = hbmPref?.entry
            }
            KEY_CABC -> {
                SystemProperties.set(CABC_PROP, value)
                cabcPref?.value = value
                cabcPref?.summary = cabcPref?.entry
            }
            else -> return false
        }
        return true
    }

    private fun updateValues() {
        hbmPref?.let { pref ->
            val value = SystemProperties.get(HBM_PROP, "0")
            pref.value = if (pref.findIndexOfValue(value) >= 0) value else "0"
            pref.summary = pref.entry
        }
        cabcPref?.let { pref ->
            val value = SystemProperties.get(CABC_PROP, "0")
            pref.value = if (pref.findIndexOfValue(value) >= 0) value else "0"
            pref.summary = pref.entry
        }
    }

    override fun onDestroyView() {
        hbmPref?.onPreferenceChangeListener = null
        cabcPref?.onPreferenceChangeListener = null
        hbmPref = null
        cabcPref = null
        super.onDestroyView()
    }
}
