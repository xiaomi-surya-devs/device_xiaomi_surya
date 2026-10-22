/*
 * Copyright (C) 2023-2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.battery

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import androidx.preference.PreferenceManager

class ChargingLimitService : Service() {
    private var mReceiverRegistered = false

    private val mBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_BATTERY_CHANGED == intent.action) {
                updateChargingState(context, intent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appContext = applicationContext
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = appContext.registerReceiver(null, filter)
        updateChargingState(appContext, batteryStatus)

        if (!mReceiverRegistered) {
            appContext.registerReceiver(mBatteryReceiver, filter)
            mReceiverRegistered = true
        }
        return START_STICKY
    }

    private fun updateChargingState(context: Context, intent: Intent?) {
        intent ?: return
        val storageContext = context.createDeviceProtectedStorageContext()
        val prefs = PreferenceManager.getDefaultSharedPreferences(storageContext)
        val isEnabled = prefs.getBoolean(BatteryUtils.PREF_CHARGING_CTRL, false)

        if (!isEnabled) {
            if (prefs.getBoolean(BatteryUtils.PREF_CHARGING_SUSPENDED, false)) {
                prefs.edit().putBoolean(BatteryUtils.PREF_CHARGING_SUSPENDED, false).apply()
            }
            BatteryUtils.setChargingSuspendAsync(false)
            return
        }

        val stopAt = prefs.getInt(
            BatteryUtils.PREF_CHARGING_LIMIT,
            BatteryUtils.DEFAULT_CHARGING_LIMIT
        )
        val startAt = prefs.getInt(
            BatteryUtils.PREF_CHARGING_START_LIMIT,
            BatteryUtils.DEFAULT_CHARGING_START_LIMIT
        )
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        if (level >= 0) {
            val hasSuspendedState = prefs.contains(BatteryUtils.PREF_CHARGING_SUSPENDED)
            val wasSuspended = if (hasSuspendedState) {
                prefs.getBoolean(BatteryUtils.PREF_CHARGING_SUSPENDED, false)
            } else {
                BatteryUtils.isChargingSuspended()
            }
            val shouldSuspend = BatteryUtils.shouldSuspendCharging(
                isEnabled,
                level,
                stopAt,
                startAt,
                wasSuspended
            )
            if (!hasSuspendedState || shouldSuspend != wasSuspended) {
                prefs.edit().putBoolean(BatteryUtils.PREF_CHARGING_SUSPENDED, shouldSuspend).apply()
            }
            BatteryUtils.setChargingSuspendAsync(shouldSuspend)
        }
    }

    override fun onDestroy() {
        if (mReceiverRegistered) {
            applicationContext.unregisterReceiver(mBatteryReceiver)
            mReceiverRegistered = false
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
