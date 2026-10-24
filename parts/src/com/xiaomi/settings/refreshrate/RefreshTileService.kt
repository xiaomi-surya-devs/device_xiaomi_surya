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
import com.xiaomi.settings.R
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

        val utils = RefreshUtils(this)
        utils.rememberUserRates()
        val min = utils.getUserMinRate()
        val peak = utils.getUserPeakRate()

        val text = when {
            min != null && min == peak -> formatRate(min)
            min != null && peak != null -> "${formatRate(min)} - ${formatRate(peak)}"
            peak != null -> formatRate(peak)
            min != null -> formatRate(min)
            else -> getString(R.string.refresh_rate_auto)
        }

        tile.contentDescription = text
        tile.subtitle = text
        tile.state = if (min != null && min == peak) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
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

        val current = RefreshUtils(this).getUserPeakRate() ?: mode.refreshRate
        val next = rates.firstOrNull { it > current } ?: rates.first()
        val value = String.format(Locale.US, "%.02f", next)
        // Only peak_refresh_rate is written: min_refresh_rate is a hard floor, and touching it
        // from here would stop a later "Maximum refresh rate" change in Settings > Display from
        // taking effect. Written directly (not through RefreshUtils) so that the service treats
        // it as the new user preference instead of one of its own per-app overrides.
        Settings.System.putString(
            contentResolver,
            RefreshUtils.KEY_PEAK_REFRESH_RATE,
            value
        )
    }

    private fun formatRate(rate: Float): String =
        String.format(Locale.US, "%.02f Hz", rate).replace(Regex("[.,]00"), "")
}
