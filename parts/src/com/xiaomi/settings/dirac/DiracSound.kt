/*
 * SPDX-FileCopyrightText: 2018 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.dirac

import android.media.audiofx.AudioEffect
import java.util.UUID

private val DIRACSOUND_UUID: UUID =
    UUID.fromString("5b8e36a5-144a-4c38-b1d7-0002a5d5c51b")

class DiracSound(priority: Int, audioSession: Int) : AudioEffect(
    AudioEffect.EFFECT_TYPE_NULL,
    DIRACSOUND_UUID,
    priority,
    audioSession
) {

    companion object {
        private const val DIRACSOUND_PARAM_HEADSET_TYPE = 1
        private const val DIRACSOUND_PARAM_EQ_LEVEL = 2
        private const val DIRACSOUND_PARAM_MUSIC = 4
        private const val DIRACSOUND_PARAM_SCENE = 15

        fun isEffectPresent(): Boolean = runCatching {
            AudioEffect.queryEffects()?.any { it.uuid == DIRACSOUND_UUID } == true
        }.getOrDefault(false)
    }

    fun setMusic(enable: Boolean) {
        ensureSuccess(setParameter(DIRACSOUND_PARAM_MUSIC, if (enable) 1 else 0))
    }

    fun getMusic(): Int {
        val value = intArrayOf(0)
        ensureSuccess(getParameter(DIRACSOUND_PARAM_MUSIC, value))
        return value[0]
    }

    fun setHeadsetType(type: Int) {
        ensureSuccess(setParameter(DIRACSOUND_PARAM_HEADSET_TYPE, type))
    }

    fun setLevel(band: Int, level: Float) {
        ensureSuccess(
            setParameter(
                intArrayOf(DIRACSOUND_PARAM_EQ_LEVEL, band),
                level.toString().toByteArray(Charsets.UTF_8)
            )
        )
    }

    fun setScenario(scene: Int) {
        ensureSuccess(setParameter(DIRACSOUND_PARAM_SCENE, scene))
    }

    private fun ensureSuccess(status: Int) {
        if (status != AudioEffect.SUCCESS) {
            throw IllegalStateException("DiracSound operation failed: $status")
        }
    }
}
