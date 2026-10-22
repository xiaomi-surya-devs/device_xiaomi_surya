/*
 * Copyright (C) 2021 The LineageOS Project
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

class AodSensor(context: Context) : SensorEventListener {
    private val mSensorManager: SensorManager = context.getSystemService(SensorManager::class.java)
    private val mSensor: Sensor? = DozeUtils.getSensor(mSensorManager, "xiaomi.sensor.aod")
    private val mExecutorService: ExecutorService = Executors.newSingleThreadExecutor()

    private fun submit(runnable: Runnable): Future<*> {
        return mExecutorService.submit(runnable)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (DEBUG) {
            Log.d(TAG, "Got sensor event: " + event.values[0])
        }

        if (event.values[0] == 3f || event.values[0] == 5f) {
            DozeUtils.setDozeMode(DozeUtils.DOZE_MODE_LBM)
        } else if (event.values[0] == 4f) {
            DozeUtils.setDozeMode(DozeUtils.DOZE_MODE_HBM)
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
            mSensor?.let { mSensorManager.registerListener(this@AodSensor, it, SensorManager.SENSOR_DELAY_NORMAL) }
        })
    }

    fun disable() {
        if (DEBUG) {
            Log.d(TAG, "Disabling")
        }
        submit(Runnable {
            mSensor?.let { mSensorManager.unregisterListener(this@AodSensor, it) }
        })
    }

    companion object {
        private const val DEBUG = false
        private const val TAG = "AodSensor"
    }
}
