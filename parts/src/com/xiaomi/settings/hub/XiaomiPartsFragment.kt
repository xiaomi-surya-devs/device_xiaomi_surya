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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.xiaomi.settings.R
import com.xiaomi.settings.battery.ChargingControlActivity
import com.xiaomi.settings.charge.ChargeActivity
import com.xiaomi.settings.corecontrol.CoreControlActivity
import com.xiaomi.settings.dirac.DiracActivity
import com.xiaomi.settings.display.KcalSettingsActivity
import com.xiaomi.settings.display.LcdFeaturesPreferenceActivity
import com.xiaomi.settings.fpsinfo.FpsInfoService
import com.xiaomi.settings.gamebar.GameBarSettingsActivity
import com.xiaomi.settings.refreshrate.RefreshActivity
import com.xiaomi.settings.speaker.ClearSpeakerActivity
import com.xiaomi.settings.thermal.ThermalActivity
import com.xiaomi.settings.utils.HapticUtils

class XiaomiPartsFragment : SettingsBasePreferenceFragment() {

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

        findPreference<SwitchPreferenceCompat>(KEY_FPS_INFO)?.apply {
            isChecked = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(FpsInfoService.PREF_FPS_INFO, false)
            setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                if (enabled && !Settings.canDrawOverlays(requireContext())) {
                    Toast.makeText(
                        requireContext(),
                        R.string.fps_overlay_permission_required,
                        Toast.LENGTH_LONG
                    ).show()
                    runCatching {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${requireContext().packageName}")
                            )
                        )
                    }
                    return@setOnPreferenceChangeListener false
                }

                val serviceIntent = Intent(requireContext(), FpsInfoService::class.java)
                if (enabled) {
                    requireContext().startService(serviceIntent)
                } else {
                    requireContext().stopService(serviceIntent)
                }
                true
            }
        }

        findPreference<SeekBarPreference>(KEY_HAPTIC_LEVEL)?.apply {
            if (!HapticUtils.isSupported()) {
                isEnabled = false
                summary = getString(R.string.haptic_level_summary_incompatible)
                return@apply
            }

            val level = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt(HapticUtils.PREF_LEVEL, 80)
            value = level
            summary = level.toString()
            setOnPreferenceChangeListener { _, newValue ->
                val newLevel = newValue as Int
                summary = newLevel.toString()
                HapticUtils.applyLevel(requireContext(), newLevel, true)
                true
            }
        }
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
        private const val KEY_FPS_INFO = "fps_info"
        private const val KEY_HAPTIC_LEVEL = HapticUtils.PREF_LEVEL
    }
}
