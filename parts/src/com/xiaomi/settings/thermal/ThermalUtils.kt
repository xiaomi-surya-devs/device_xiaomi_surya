/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.thermal

import android.content.Context
import android.content.Intent
import android.os.SystemProperties
import android.util.Log
import androidx.preference.PreferenceManager
import com.xiaomi.settings.utils.fileExists
import com.xiaomi.settings.utils.writeLine

class ThermalUtils(context: Context) {

    companion object {
        private const val TAG = "ThermalUtils"
        private const val PREF_PACKAGE_PROFILE = "thermal_profile_"
        private const val THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig"
        private const val PERF_PROFILE_PROP = "sys.perf_profile"

        const val STATE_DEFAULT = 0
        const val STATE_PERFORMANCE = 1
        const val STATE_CAMERA = 2
        const val STATE_DIALER = 3

        private val THERMAL_VALUES = intArrayOf(10, 20, 12, 8)

        fun startService(context: Context) {
            runCatching {
                context.startService(Intent(context, ThermalService::class.java))
            }.onFailure { Log.e(TAG, "Failed to start ThermalService", it) }
        }
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun isSupported(): Boolean = fileExists(THERMAL_SCONFIG)

    fun getProfile(packageName: String): Int =
        prefs.getInt(PREF_PACKAGE_PROFILE + packageName, STATE_DEFAULT)
            .coerceIn(STATE_DEFAULT, STATE_DIALER)

    fun setProfile(packageName: String, profile: Int) {
        prefs.edit()
            .putInt(PREF_PACKAGE_PROFILE + packageName, profile.coerceIn(STATE_DEFAULT, STATE_DIALER))
            .apply()
    }

    fun applyProfile(packageName: String) {
        val profile = getProfile(packageName)
        writeLine(THERMAL_SCONFIG, THERMAL_VALUES[profile].toString())
        setPerformanceProperty(profile == STATE_PERFORMANCE)
    }

    fun applyDefault() {
        writeLine(THERMAL_SCONFIG, THERMAL_VALUES[STATE_DEFAULT].toString())
        setPerformanceProperty(false)
    }

    private fun setPerformanceProperty(enabled: Boolean) {
        runCatching {
            SystemProperties.set(PERF_PROFILE_PROP, if (enabled) "1" else "0")
        }.onFailure { Log.e(TAG, "Failed to set $PERF_PROFILE_PROP", it) }
    }
}
