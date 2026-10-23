/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.refreshrate

import android.view.View
import com.xiaomi.settings.utils.applyBottomInsets
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.xiaomi.settings.R
import java.util.Locale

class RefreshSettingsFragment : SettingsBasePreferenceFragment() {

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
                title = getString(R.string.refresh_title)
                summary = getString(R.string.refresh_summary)
                isSelectable = false
            }
        )

        val category = PreferenceCategory(context).apply {
            title = getString(R.string.refresh_apps_category)
        }
        screen.addPreference(category)

        val utils = RefreshUtils(context)
        launcherApps(context.packageManager).forEach { (packageName, label) ->
            category.addPreference(
                ListPreference(context).apply {
                    key = packageName
                    title = label
                    entries = resources.getStringArray(R.array.refresh_rate_entries)
                    entryValues = resources.getStringArray(R.array.refresh_rate_values)
                    value = utils.getPackageRate(packageName).toString()
                    summary = "%s"
                    isPersistent = false
                    setOnPreferenceChangeListener { _, newValue ->
                        utils.setPackageRate(packageName, newValue.toString().toInt())
                        true
                    }
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        RefreshUtils.startService(requireContext())
    }

    private fun launcherApps(packageManager: PackageManager): List<Pair<String, String>> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName == requireContext().packageName) {
                    null
                } else {
                    packageName to resolveInfo.loadLabel(packageManager).toString()
                }
            }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase(Locale.getDefault()) }
            .toList()
    }
}
