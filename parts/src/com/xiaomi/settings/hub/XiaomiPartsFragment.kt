/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaomi.settings.hub

import android.view.View
import com.xiaomi.settings.utils.applyBottomInsets
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.android.settingslib.widget.SliderPreference
import com.xiaomi.settings.R
import com.xiaomi.settings.battery.ChargingControlActivity
import com.xiaomi.settings.charge.ChargeActivity
import com.xiaomi.settings.corecontrol.CoreControlActivity
import com.xiaomi.settings.dirac.DiracActivity
import com.xiaomi.settings.display.KcalSettingsActivity
import com.xiaomi.settings.display.LcdFeaturesPreferenceActivity
import com.xiaomi.settings.gamebar.GameBarSettingsActivity
import com.xiaomi.settings.refreshrate.RefreshActivity
import com.xiaomi.settings.speaker.ClearSpeakerActivity
import com.xiaomi.settings.thermal.ThermalActivity
import com.xiaomi.settings.utils.HapticUtils

class XiaomiPartsFragment : SettingsBasePreferenceFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBottomInsets()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.xiaomi_parts, rootKey)

        bindActivity(KEY_GAME_BAR, GameBarSettingsActivity::class.java)
        bindActivity(KEY_CORE_CONTROL, CoreControlActivity::class.java)
        bindActivity(KEY_CHARGING_CONTROL, ChargingControlActivity::class.java)
        bindActivity(KEY_DIRAC, DiracActivity::class.java)
        bindActivity(KEY_CLEAR_SPEAKER, ClearSpeakerActivity::class.java)
        bindActivity(KEY_KCAL, KcalSettingsActivity::class.java)
        bindActivity(KEY_LCD_FEATURES, LcdFeaturesPreferenceActivity::class.java)
        bindActivity(KEY_REFRESH_RATE, RefreshActivity::class.java)
        bindActivity(KEY_THERMAL, ThermalActivity::class.java)
        bindActivity(KEY_CHARGE_BYPASS, ChargeActivity::class.java)

        findPreference<SliderPreference>(KEY_HAPTIC_LEVEL)?.apply {
            if (!HapticUtils.isSupported()) {
                isEnabled = false
                summary = getString(R.string.haptic_level_summary_incompatible)
                return@apply
            }

            val level = normalizeHapticLevel(
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getInt(HapticUtils.PREF_LEVEL, HAPTIC_DEFAULT)
            )
            sliderIncrement = HAPTIC_STEP
            setTickVisible(true)
            value = level
            summary = getString(R.string.haptic_level_summary, level)
            setOnPreferenceChangeListener { _, newValue ->
                val newLevel = normalizeHapticLevel(newValue as Int)
                summary = getString(R.string.haptic_level_summary, newLevel)
                HapticUtils.applyLevel(requireContext(), newLevel, true)
                true
            }
        }
    }

    private fun normalizeHapticLevel(value: Int): Int {
        val maxAligned = HAPTIC_MAX - (HAPTIC_MAX - HAPTIC_MIN) % HAPTIC_STEP
        val clamped = value.coerceIn(HAPTIC_MIN, maxAligned)
        return HAPTIC_MIN + ((clamped - HAPTIC_MIN + HAPTIC_STEP / 2) / HAPTIC_STEP) * HAPTIC_STEP
    }

    private fun bindActivity(key: String, activity: Class<*>) {
        findPreference<Preference>(key)?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), activity))
            true
        }
    }

    companion object {
        private const val KEY_GAME_BAR = "parts_game_bar"
        private const val KEY_CORE_CONTROL = "parts_core_control"
        private const val KEY_CHARGING_CONTROL = "parts_charging_control"
        private const val KEY_DIRAC = "parts_dirac"
        private const val KEY_CLEAR_SPEAKER = "parts_clear_speaker"
        private const val KEY_KCAL = "parts_kcal"
        private const val KEY_LCD_FEATURES = "parts_lcd_features"
        private const val KEY_REFRESH_RATE = "parts_refresh_rate"
        private const val KEY_THERMAL = "parts_thermal"
        private const val KEY_CHARGE_BYPASS = "parts_charge_bypass"
        private const val KEY_HAPTIC_LEVEL = HapticUtils.PREF_LEVEL
        private const val HAPTIC_DEFAULT = 80
        private const val HAPTIC_MIN = 0
        private const val HAPTIC_MAX = 100
        private const val HAPTIC_STEP = 5
    }
}
