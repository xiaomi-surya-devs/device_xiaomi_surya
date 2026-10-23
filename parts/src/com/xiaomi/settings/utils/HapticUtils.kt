/*
 * SPDX-FileCopyrightText: 2021 chaldeaprjkt
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.utils

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.preference.PreferenceManager
import kotlin.math.roundToInt

object HapticUtils {

    const val PREF_LEVEL = "haptic_level_pref"
    const val PATH_LEVEL = "/sys/devices/platform/soc/884000.i2c/i2c-3/3-005a/ulevel"

    private const val MIN_LEVEL = 1
    private const val MAX_LEVEL = 128

    fun isSupported(): Boolean = fileExists(PATH_LEVEL)

    fun applyLevel(context: Context, value: Int, test: Boolean) {
        if (!isSupported()) return

        val level = value / 100.0 * (MAX_LEVEL - MIN_LEVEL) + MIN_LEVEL
        writeLine(PATH_LEVEL, level.roundToInt().toString())

        if (test) {
            val vibrator = context.getSystemService(Vibrator::class.java) ?: return
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        }
    }

    fun restoreLevel(context: Context) {
        val level = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(PREF_LEVEL, 80)
        applyLevel(context, level, false)
    }
}
