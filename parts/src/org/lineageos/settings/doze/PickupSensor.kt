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
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class PickupSensor(private val mContext: Context) : SensorEventListener {
    private val mSensorManager: SensorManager = mContext.getSystemService(SensorManager::class.java)
    private val mSensor: Sensor? = DozeUtils.getSensor(mSensorManager, "xiaomi.sensor.pickup")
    private val mExecutorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var mEntryTimestamp: Long = 0

    private fun submit(runnable: Runnable): Future<*> {
        return mExecutorService.submit(runnable)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (DEBUG) {
            Log.d(TAG, "Got sensor event: " + event.values[0])
        }

        val delta = SystemClock.elapsedRealtime() - mEntryTimestamp
        if (delta < MIN_PULSE_INTERVAL_MS) {
            return
        }

        mEntryTimestamp = SystemClock.elapsedRealtime()

        if (event.values[0] == 1f) {
            DozeUtils.wakeOrLaunchDozePulse(mContext)
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
            mSensor?.let { mSensorManager.registerListener(this@PickupSensor, it, SensorManager.SENSOR_DELAY_NORMAL) }
            mEntryTimestamp = SystemClock.elapsedRealtime()
        })
    }

    fun disable() {
        if (DEBUG) {
            Log.d(TAG, "Disabling")
        }
        submit(Runnable {
            mSensor?.let { mSensorManager.unregisterListener(this@PickupSensor, it) }
        })
    }

    companion object {
        private const val DEBUG = false
        private const val TAG = "PickupSensor"
        private const val MIN_PULSE_INTERVAL_MS = 2500L
    }
}
