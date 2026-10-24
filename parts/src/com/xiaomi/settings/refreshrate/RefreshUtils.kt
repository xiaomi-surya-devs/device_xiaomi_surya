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
import java.util.Locale

class RefreshUtils(context: Context) {

    companion object {
        private const val TAG = "RefreshUtils"

        const val KEY_MIN_REFRESH_RATE = "min_refresh_rate"
        const val KEY_PEAK_REFRESH_RATE = "peak_refresh_rate"

        /*
         * min_refresh_rate is a hard floor: DisplayModeDirector clamps the display to
         * [0, max(min_refresh_rate, peak_refresh_rate)], so both keys have to be written
         * together when overriding the rate of an app.
         *
         * KEY_USER_* holds what the user picked in Settings > Display, KEY_APPLIED_* what this
         * class wrote last. The two are compared to tell the user's changes apart from our own.
         */
        private const val KEY_USER_MIN_RATE = "user_min_refresh_rate"
        private const val KEY_USER_PEAK_RATE = "user_peak_refresh_rate"
        private const val KEY_APPLIED_MIN_RATE = "applied_min_refresh_rate"
        private const val KEY_APPLIED_PEAK_RATE = "applied_peak_refresh_rate"

        // Marks a setting which does not exist, so the framework default applies again.
        private const val UNSET = ""

        // Leftovers of the previous implementation, which cached the values it found once.
        private const val LEGACY_KEY_DEFAULT_MIN_RATE = "refresh_default_min_rate"
        private const val LEGACY_KEY_DEFAULT_PEAK_RATE = "refresh_default_peak_rate"

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
        with(prefs.edit()) {
            if (rate > 0) {
                putInt(PREF_PACKAGE_RATE + packageName, rate)
            } else {
                remove(PREF_PACKAGE_RATE + packageName)
            }
        }.apply()
    }

    /**
     * The old implementation stored the min/peak values it found when it first started and
     * restored those on every foreground app change, which made Settings > Display unable to
     * change the refresh rate. Drop that cache and remove the stranded min_refresh_rate floor
     * (the old service and the QS tile used to write it), so the framework default applies again.
     * peak_refresh_rate is left untouched: that is the value picked in Settings > Display.
     */
    fun migrateLegacyDefaults() {
        if (!prefs.contains(LEGACY_KEY_DEFAULT_MIN_RATE) &&
            !prefs.contains(LEGACY_KEY_DEFAULT_PEAK_RATE)
        ) {
            return
        }

        Log.i(TAG, "Dropping stale refresh rate defaults of the previous implementation")
        deleteRate(KEY_MIN_REFRESH_RATE)
        prefs.edit()
            .remove(LEGACY_KEY_DEFAULT_MIN_RATE)
            .remove(LEGACY_KEY_DEFAULT_PEAK_RATE)
            .remove(KEY_USER_MIN_RATE)
            .remove(KEY_USER_PEAK_RATE)
            .remove(KEY_APPLIED_MIN_RATE)
            .remove(KEY_APPLIED_PEAK_RATE)
            .apply()
    }

    /**
     * Remembers the min/peak pair currently stored in Settings as the user's own preference.
     *
     * Values this class wrote itself (per-app overrides) are ignored, so a user change made in
     * Settings > Display is never overwritten by a stale, cached default again.
     */
    fun rememberUserRates() {
        val min = readRate(KEY_MIN_REFRESH_RATE)
        val peak = readRate(KEY_PEAK_REFRESH_RATE)
        if (min == appliedRate(KEY_APPLIED_MIN_RATE) &&
            peak == appliedRate(KEY_APPLIED_PEAK_RATE)
        ) {
            return
        }

        Log.d(TAG, "Recording user refresh rates: min=$min peak=$peak")
        prefs.edit()
            .putString(KEY_USER_MIN_RATE, min)
            .putString(KEY_USER_PEAK_RATE, peak)
            .putString(KEY_APPLIED_MIN_RATE, min)
            .putString(KEY_APPLIED_PEAK_RATE, peak)
            .apply()
    }

    /** The minimum rate picked in Settings > Display, or null if it was never set. */
    fun getUserMinRate(): Float? =
        prefs.getString(KEY_USER_MIN_RATE, UNSET).orEmpty().toFloatOrNull()
            ?.takeUnless { it.isInfinite() }

    /** The peak rate picked in Settings > Display, or null if it was never set. */
    fun getUserPeakRate(): Float? =
        prefs.getString(KEY_USER_PEAK_RATE, UNSET).orEmpty().toFloatOrNull()
            ?.takeUnless { it.isInfinite() }

    fun applyRate(packageName: String) {
        rememberUserRates()

        val rate = getPackageRate(packageName)
        if (rate > 0) {
            writeRate(rate.toFloat())
        } else {
            restoreUserRate()
        }
    }

    /** Restores the rate the user selected in Settings > Display. */
    fun restoreUserRate() {
        putRate(KEY_MIN_REFRESH_RATE, prefs.getString(KEY_USER_MIN_RATE, UNSET).orEmpty())
        putRate(KEY_PEAK_REFRESH_RATE, prefs.getString(KEY_USER_PEAK_RATE, UNSET).orEmpty())
    }

    fun writeRate(rate: Float) {
        val value = String.format(Locale.US, "%.02f", rate)
        putRate(KEY_MIN_REFRESH_RATE, value)
        putRate(KEY_PEAK_REFRESH_RATE, value)
    }

    private fun readRate(key: String): String =
        Settings.System.getString(contentResolver, key) ?: UNSET

    private fun appliedRate(key: String): String? =
        if (prefs.contains(key)) prefs.getString(key, UNSET) else null

    private fun putRate(key: String, value: String) {
        if (readRate(key) == value) {
            storeApplied(key, value)
            return
        }

        try {
            Settings.System.putString(
                contentResolver,
                key,
                if (value == UNSET) null else value
            )
            storeApplied(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write $key=$value", e)
        }
    }

    private fun deleteRate(key: String) {
        runCatching {
            Settings.System.putString(contentResolver, key, null)
        }.onFailure { Log.e(TAG, "Failed to delete $key", it) }
    }

    private fun storeApplied(key: String, value: String) {
        val prefKey = when (key) {
            KEY_MIN_REFRESH_RATE -> KEY_APPLIED_MIN_RATE
            else -> KEY_APPLIED_PEAK_RATE
        }
        prefs.edit().putString(prefKey, value).apply()
    }
}
