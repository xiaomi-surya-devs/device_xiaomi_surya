/*
 * SPDX-FileCopyrightText: 2023 Paranoid Android
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.charge

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class BypassChargingTileService : TileService() {

    private lateinit var chargeUtils: ChargeUtils

    override fun onCreate() {
        super.onCreate()
        chargeUtils = ChargeUtils(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        chargeUtils.enableBypassCharge(!chargeUtils.isBypassChargeEnabled())
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val enabled = chargeUtils.isBypassChargeEnabled()
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (enabled) "On" else "Off"
        tile.updateTile()
    }
}
