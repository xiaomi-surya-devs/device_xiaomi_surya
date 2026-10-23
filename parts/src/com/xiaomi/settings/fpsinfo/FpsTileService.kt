/*
 * SPDX-FileCopyrightText: 2020 DerpFest ROM
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.fpsinfo

import android.content.Intent
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.preference.PreferenceManager

class FpsTileService : TileService() {

    private var showing = false

    override fun onStartListening() {
        super.onStartListening()
        showing = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(FpsInfoService.PREF_FPS_INFO, false)
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, FpsInfoService::class.java)

        if (showing) {
            stopService(intent)
            showing = false
            setEnabledPreference(false)
        } else {
            if (!Settings.canDrawOverlays(this)) {
                showing = false
                updateTile()
                return
            }
            startService(intent)
            showing = true
            setEnabledPreference(true)
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (showing) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (showing) "On" else "Off"
        tile.updateTile()
    }

    private fun setEnabledPreference(enabled: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putBoolean(FpsInfoService.PREF_FPS_INFO, enabled)
            .apply()
    }
}
