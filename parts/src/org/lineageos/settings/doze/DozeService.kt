/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2018 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.settings.doze

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log

class DozeService : Service() {
    private var mAodSensor: AodSensor? = null
    private var mProximitySensor: ProximitySensor? = null
    private var mPickupSensor: PickupSensor? = null

    override fun onCreate() {
        if (DEBUG) {
            Log.d(TAG, "Creating service")
        }
        mAodSensor = AodSensor(this)
        mProximitySensor = ProximitySensor(this)
        mPickupSensor = PickupSensor(this)

        val screenStateFilter = IntentFilter()
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON)
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(mScreenStateReceiver, screenStateFilter)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (DEBUG) {
            Log.d(TAG, "Starting service")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) {
            Log.d(TAG, "Destroying service")
        }
        super.onDestroy()
        unregisterReceiver(mScreenStateReceiver)
        mProximitySensor?.disable()
        mPickupSensor?.disable()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun onDisplayOn() {
        if (DEBUG) {
            Log.d(TAG, "Display on")
        }
        if (DozeUtils.isPickUpEnabled(this)) {
            mPickupSensor?.disable()
        }
        if (DozeUtils.isHandwaveGestureEnabled(this) || DozeUtils.isPocketGestureEnabled(this)) {
            mProximitySensor?.disable()
        }
        if (DozeUtils.isDozeAutoBrightnessEnabled(this)) {
            mAodSensor?.disable()
        }
    }

    private fun onDisplayOff() {
        if (DEBUG) {
            Log.d(TAG, "Display off")
        }
        if (DozeUtils.isPickUpEnabled(this)) {
            mPickupSensor?.enable()
        }
        if (DozeUtils.isHandwaveGestureEnabled(this) || DozeUtils.isPocketGestureEnabled(this)) {
            mProximitySensor?.enable()
        }
        if (DozeUtils.isDozeAutoBrightnessEnabled(this)) {
            mAodSensor?.enable()
        }
    }

    private val mScreenStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_SCREEN_ON == intent.action) {
                onDisplayOn()
            } else if (Intent.ACTION_SCREEN_OFF == intent.action) {
                onDisplayOff()
            }
        }
    }

    companion object {
        private const val TAG = "DozeService"
        private const val DEBUG = false
    }
}
