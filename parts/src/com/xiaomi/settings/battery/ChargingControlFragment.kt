/*
 * Copyright (C) 2023-2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.battery

import android.view.View
import com.xiaomi.settings.utils.applyBottomInsets
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import com.android.settingslib.widget.SliderPreference
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.xiaomi.settings.R

class ChargingControlFragment : SettingsBasePreferenceFragment(),
    Preference.OnPreferenceChangeListener {

    private var mLimitPref: SliderPreference? = null
    private var mStartLimitPref: SliderPreference? = null
    private var mMasterSwitch: MainSwitchPreference? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBottomInsets()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.charging_control_settings, rootKey)

        mLimitPref = findPreference<SliderPreference>(BatteryUtils.PREF_CHARGING_LIMIT)?.apply {
            sliderIncrement = 5
            setTickVisible(true)
            onPreferenceChangeListener = this@ChargingControlFragment
            updateSummary(value)
        }

        mStartLimitPref = findPreference<SliderPreference>(
            BatteryUtils.PREF_CHARGING_START_LIMIT
        )?.apply {
            sliderIncrement = 5
            setTickVisible(true)
            onPreferenceChangeListener = this@ChargingControlFragment
            updateStartSummary(value)
        }

        mMasterSwitch = findPreference<MainSwitchPreference>(BatteryUtils.PREF_CHARGING_CTRL)?.apply {
            onPreferenceChangeListener = this@ChargingControlFragment
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        var stopAt = mLimitPref?.value ?: BatteryUtils.DEFAULT_CHARGING_LIMIT
        var startAt = mStartLimitPref?.value ?: BatteryUtils.DEFAULT_CHARGING_START_LIMIT
        var thresholdChanged = false

        if (preference == mLimitPref) {
            stopAt = newValue as Int
            thresholdChanged = true
        } else if (preference == mStartLimitPref) {
            startAt = newValue as Int
            thresholdChanged = true
        }

        if (thresholdChanged && startAt >= stopAt) {
            Toast.makeText(
                requireContext(),
                R.string.charging_threshold_error,
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (preference == mLimitPref) {
            updateSummary(stopAt)
        } else if (preference == mStartLimitPref) {
            updateStartSummary(startAt)
        }

        // Let the preference framework persist the new threshold before the service reads it.
        view?.post {
            context?.let { currentContext ->
                currentContext.startService(
                    Intent(currentContext, ChargingLimitService::class.java)
                )
            }
        }
        return true
    }

    private fun updateSummary(value: Int) {
        mLimitPref?.summary = getString(R.string.charging_limit_summary, value)
    }

    private fun updateStartSummary(value: Int) {
        mStartLimitPref?.summary = getString(R.string.charging_start_limit_summary, value)
    }

    override fun onDestroyView() {
        mLimitPref?.onPreferenceChangeListener = null
        mStartLimitPref?.onPreferenceChangeListener = null
        mMasterSwitch?.onPreferenceChangeListener = null
        mLimitPref = null
        mStartLimitPref = null
        mMasterSwitch = null
        super.onDestroyView()
    }
}
