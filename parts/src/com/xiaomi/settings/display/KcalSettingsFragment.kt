/*
 * SPDX-FileCopyrightText: 2020 ArrowOS
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.display

import android.view.View
import com.xiaomi.settings.utils.applyBottomInsets
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.xiaomi.settings.R

class KcalSettingsFragment : SettingsBasePreferenceFragment(),
    Preference.OnPreferenceChangeListener {

    private lateinit var prefs: SharedPreferences
    private var kcalSwitch: MainSwitchPreference? = null
    private var resetButton: Preference? = null
    private val sliders = mutableListOf<SeekBarPreference>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBottomInsets()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.kcal_settings, rootKey)
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        kcalSwitch = findPreference("kcal_enable")
        resetButton = findPreference("reset_default_button")

        if (!KcalUtils.isKcalSupported()) {
            kcalSwitch?.apply {
                isEnabled = false
                summary = getString(R.string.kcal_not_supported)
            }
            resetButton?.isEnabled = false
            return
        }

        configureSliders()
        KcalUtils.writeCurrentSettings(prefs)
        updateSliderStates(prefs.getBoolean("kcal_enable", false))

        kcalSwitch?.apply {
            isChecked = prefs.getBoolean("kcal_enable", false)
            setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                KcalUtils.setEnabled(enabled)
                updateSliderStates(enabled)
                true
            }
        }

        resetButton?.setOnPreferenceClickListener {
            resetSettings()
            true
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val value = newValue as Int
        when (preference.key) {
            "red_slider" -> KcalUtils.writeRgbComponent(1, value)
            "green_slider" -> KcalUtils.writeRgbComponent(2, value)
            "blue_slider" -> KcalUtils.writeRgbComponent(3, value)
            "saturation_slider" -> KcalUtils.writeNode(KcalUtils.KCAL_SATURATION_NODE, value)
            "contrast_slider" -> KcalUtils.writeNode(KcalUtils.KCAL_CONTRAST_NODE, value)
            "hue_slider" -> KcalUtils.writeNode(KcalUtils.KCAL_HUE_NODE, value)
            "brightness_slider" -> KcalUtils.writeNode(KcalUtils.KCAL_BRIGHTNESS_NODE, value)
        }
        preference.summary = value.toString()
        return true
    }

    private fun configureSliders() {
        sliders.clear()
        listOf(
            "red_slider" to 256,
            "green_slider" to 256,
            "blue_slider" to 256,
            "saturation_slider" to 383,
            "contrast_slider" to 383,
            "hue_slider" to 255,
            "brightness_slider" to 383
        ).forEach { (key, max) ->
            findPreference<SeekBarPreference>(key)?.apply {
                this.max = max
                min = when (key) {
                    "hue_slider" -> 0
                    "saturation_slider", "contrast_slider", "brightness_slider" -> 224
                    else -> 1
                }
                setUpdatesContinuously(true)
                onPreferenceChangeListener = this@KcalSettingsFragment
                summary = value.toString()
                sliders.add(this)
            }
        }
    }

    private fun updateSliderStates(enabled: Boolean) {
        sliders.forEach { it.isEnabled = enabled }
    }

    private fun resetSettings() {
        prefs.edit()
            .remove("kcal_enable")
            .remove("red_slider")
            .remove("green_slider")
            .remove("blue_slider")
            .remove("saturation_slider")
            .remove("contrast_slider")
            .remove("hue_slider")
            .remove("brightness_slider")
            .apply()

        kcalSwitch?.isChecked = false
        updateSliderStates(false)
        findPreference<SeekBarPreference>("red_slider")?.value = 256
        findPreference<SeekBarPreference>("green_slider")?.value = 256
        findPreference<SeekBarPreference>("blue_slider")?.value = 256
        findPreference<SeekBarPreference>("saturation_slider")?.value = 255
        findPreference<SeekBarPreference>("contrast_slider")?.value = 255
        findPreference<SeekBarPreference>("hue_slider")?.value = 0
        findPreference<SeekBarPreference>("brightness_slider")?.value = 255
        sliders.forEach { it.summary = it.value.toString() }
        KcalUtils.writeCurrentSettings(prefs)
    }

    override fun onDestroyView() {
        kcalSwitch?.setOnPreferenceChangeListener(null)
        resetButton?.setOnPreferenceClickListener(null)
        sliders.forEach { it.onPreferenceChangeListener = null }
        sliders.clear()
        kcalSwitch = null
        resetButton = null
        super.onDestroyView()
    }
}
