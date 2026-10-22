/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.display.AmbientDisplayConfiguration
import android.os.PowerManager
import android.os.SystemClock
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.PreferenceManager
import org.lineageos.settings.R
import org.lineageos.settings.utils.FileUtils

object DozeUtils {
    private const val TAG = "DozeUtils"
    private const val DEBUG = false

    private const val DOZE_INTENT = "com.android.systemui.doze.pulse"

    const val DOZE_ENABLE = "doze_enable"
    const val ALWAYS_ON_DISPLAY = "always_on_display"
    const val DOZE_BRIGHTNESS_KEY = "doze_brightness"
    const val WAKE_ON_GESTURE_KEY = "wake_on_gesture"
    const val CATEG_GESTURES = "gestures"
    const val CATEG_PICKUP_SENSOR = "pickup_sensor"
    const val CATEG_PROX_SENSOR = "proximity_sensor"

    const val GESTURE_PICK_UP_KEY = "gesture_pick_up"
    const val GESTURE_HAND_WAVE_KEY = "gesture_hand_wave"
    const val GESTURE_POCKET_KEY = "gesture_pocket"

    const val DOZE_MODE_PATH = "/sys/devices/platform/soc/soc:qcom,dsi-display/doze_mode"
    const val DOZE_MODE_HBM = "1"
    const val DOZE_MODE_LBM = "0"

    const val DOZE_BRIGHTNESS_LBM = "0"
    const val DOZE_BRIGHTNESS_HBM = "1"
    const val DOZE_BRIGHTNESS_AUTO = "2"

    @JvmStatic
    fun onBootCompleted(context: Context) {
        checkDozeService(context)
        restoreDozeModes(context)
    }

    @JvmStatic
    fun startService(context: Context) {
        if (DEBUG) {
            Log.d(TAG, "Starting service")
        }
        context.startServiceAsUser(Intent(context, DozeService::class.java), UserHandle.CURRENT)
    }

    @JvmStatic
    fun stopService(context: Context) {
        if (DEBUG) {
            Log.d(TAG, "Stopping service")
        }
        context.stopServiceAsUser(Intent(context, DozeService::class.java), UserHandle.CURRENT)
    }

    @JvmStatic
    fun checkDozeService(context: Context) {
        if (isDozeEnabled(context) && (isAlwaysOnEnabled(context) || sensorsEnabled(context))) {
            startService(context)
        } else {
            stopService(context)
        }
    }

    private fun restoreDozeModes(context: Context) {
        if (isAlwaysOnEnabled(context) && !isDozeAutoBrightnessEnabled(context)) {
            setDozeMode(
                PreferenceManager.getDefaultSharedPreferences(context).getString(
                    DOZE_BRIGHTNESS_KEY, DOZE_BRIGHTNESS_LBM
                )!!
            )
        }
    }

    @JvmStatic
    fun getProxCheckBeforePulse(context: Context): Boolean {
        return try {
            val con = context.createPackageContext("com.android.systemui", 0)
            val id = con.resources.getIdentifier(
                "doze_proximity_check_before_pulse", "bool", "com.android.systemui"
            )
            con.resources.getBoolean(id)
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    @JvmStatic
    fun enableDoze(context: Context, enable: Boolean): Boolean {
        return Settings.Secure.putInt(context.contentResolver, Settings.Secure.DOZE_ENABLED, if (enable) 1 else 0)
    }

    @JvmStatic
    fun isDozeEnabled(context: Context): Boolean {
        return Settings.Secure.getInt(context.contentResolver, Settings.Secure.DOZE_ENABLED, 1) != 0
    }

    @JvmStatic
    fun wakeOrLaunchDozePulse(context: Context) {
        if (isWakeOnGestureEnabled(context)) {
            if (DEBUG) {
                Log.d(TAG, "Wake up display")
            }
            val powerManager = context.getSystemService(PowerManager::class.java)
            powerManager.wakeUp(SystemClock.uptimeMillis(), PowerManager.WAKE_REASON_GESTURE, TAG)
        } else {
            if (DEBUG) {
                Log.d(TAG, "Launch doze pulse")
            }
            context.sendBroadcastAsUser(
                Intent(DOZE_INTENT), UserHandle(UserHandle.USER_CURRENT)
            )
        }
    }

    @JvmStatic
    fun enableAlwaysOn(context: Context, enable: Boolean): Boolean {
        return Settings.Secure.putIntForUser(
            context.contentResolver, Settings.Secure.DOZE_ALWAYS_ON,
            if (enable) 1 else 0, UserHandle.USER_CURRENT
        )
    }

    @JvmStatic
    fun isAlwaysOnEnabled(context: Context): Boolean {
        val enabledByDefault = context.resources.getBoolean(
            com.android.internal.R.bool.config_dozeAlwaysOnEnabled
        )

        return Settings.Secure.getIntForUser(
            context.contentResolver, Settings.Secure.DOZE_ALWAYS_ON,
            if (alwaysOnDisplayAvailable(context) && enabledByDefault) 1 else 0,
            UserHandle.USER_CURRENT
        ) != 0
    }

    @JvmStatic
    fun alwaysOnDisplayAvailable(context: Context): Boolean {
        return AmbientDisplayConfiguration(context).alwaysOnAvailable()
    }

    @JvmStatic
    fun setDozeMode(value: String): Boolean {
        return FileUtils.writeLine(DOZE_MODE_PATH, value)
    }

    @JvmStatic
    fun isDozeAutoBrightnessEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(DOZE_BRIGHTNESS_KEY, DOZE_BRIGHTNESS_LBM)
            == DOZE_BRIGHTNESS_AUTO
    }

    private fun isGestureEnabled(context: Context, gesture: String): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(gesture, false)
    }

    @JvmStatic
    fun isWakeOnGestureEnabled(context: Context): Boolean {
        return isGestureEnabled(context, WAKE_ON_GESTURE_KEY)
    }

    @JvmStatic
    fun isPickUpEnabled(context: Context): Boolean {
        return isGestureEnabled(context, GESTURE_PICK_UP_KEY)
    }

    @JvmStatic
    fun isHandwaveGestureEnabled(context: Context): Boolean {
        return isGestureEnabled(context, GESTURE_HAND_WAVE_KEY)
    }

    @JvmStatic
    fun isPocketGestureEnabled(context: Context): Boolean {
        return isGestureEnabled(context, GESTURE_POCKET_KEY)
    }

    @JvmStatic
    fun sensorsEnabled(context: Context): Boolean {
        return isDozeAutoBrightnessEnabled(context) || isHandwaveGestureEnabled(context)
                || isPickUpEnabled(context) || isPocketGestureEnabled(context)
    }

    @JvmStatic
    fun getSensor(sm: SensorManager, type: String): Sensor? {
        for (sensor in sm.getSensorList(Sensor.TYPE_ALL)) {
            if (type == sensor.stringType) {
                return sensor
            }
        }
        return null
    }

    @JvmStatic
    fun updateDozeBrightnessIcon(context: Context, preference: ListPreference) {
        when (
            PreferenceManager.getDefaultSharedPreferences(context).getString(
                DOZE_BRIGHTNESS_KEY, DOZE_BRIGHTNESS_LBM
            )
        ) {
            DOZE_BRIGHTNESS_LBM -> preference.setIcon(R.drawable.ic_doze_brightness_low)
            DOZE_BRIGHTNESS_HBM -> preference.setIcon(R.drawable.ic_doze_brightness_high)
            DOZE_BRIGHTNESS_AUTO -> preference.setIcon(R.drawable.ic_doze_brightness_auto)
        }
    }
}
