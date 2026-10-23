/*
 * SPDX-FileCopyrightText: 2020 ArrowOS
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.display

import android.content.SharedPreferences
import com.xiaomi.settings.utils.fileExists
import com.xiaomi.settings.utils.readOneLine
import com.xiaomi.settings.utils.writeLine

object KcalUtils {

    const val KCAL_ENABLE_NODE = "/sys/devices/platform/kcal_ctrl.0/kcal_enable"
    const val KCAL_RGB_NODE = "/sys/devices/platform/kcal_ctrl.0/kcal"
    const val KCAL_SATURATION_NODE = "/sys/devices/platform/kcal_ctrl.0/kcal_sat"
    const val KCAL_CONTRAST_NODE = "/sys/devices/platform/kcal_ctrl.0/kcal_cont"
    const val KCAL_HUE_NODE = "/sys/devices/platform/kcal_ctrl.0/kcal_hue"
    const val KCAL_BRIGHTNESS_NODE = "/sys/devices/platform/kcal_ctrl.0/kcal_val"

    private const val DEFAULT_RGB = 256
    private const val DEFAULT_SATURATION = 255
    private const val DEFAULT_CONTRAST = 255
    private const val DEFAULT_HUE = 0
    private const val DEFAULT_BRIGHTNESS = 255

    fun isKcalSupported(): Boolean = fileExists(KCAL_ENABLE_NODE)

    fun setEnabled(enabled: Boolean) {
        writeLine(KCAL_ENABLE_NODE, if (enabled) "1" else "0")
    }

    fun writeRgb(red: Int, green: Int, blue: Int) {
        writeLine(KCAL_RGB_NODE, "$red $green $blue")
    }

    fun writeNode(node: String, value: Int) {
        writeLine(node, value.toString())
    }

    fun writeRgbComponent(component: Int, value: Int) {
        val current = readRgb()
        val red = if (component == 1) value else current[0]
        val green = if (component == 2) value else current[1]
        val blue = if (component == 3) value else current[2]
        writeRgb(red, green, blue)
    }

    fun writeCurrentSettings(prefs: SharedPreferences) {
        setEnabled(prefs.getBoolean("kcal_enable", false))

        writeRgb(
            prefs.getInt("red_slider", DEFAULT_RGB),
            prefs.getInt("green_slider", DEFAULT_RGB),
            prefs.getInt("blue_slider", DEFAULT_RGB)
        )

        writeLine(
            KCAL_SATURATION_NODE,
            prefs.getInt("saturation_slider", DEFAULT_SATURATION).toString()
        )
        writeLine(
            KCAL_CONTRAST_NODE,
            prefs.getInt("contrast_slider", DEFAULT_CONTRAST).toString()
        )
        writeLine(KCAL_HUE_NODE, prefs.getInt("hue_slider", DEFAULT_HUE).toString())
        writeLine(
            KCAL_BRIGHTNESS_NODE,
            prefs.getInt("brightness_slider", DEFAULT_BRIGHTNESS).toString()
        )
    }

    private fun readRgb(): IntArray {
        val values = readOneLine(KCAL_RGB_NODE)
            ?.trim()
            ?.split(Regex("\\s+"))
            ?: emptyList()

        return intArrayOf(
            values.getOrNull(0)?.toIntOrNull() ?: DEFAULT_RGB,
            values.getOrNull(1)?.toIntOrNull() ?: DEFAULT_RGB,
            values.getOrNull(2)?.toIntOrNull() ?: DEFAULT_RGB
        )
    }
}
