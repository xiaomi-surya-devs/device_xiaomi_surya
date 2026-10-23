/*
 * SPDX-FileCopyrightText: 2023 Paranoid Android
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.speaker

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.preference.Preference
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.xiaomi.settings.R

class ClearSpeakerFragment : SettingsBasePreferenceFragment(),
    Preference.OnPreferenceChangeListener {

    companion object {
        private const val TAG = "ClearSpeakerFragment"
        private const val PREF_CLEAR_SPEAKER = "clear_speaker_pref"
        private const val PLAY_DURATION_MS = 30_000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var clearSpeakerPref: MainSwitchPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.clear_speaker_settings, rootKey)
        clearSpeakerPref = findPreference<MainSwitchPreference>(PREF_CLEAR_SPEAKER)?.apply {
            setOnPreferenceChangeListener(this@ClearSpeakerFragment)
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val enabled = newValue as Boolean
        if (enabled) {
            if (!startPlaying()) {
                return false
            }
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({ stopPlaying() }, PLAY_DURATION_MS)
        } else {
            stopPlaying()
        }
        return true
    }

    private fun startPlaying(): Boolean {
        stopPlaying()
        requireActivity().volumeControlStream = AudioManager.STREAM_MUSIC

        return try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
            }
            resources.openRawResourceFd(R.raw.clear_speaker_sound).use { afd ->
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                player.setVolume(1.0f, 1.0f)
                player.prepare()
                player.start()
            }
            mediaPlayer = player
            clearSpeakerPref?.isEnabled = false
            clearSpeakerPref?.isChecked = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play speaker clean sound", e)
            releasePlayer()
            clearSpeakerPref?.isChecked = false
            false
        }
    }

    private fun stopPlaying() {
        handler.removeCallbacksAndMessages(null)
        releasePlayer()
        clearSpeakerPref?.isEnabled = true
        clearSpeakerPref?.isChecked = false
    }

    private fun releasePlayer() {
        mediaPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) player.stop()
            }.onFailure { Log.e(TAG, "Failed to stop media player", it) }
            runCatching { player.release() }
        }
        mediaPlayer = null
    }

    override fun onStop() {
        super.onStop()
        stopPlaying()
    }

    override fun onDestroyView() {
        stopPlaying()
        clearSpeakerPref?.setOnPreferenceChangeListener(null)
        clearSpeakerPref = null
        super.onDestroyView()
    }
}
