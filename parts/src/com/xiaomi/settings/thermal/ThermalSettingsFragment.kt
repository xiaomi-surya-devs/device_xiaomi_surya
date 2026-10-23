/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.thermal

import android.view.View
import com.xiaomi.settings.utils.applyBottomInsets
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.xiaomi.settings.R
import java.util.Locale

class ThermalSettingsFragment : SettingsBasePreferenceFragment() {

    private data class AppEntry(
        val packageName: String,
        val label: String,
        val icon: Drawable?
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBottomInsets()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        screen.addPreference(
            Preference(context).apply {
                title = getString(R.string.thermal_title)
                summary = getString(R.string.thermal_summary)
                isSelectable = false
            }
        )

        val category = PreferenceCategory(context).apply {
            title = getString(R.string.thermal_apps_category)
        }
        screen.addPreference(category)

        val utils = ThermalUtils(context)
        launcherApps(context.packageManager).forEach { app ->
            category.addPreference(
                ListPreference(context).apply {
                    key = app.packageName
                    title = app.label
                    icon = app.icon
                    entries = resources.getStringArray(R.array.thermal_profile_entries)
                    entryValues = resources.getStringArray(R.array.thermal_profile_values)
                    value = utils.getProfile(app.packageName).toString()
                    summary = "%s"
                    isPersistent = false
                    setOnPreferenceChangeListener { _, newValue ->
                        utils.setProfile(app.packageName, newValue.toString().toInt())
                        true
                    }
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        ThermalUtils.startService(requireContext())
    }

    private fun launcherApps(packageManager: PackageManager): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName == requireContext().packageName) {
                    null
                } else {
                    AppEntry(
                        packageName,
                        resolveInfo.loadLabel(packageManager).toString(),
                        resolveInfo.loadIcon(packageManager)
                    )
                }
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .toList()
    }
}
