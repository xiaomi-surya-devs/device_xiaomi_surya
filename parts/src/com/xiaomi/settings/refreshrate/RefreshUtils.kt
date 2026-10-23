/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.refreshrate

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.preference.PreferenceManager

class RefreshUtils(context: Context) {

    companion object {
        private const val TAG = "RefreshUtils"
        const val KEY_MIN_REFRESH_RATE = "min_refresh_rate"
        const val KEY_PEAK_REFRESH_RATE = "peak_refresh_rate"
        const val KEY_DEFAULT_MIN_RATE = "refresh_default_min_rate"
        const val KEY_DEFAULT_PEAK_RATE = "refresh_default_peak_rate"
        private const val PREF_PACKAGE_RATE = "refresh_rate_"

        fun startService(context: Context) {
            runCatching {
                context.startService(Intent(context, RefreshService::class.java))
            }.onFailure { Log.e(TAG, "Failed to start RefreshService", it) }
        }
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val contentResolver = context.contentResolver

    fun getPackageRate(packageName: String): Int =
        prefs.getInt(PREF_PACKAGE_RATE + packageName, 0)

    fun setPackageRate(packageName: String, rate: Int) {
        prefs.edit().putInt(PREF_PACKAGE_RATE + packageName, rate).apply()
    }

    fun captureDefaultsIfNeeded() {
        if (prefs.contains(KEY_DEFAULT_MIN_RATE) && prefs.contains(KEY_DEFAULT_PEAK_RATE)) {
            return
        }

        val defaultMin = Settings.System.getString(
            contentResolver,
            KEY_MIN_REFRESH_RATE
        ) ?: Settings.System.getFloat(contentResolver, KEY_MIN_REFRESH_RATE, 60f).toString()
        val defaultPeak = Settings.System.getString(
            contentResolver,
            KEY_PEAK_REFRESH_RATE
        ) ?: Settings.System.getFloat(contentResolver, KEY_PEAK_REFRESH_RATE, 60f).toString()

        prefs.edit()
            .putString(KEY_DEFAULT_MIN_RATE, defaultMin)
            .putString(KEY_DEFAULT_PEAK_RATE, defaultPeak)
            .apply()
    }

    fun applyRate(packageName: String) {
        captureDefaultsIfNeeded()
        val rate = getPackageRate(packageName)

        if (rate > 0) {
            putRate(KEY_MIN_REFRESH_RATE, rate.toString())
            putRate(KEY_PEAK_REFRESH_RATE, rate.toString())
        } else {
            resetToDefault()
        }
    }

    fun resetToDefault() {
        captureDefaultsIfNeeded()
        putRate(
            KEY_MIN_REFRESH_RATE,
            prefs.getString(KEY_DEFAULT_MIN_RATE, "60") ?: "60"
        )
        putRate(
            KEY_PEAK_REFRESH_RATE,
            prefs.getString(KEY_DEFAULT_PEAK_RATE, "60") ?: "60"
        )
    }

    private fun putRate(key: String, value: String) {
        try {
            Settings.System.putString(contentResolver, key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write $key=$value", e)
        }
    }
}
