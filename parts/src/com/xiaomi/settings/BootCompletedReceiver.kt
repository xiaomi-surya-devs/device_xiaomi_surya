/*
 * SPDX-FileCopyrightText: 2018 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.UserHandle
import android.util.Log
import android.view.Display
import android.view.Display.HdrCapabilities
import androidx.preference.PreferenceManager
import com.xiaomi.settings.battery.ChargingLimitService
import com.xiaomi.settings.charge.ChargeUtils
import com.xiaomi.settings.dirac.DiracUtils
import com.xiaomi.settings.display.KcalUtils
import com.xiaomi.settings.refreshrate.RefreshUtils
import com.xiaomi.settings.thermal.ThermalUtils
import com.xiaomi.settings.utils.HapticUtils

/** Everything begins at boot. */
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (DEBUG) Log.d(TAG, "Received boot completed intent: ${intent.action}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> onBootCompleted(context)
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> onLockedBootCompleted(context)
        }

        // Override HDR types
        context.getSystemService(DisplayManager::class.java)?.overrideHdrTypes(
            Display.DEFAULT_DISPLAY,
            intArrayOf(
                HdrCapabilities.HDR_TYPE_DOLBY_VISION,
                HdrCapabilities.HDR_TYPE_HDR10,
                HdrCapabilities.HDR_TYPE_HLG,
                HdrCapabilities.HDR_TYPE_HDR10_PLUS
            )
        )
    }

    private fun onBootCompleted(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        runCatching { DiracUtils.onBootCompleted(context) }
            .onFailure { Log.e(TAG, "Failed to restore Dirac state", it) }

        runCatching { KcalUtils.writeCurrentSettings(prefs) }
            .onFailure { Log.e(TAG, "Failed to restore KCAL state", it) }

        runCatching { HapticUtils.restoreLevel(context) }
            .onFailure { Log.e(TAG, "Failed to restore haptic level", it) }

        runCatching { ChargeUtils(context).enableBypassCharge(false) }
            .onFailure { Log.e(TAG, "Failed to disable bypass charging", it) }

        RefreshUtils.startService(context)
        ThermalUtils.startService(context)
    }

    private fun onLockedBootCompleted(context: Context) {
        // Battery
        context.startServiceAsUser(Intent(context, ChargingLimitService::class.java), UserHandle.CURRENT)
    }
}
