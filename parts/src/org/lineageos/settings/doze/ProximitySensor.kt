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

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class ProximitySensor(private val mContext: Context) : SensorEventListener {
    private val mSensorManager: SensorManager = mContext.getSystemService(SensorManager::class.java)
    private val mSensor: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY, false)
    private val mExecutorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var mSawNear = false
    private var mInPocketTime: Long = 0

    private fun submit(runnable: Runnable): Future<*> {
        return mExecutorService.submit(runnable)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val isNear = event.values[0] < mSensor!!.maximumRange
        if (mSawNear && !isNear) {
            if (shouldPulse(event.timestamp)) {
                DozeUtils.wakeOrLaunchDozePulse(mContext)
            }
        } else {
            mInPocketTime = event.timestamp
        }
        mSawNear = isNear
    }

    private fun shouldPulse(timestamp: Long): Boolean {
        val delta = timestamp - mInPocketTime

        return if (DozeUtils.isHandwaveGestureEnabled(mContext)
            && DozeUtils.isPocketGestureEnabled(mContext)
        ) {
            true
        } else if (DozeUtils.isHandwaveGestureEnabled(mContext)) {
            delta < HANDWAVE_MAX_DELTA_NS
        } else if (DozeUtils.isPocketGestureEnabled(mContext)) {
            delta >= POCKET_MIN_DELTA_NS
        } else {
            false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        /* Empty */
    }

    fun enable() {
        if (DEBUG) {
            Log.d(TAG, "Enabling")
        }
        submit(Runnable {
            mSensor?.let { mSensorManager.registerListener(this@ProximitySensor, it, SensorManager.SENSOR_DELAY_NORMAL) }
        })
    }

    fun disable() {
        if (DEBUG) {
            Log.d(TAG, "Disabling")
        }
        submit(Runnable {
            mSensor?.let { mSensorManager.unregisterListener(this@ProximitySensor, it) }
        })
    }

    companion object {
        private const val DEBUG = false
        private const val TAG = "ProximitySensor"
        // Maximum time for the hand to cover the sensor: 1s
        private const val HANDWAVE_MAX_DELTA_NS = 1000L * 1000L * 1000L
        // Minimum time until the device is considered to have been in the pocket: 2s
        private const val POCKET_MIN_DELTA_NS = 2000L * 1000L * 1000L
    }
}
