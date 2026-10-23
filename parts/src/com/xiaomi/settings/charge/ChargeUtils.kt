/*
 * SPDX-FileCopyrightText: 2023 Paranoid Android
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.charge

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.xiaomi.settings.utils.fileExists
import com.xiaomi.settings.utils.readOneLine
import com.xiaomi.settings.utils.writeLine

class ChargeUtils(context: Context) {

    companion object {
        private const val TAG = "ChargeUtils"
        const val BYPASS_CHARGE_NODE =
            "/sys/class/power_supply/battery/charging_enabled"
        const val PREF_BYPASS_CHARGE = "bypass_charge"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun isBypassChargeEnabled(): Boolean {
        return readOneLine(BYPASS_CHARGE_NODE)?.trim() == "0"
    }

    fun enableBypassCharge(enable: Boolean) {
        if (!isBypassChargeSupported()) return
        if (!writeLine(BYPASS_CHARGE_NODE, if (enable) "0" else "1")) {
            Log.e(TAG, "Failed to write $BYPASS_CHARGE_NODE")
            return
        }
        prefs.edit().putBoolean(PREF_BYPASS_CHARGE, enable).apply()
    }

    fun isBypassChargeSupported(): Boolean = fileExists(BYPASS_CHARGE_NODE)
}
