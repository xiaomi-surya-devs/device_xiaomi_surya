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

class HbmTileService : TileService() {

    private lateinit var hbmModes: Array<String>

    override fun onCreate() {
        super.onCreate()
        hbmModes = resources.getStringArray(R.array.lcd_hbm_modes)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val current = SystemProperties.getInt(
            LcdFeaturesPreferenceFragment.HBM_PROP,
            0
        ).coerceIn(0, hbmModes.lastIndex)
        val next = if (current >= hbmModes.lastIndex) 0 else current + 1
        SystemProperties.set(LcdFeaturesPreferenceFragment.HBM_PROP, next.toString())
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val current = SystemProperties.getInt(
            LcdFeaturesPreferenceFragment.HBM_PROP,
            0
        ).coerceIn(0, hbmModes.lastIndex)
        tile.state = if (current > 0) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = hbmModes[current]
        tile.contentDescription = hbmModes[current]
        tile.updateTile()
    }
}
