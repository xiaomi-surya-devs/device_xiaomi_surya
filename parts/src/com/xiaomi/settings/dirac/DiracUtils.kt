/*
 * SPDX-FileCopyrightText: 2018 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.dirac

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager

object DiracUtils {

    private const val TAG = "DiracUtils"
    const val PREF_ENABLE = "dirac_enable"
    const val PREF_HEADSET = "dirac_headset_pref"
    const val PREF_PRESET = "dirac_preset_pref"
    const val PREF_SCENE = "scenario_selection"

    private var diracSound: DiracSound? = null
    private var initialized = false
    private var supported = false

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true

        if (!DiracSound.isEffectPresent()) {
            Log.w(TAG, "DiracSound effect is not available")
            return
        }

        diracSound = runCatching { DiracSound(0, 0) }
            .onFailure { Log.e(TAG, "Failed to initialize DiracSound", it) }
            .getOrNull()
        supported = diracSound != null
    }

    fun isSupported(): Boolean = supported

    fun isDiracEnabled(): Boolean =
        runCatching { diracSound?.getMusic() == 1 }.getOrDefault(false)

    fun onBootCompleted(context: Context) {
        initialize(context)
        if (!supported) return

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val scene = prefs.getString(PREF_SCENE, "4") ?: "4"
        runCatching { setScenario(scene.toInt()) }
        runCatching { setMusic(prefs.getBoolean(PREF_ENABLE, false)) }
    }

    fun setMusic(enable: Boolean): Boolean {
        val sound = diracSound ?: return false
        return runCatching {
            sound.setMusic(enable)
            true
        }.onFailure { Log.e(TAG, "Failed to set Dirac music mode", it) }.getOrDefault(false)
    }

    fun setHeadsetType(type: Int): Boolean {
        val sound = diracSound ?: return false
        return runCatching {
            sound.setHeadsetType(type)
            true
        }.onFailure { Log.e(TAG, "Failed to set Dirac headset type", it) }.getOrDefault(false)
    }

    fun setLevel(preset: String): Boolean {
        val sound = diracSound ?: return false
        return runCatching {
            preset.split(",").forEachIndexed { band, value ->
                sound.setLevel(band, value.trim().toFloat())
            }
            true
        }.onFailure { Log.e(TAG, "Failed to set Dirac preset", it) }.getOrDefault(false)
    }

    fun setScenario(scene: Int): Boolean {
        val sound = diracSound ?: return false
        return runCatching {
            sound.setScenario(scene)
            true
        }.onFailure { Log.e(TAG, "Failed to set Dirac scenario", it) }.getOrDefault(false)
    }
}
