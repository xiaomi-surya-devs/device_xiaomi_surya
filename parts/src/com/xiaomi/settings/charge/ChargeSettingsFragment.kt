/*
 * SPDX-FileCopyrightText: 2023 Paranoid Android
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.charge

import android.view.View
import com.xiaomi.settings.utils.applyBottomInsets
import android.os.Bundle
import androidx.preference.Preference
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.xiaomi.settings.R

class ChargeSettingsFragment : SettingsBasePreferenceFragment(),
    Preference.OnPreferenceChangeListener {

    private var bypassCharge: MainSwitchPreference? = null
    private lateinit var chargeUtils: ChargeUtils

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBottomInsets()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.charge_settings, rootKey)
        chargeUtils = ChargeUtils(requireContext())

        bypassCharge = findPreference(ChargeUtils.PREF_BYPASS_CHARGE)
        if (!chargeUtils.isBypassChargeSupported()) {
            bypassCharge?.apply {
                isEnabled = false
                summary = getString(R.string.charge_bypass_unavailable)
            }
            return
        }

        bypassCharge?.apply {
            isChecked = chargeUtils.isBypassChargeEnabled()
            onPreferenceChangeListener = this@ChargeSettingsFragment
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference.key == ChargeUtils.PREF_BYPASS_CHARGE) {
            val enabled = newValue as Boolean
            chargeUtils.enableBypassCharge(enabled)
            bypassCharge?.isChecked = enabled
            return true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        if (chargeUtils.isBypassChargeSupported()) {
            bypassCharge?.isChecked = chargeUtils.isBypassChargeEnabled()
        }
    }

    override fun onDestroyView() {
        bypassCharge?.onPreferenceChangeListener = null
        bypassCharge = null
        super.onDestroyView()
    }
}
