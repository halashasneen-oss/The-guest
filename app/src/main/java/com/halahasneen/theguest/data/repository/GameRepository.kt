package com.halahasneen.theguest.data.repository

import android.content.Context
import com.halahasneen.theguest.data.model.GameSaveData

class GameRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(data: GameSaveData) {
        preferences.edit().putString(KEY_SAVE_JSON, GameSaveCodec.encode(data)).apply()
    }

    fun load(): GameSaveData? {
        val raw = preferences.getString(KEY_SAVE_JSON, null) ?: return null
        return GameSaveCodec.decode(raw)
    }

    fun hasValidSave(): Boolean = load() != null

    fun clear() {
        preferences.edit().remove(KEY_SAVE_JSON).apply()
    }

    private companion object {
        const val PREFS_NAME = "the_guest_game"
        const val KEY_SAVE_JSON = "game_save_json"
    }
}
