/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.refreshrate

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.xiaomi.settings.gamebar.ForegroundAppDetector

class RefreshService : Service() {

    companion object {
        private const val POLL_INTERVAL_MS = 1_000L
        private const val UNKNOWN_PACKAGE = "Unknown"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var previousPackage: String? = null
    private lateinit var refreshUtils: RefreshUtils

    private val pollRunnable = object : Runnable {
        override fun run() {
            val packageName = ForegroundAppDetector.getForegroundPackageName(this@RefreshService)
            if (packageName != UNKNOWN_PACKAGE && packageName != previousPackage) {
                refreshUtils.applyRate(packageName)
                previousPackage = packageName
            }
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                previousPackage = null
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        refreshUtils = RefreshUtils(this)
        refreshUtils.captureDefaultsIfNeeded()
        refreshUtils.resetToDefault()

        registerReceiver(
            screenReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            Context.RECEIVER_NOT_EXPORTED
        )
        handler.post(pollRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(screenReceiver) }
        super.onDestroy()
    }
}
