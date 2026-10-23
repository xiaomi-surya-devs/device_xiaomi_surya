/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.thermal

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.xiaomi.settings.gamebar.ForegroundAppDetector

class ThermalService : Service() {

    companion object {
        private const val POLL_INTERVAL_MS = 1_000L
        private const val UNKNOWN_PACKAGE = "Unknown"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var previousPackage: String? = null
    private lateinit var thermalUtils: ThermalUtils

    private val pollRunnable = object : Runnable {
        override fun run() {
            val packageName = ForegroundAppDetector.getForegroundPackageName(this@ThermalService)
            if (packageName != UNKNOWN_PACKAGE && packageName != previousPackage) {
                thermalUtils.applyProfile(packageName)
                previousPackage = packageName
            }
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF ||
                intent.action == Intent.ACTION_SCREEN_ON
            ) {
                previousPackage = null
                thermalUtils.applyDefault()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        thermalUtils = ThermalUtils(this)
        thermalUtils.applyDefault()

        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            Context.RECEIVER_NOT_EXPORTED
        )
        handler.post(pollRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        thermalUtils.applyDefault()
        runCatching { unregisterReceiver(screenReceiver) }
        super.onDestroy()
    }
}
