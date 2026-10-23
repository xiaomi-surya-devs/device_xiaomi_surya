/*
 * SPDX-FileCopyrightText: 2023 Paranoid Android
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.display

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.collapsingtoolbar.R as CollapsingToolbarR

class LcdFeaturesPreferenceActivity : CollapsingToolbarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    CollapsingToolbarR.id.content_frame,
                    LcdFeaturesPreferenceFragment()
                )
                .commit()
        }
    }
}
