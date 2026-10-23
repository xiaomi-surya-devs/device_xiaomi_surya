/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.utils

import android.view.WindowInsets
import androidx.preference.PreferenceFragmentCompat

/**
 * Keep the last preference items clear of the gesture/navigation bar
 * instead of drawing underneath it (edge-to-edge).
 */
fun PreferenceFragmentCompat.applyBottomInsets() {
    val list = listView ?: return
    val basePadding = list.paddingBottom
    list.clipToPadding = false
    list.setOnApplyWindowInsetsListener { v, insets ->
        val bars = insets.getInsets(
            WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout()
        )
        v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, basePadding + bars.bottom)
        insets
    }
    list.requestApplyInsets()
}
