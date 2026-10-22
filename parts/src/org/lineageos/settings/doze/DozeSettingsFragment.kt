/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2024 The LineageOS Project
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
package org.lineageos.settings.doze

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import org.lineageos.settings.R
import org.lineageos.settings.utils.FileUtils

class DozeSettingsFragment : SettingsBasePreferenceFragment(), OnPreferenceChangeListener {
    private var mAlwaysOnDisplayPreference: SwitchPreferenceCompat? = null
    private var mDozeBrightnessPreference: ListPreference? = null
    private var mWakeOnGesturePreference: SwitchPreferenceCompat? = null
    private var mPickUpPreference: SwitchPreferenceCompat? = null
    private var mHandwavePreference: SwitchPreferenceCompat? = null
    private var mPocketPreference: SwitchPreferenceCompat? = null

    private val mHandler = Handler()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.doze_settings, rootKey)

        val prefs =
            requireActivity().getSharedPreferences("doze_settings", Activity.MODE_PRIVATE)
        if (savedInstanceState == null && !prefs.getBoolean("first_help_shown", false)) {
            showHelp()
        }

        val dozeEnabled = DozeUtils.isDozeEnabled(requireActivity())

        val switchBar = findPreference<MainSwitchPreference>(DozeUtils.DOZE_ENABLE)
        switchBar?.onPreferenceChangeListener = this
        switchBar?.isChecked = dozeEnabled

        mAlwaysOnDisplayPreference =
            findPreference(DozeUtils.ALWAYS_ON_DISPLAY)
        mAlwaysOnDisplayPreference?.isEnabled = dozeEnabled
        mAlwaysOnDisplayPreference?.isChecked = DozeUtils.isAlwaysOnEnabled(requireActivity())
        mAlwaysOnDisplayPreference?.onPreferenceChangeListener = this

        mDozeBrightnessPreference = findPreference(DozeUtils.DOZE_BRIGHTNESS_KEY)
        mDozeBrightnessPreference?.isEnabled =
            dozeEnabled && DozeUtils.isAlwaysOnEnabled(requireActivity())
        mDozeBrightnessPreference?.onPreferenceChangeListener = this

        val gesturesCategory =
            findPreference<PreferenceCategory>(DozeUtils.CATEG_GESTURES)
        val pickupSensorCategory =
            findPreference<PreferenceCategory>(DozeUtils.CATEG_PICKUP_SENSOR)
        val proximitySensorCategory =
            findPreference<PreferenceCategory>(DozeUtils.CATEG_PROX_SENSOR)

        mWakeOnGesturePreference =
            findPreference(DozeUtils.WAKE_ON_GESTURE_KEY)
        mWakeOnGesturePreference?.isEnabled = dozeEnabled
        mWakeOnGesturePreference?.onPreferenceChangeListener = this

        mPickUpPreference = findPreference(DozeUtils.GESTURE_PICK_UP_KEY)
        mPickUpPreference?.isEnabled = dozeEnabled
        mPickUpPreference?.onPreferenceChangeListener = this

        mHandwavePreference =
            findPreference(DozeUtils.GESTURE_HAND_WAVE_KEY)
        mHandwavePreference?.isEnabled = dozeEnabled
        mHandwavePreference?.onPreferenceChangeListener = this

        mPocketPreference = findPreference(DozeUtils.GESTURE_POCKET_KEY)
        mPocketPreference?.isEnabled = dozeEnabled
        mPocketPreference?.onPreferenceChangeListener = this

        // Hide proximity sensor related features if the device doesn't support them
        if (!DozeUtils.getProxCheckBeforePulse(requireActivity())) {
            proximitySensorCategory?.let { preferenceScreen.removePreference(it) }
        }

        // Hide AOD and doze brightness if not supported and set all its dependents otherwise
        if (!DozeUtils.alwaysOnDisplayAvailable(requireActivity())) {
            mAlwaysOnDisplayPreference?.let { preferenceScreen.removePreference(it) }
            mDozeBrightnessPreference?.let { preferenceScreen.removePreference(it) }
        } else {
            if (!FileUtils.isFileWritable(DozeUtils.DOZE_MODE_PATH)) {
                mDozeBrightnessPreference?.let { preferenceScreen.removePreference(it) }
            } else {
                DozeUtils.updateDozeBrightnessIcon(requireContext(), mDozeBrightnessPreference!!)
            }
            gesturesCategory?.dependency = DozeUtils.ALWAYS_ON_DISPLAY
            pickupSensorCategory?.dependency = DozeUtils.ALWAYS_ON_DISPLAY
            proximitySensorCategory?.dependency = DozeUtils.ALWAYS_ON_DISPLAY
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (DozeUtils.DOZE_ENABLE == preference.key) {
            val isChecked = newValue as Boolean
            DozeUtils.enableDoze(requireActivity(), isChecked)

            if (!isChecked) {
                DozeUtils.enableAlwaysOn(requireActivity(), false)
                mAlwaysOnDisplayPreference?.isChecked = false
                mDozeBrightnessPreference?.value = DozeUtils.DOZE_BRIGHTNESS_LBM
                DozeUtils.updateDozeBrightnessIcon(requireContext(), mDozeBrightnessPreference!!)
                mPickUpPreference?.isChecked = false
                mHandwavePreference?.isChecked = false
                mPocketPreference?.isChecked = false
            }
            mAlwaysOnDisplayPreference?.isEnabled = isChecked
            mDozeBrightnessPreference?.isEnabled =
                isChecked && DozeUtils.isAlwaysOnEnabled(requireActivity())
            mWakeOnGesturePreference?.isEnabled = isChecked
            mPickUpPreference?.isEnabled = isChecked
            mHandwavePreference?.isEnabled = isChecked
            mPocketPreference?.isEnabled = isChecked
        } else if (DozeUtils.ALWAYS_ON_DISPLAY == preference.key) {
            DozeUtils.enableAlwaysOn(requireActivity(), newValue as Boolean)
            if (!(newValue as Boolean)) {
                mDozeBrightnessPreference?.value = DozeUtils.DOZE_BRIGHTNESS_LBM
                DozeUtils.setDozeMode(DozeUtils.DOZE_BRIGHTNESS_LBM)
            } else {
                mPickUpPreference?.isChecked = false
                mHandwavePreference?.isChecked = false
                mPocketPreference?.isChecked = false
            }
            mDozeBrightnessPreference?.isEnabled = newValue
        } else if (DozeUtils.DOZE_BRIGHTNESS_KEY == preference.key) {
            if (DozeUtils.DOZE_BRIGHTNESS_AUTO != (newValue as String)) {
                DozeUtils.setDozeMode(newValue)
            }
        }

        mHandler.post {
            DozeUtils.checkDozeService(requireActivity())
            mDozeBrightnessPreference?.let { DozeUtils.updateDozeBrightnessIcon(requireContext(), it) }
        }

        return true
    }

    private fun showHelp() {
        val helpDialog = AlertDialog.Builder(requireActivity())
            .setTitle(R.string.doze_settings_help_title)
            .setMessage(R.string.doze_settings_help_text)
            .setPositiveButton(
                R.string.dialog_ok
            ) { dialog, _ ->
                requireActivity()
                    .getSharedPreferences("doze_settings", Activity.MODE_PRIVATE)
                    .edit()
                    .putBoolean("first_help_shown", true)
                    .commit()
                dialog.cancel()
            }
            .create()
        helpDialog.show()
    }
}
