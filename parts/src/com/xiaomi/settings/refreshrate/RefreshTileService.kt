/*
 * SPDX-FileCopyrightText: 2021 WaveOS
 * SPDX-FileCopyrightText: 2021 Chaldeaprjkt
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.refreshrate

import android.hardware.display.DisplayManager
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.Display
import java.util.Locale

class RefreshTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        cycleRefreshRate()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val min = Settings.System.getFloat(
            contentResolver,
            RefreshUtils.KEY_MIN_REFRESH_RATE,
            60f
        )
        val peak = Settings.System.getFloat(
            contentResolver,
            RefreshUtils.KEY_PEAK_REFRESH_RATE,
            60f
        )
        val text = if (min == peak) formatRate(min) else "${formatRate(min)} - ${formatRate(peak)}"

        tile.contentDescription = text
        tile.subtitle = text
        tile.state = if (min == peak) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    private fun cycleRefreshRate() {
        val currentDisplay = display
            ?: getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
            ?: return
        val mode = currentDisplay.mode ?: return
        val rates = currentDisplay.supportedModes
            ?.filter {
                it.physicalWidth == mode.physicalWidth &&
                    it.physicalHeight == mode.physicalHeight
            }
            ?.map { it.refreshRate }
            ?.distinct()
            ?.sorted()
            ?: return

        if (rates.isEmpty()) return

        val current = Settings.System.getFloat(
            contentResolver,
            RefreshUtils.KEY_PEAK_REFRESH_RATE,
            rates.first()
        )
        val next = rates.firstOrNull { it > current } ?: rates.first()
        Settings.System.putString(
            contentResolver,
            RefreshUtils.KEY_MIN_REFRESH_RATE,
            next.toString()
        )
        Settings.System.putString(
            contentResolver,
            RefreshUtils.KEY_PEAK_REFRESH_RATE,
            next.toString()
        )
    }

    private fun formatRate(rate: Float): String =
        String.format(Locale.US, "%.02f Hz", rate).replace(Regex("[.,]00"), "")
}
