/*
 * SPDX-FileCopyrightText: 2023 Paranoid Android
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.display

import android.os.SystemProperties
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.xiaomi.settings.R

class CabcTileService : TileService() {

    private lateinit var cabcModes: Array<String>
    private lateinit var cabcValues: Array<String>

    override fun onCreate() {
        super.onCreate()
        cabcModes = resources.getStringArray(R.array.lcd_cabc_modes)
        cabcValues = resources.getStringArray(R.array.lcd_cabc_values)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val current = currentIndex()
        val next = if (current >= cabcValues.lastIndex) 0 else current + 1
        SystemProperties.set(LcdFeaturesPreferenceFragment.CABC_PROP, cabcValues[next])
        updateTile()
    }

    private fun currentIndex(): Int {
        val current = SystemProperties.get(LcdFeaturesPreferenceFragment.CABC_PROP, "0")
        return cabcValues.indexOf(current).coerceAtLeast(0)
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val current = currentIndex()
        tile.state = if (current > 0) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = cabcModes[current]
        tile.contentDescription = cabcModes[current]
        tile.updateTile()
    }
}
