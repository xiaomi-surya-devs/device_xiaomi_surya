/*
 * SPDX-FileCopyrightText: 2019 The OmniROM Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.fpsinfo

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.xiaomi.settings.utils.fileExists
import com.xiaomi.settings.utils.readOneLine

class FpsInfoService : Service() {

    companion object {
        private const val TAG = "FpsInfoService"
        const val PREF_FPS_INFO = "fps_info"
        private const val UPDATE_INTERVAL_MS = 1_000L
        private val MEASURED_FPS_PATHS = listOf(
            "/sys/class/drm/sde-crtc-0/measured_fps",
            "/sys/class/drm/card0/sde-crtc-0/measured_fps"
        )
        private val FPS_PATTERN = Regex("fps:\\s*([0-9]+(?:\\.[0-9]+)?)")
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private var fpsView: TextView? = null
    private var showing = false

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateFps()
            if (showing) {
                handler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    handler.removeCallbacks(updateRunnable)
                    fpsView?.visibility = View.GONE
                }

                Intent.ACTION_SCREEN_ON -> {
                    fpsView?.visibility = View.VISIBLE
                    handler.removeCallbacks(updateRunnable)
                    handler.post(updateRunnable)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission is not granted")
            setEnabledPreference(false)
            stopSelf()
            return
        }

        val wm = getSystemService(WindowManager::class.java)
        if (wm == null) {
            Log.e(TAG, "WindowManager is unavailable")
            setEnabledPreference(false)
            stopSelf()
            return
        }
        windowManager = wm

        if (!addOverlay()) {
            setEnabledPreference(false)
            stopSelf()
            return
        }

        setEnabledPreference(true)
        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            Context.RECEIVER_NOT_EXPORTED
        )
        handler.post(updateRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        showing = false
        handler.removeCallbacksAndMessages(null)
        fpsView?.let { view ->
            runCatching { windowManager.removeView(view) }
            fpsView = null
        }
        runCatching { unregisterReceiver(screenReceiver) }
        setEnabledPreference(false)
        super.onDestroy()
    }

    private fun addOverlay(): Boolean {
        val density = resources.displayMetrics.density
        val view = TextView(this).apply {
            setTextColor(Color.YELLOW)
            textSize = 16f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setShadowLayer(5f, 0f, 0f, Color.BLACK)
            setPadding(
                (9 * density).toInt(),
                (9 * density).toInt(),
                (9 * density).toInt(),
                (9 * density).toInt()
            )
            text = "FPS: --"
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (12 * density).toInt()
            y = (48 * density).toInt()
            setTitle("FPS Info")
        }

        return runCatching {
            windowManager.addView(view, params)
            fpsView = view
            showing = true
            true
        }.onFailure {
            Log.e(TAG, "Failed to add FPS overlay", it)
        }.getOrDefault(false)
    }

    private fun updateFps() {
        val path = MEASURED_FPS_PATHS.firstOrNull { fileExists(it) }
        val raw = path?.let { readOneLine(it) }
        val fps = raw?.let { FPS_PATTERN.find(it)?.groupValues?.getOrNull(1) }
        fpsView?.text = if (fps != null) "$fps FPS" else "FPS: --"
    }

    private fun setEnabledPreference(enabled: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putBoolean(PREF_FPS_INFO, enabled)
            .apply()
    }
}
