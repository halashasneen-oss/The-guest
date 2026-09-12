package com.halahasneen.theguest.data.repository

import android.content.Context
import com.halahasneen.theguest.data.model.GameSettings

class GameSettingsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): GameSettings = GameSettings(
        masterVolume = preferences.getFloat(KEY_MASTER, 1f),
        effectsVolume = preferences.getFloat(KEY_EFFECTS, 0.75f),
        ambientVolume = preferences.getFloat(KEY_AMBIENT, 0.22f),
        vibrationEnabled = preferences.getBoolean(KEY_VIBRATION, true),
        subtitlesEnabled = preferences.getBoolean(KEY_SUBTITLES, true)
    ).normalized()

    fun save(settings: GameSettings) {
        val normalized = settings.normalized()
        preferences.edit()
            .putFloat(KEY_MASTER, normalized.masterVolume)
            .putFloat(KEY_EFFECTS, normalized.effectsVolume)
            .putFloat(KEY_AMBIENT, normalized.ambientVolume)
            .putBoolean(KEY_VIBRATION, normalized.vibrationEnabled)
            .putBoolean(KEY_SUBTITLES, normalized.subtitlesEnabled)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "the_guest_settings"
        const val KEY_MASTER = "master_volume"
        const val KEY_EFFECTS = "effects_volume"
        const val KEY_AMBIENT = "ambient_volume"
        const val KEY_VIBRATION = "vibration_enabled"
        const val KEY_SUBTITLES = "subtitles_enabled"
    }
}
