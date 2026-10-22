/*
 * Copyright (C) 2023-2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.battery

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

object BatteryUtils {
    private const val TAG = "XiaomiBatteryUtils"
    const val PREF_CHARGING_CTRL = "charging_control"
    const val PREF_CHARGING_LIMIT = "charging_limit"
    const val PREF_CHARGING_START_LIMIT = "charging_start_limit"
    const val PREF_CHARGING_SUSPENDED = "charging_suspended"
    const val DEFAULT_CHARGING_LIMIT = 80
    const val DEFAULT_CHARGING_START_LIMIT = 0
    private const val NODE_SUSPEND = "/sys/class/power_supply/battery/input_suspend"

    private val ioScope = CoroutineScope(
        Dispatchers.IO.limitedParallelism(1) + SupervisorJob()
    )

    fun setChargingSuspend(suspend: Boolean) {
        val file = File(NODE_SUSPEND)
        if (!file.exists()) return
        val value = if (suspend) "1" else "0"
        try {
            FileOutputStream(file).use { fos ->
                fos.write(value.toByteArray(StandardCharsets.UTF_8))
                fos.flush()
                try {
                    fos.fd.sync()
                } catch (ignored: IOException) {}
            }
            Log.d(TAG, "Successfully wrote $value to $NODE_SUSPEND")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write to $NODE_SUSPEND", e)
        }
    }

    fun setChargingSuspendAsync(suspend: Boolean) {
        ioScope.launch {
            setChargingSuspend(suspend)
        }
    }

    fun isChargingSuspended(): Boolean {
        return try {
            File(NODE_SUSPEND).readText().trim() == "1"
        } catch (e: IOException) {
            false
        }
    }

    fun shouldSuspendCharging(
        isEnabled: Boolean,
        level: Int,
        stopAt: Int,
        startAt: Int,
        wasSuspended: Boolean
    ): Boolean {
        if (!isEnabled) return false
        if (level >= stopAt) return true

        // A start value of 0 keeps the original behavior: resume as soon as
        // the battery drops below the stop threshold.
        val resumeAt = if (startAt == 0) stopAt - 1 else startAt
        return wasSuspended && level > resumeAt
    }
}
