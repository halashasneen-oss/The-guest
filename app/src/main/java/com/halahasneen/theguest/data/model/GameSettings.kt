package com.halahasneen.theguest.data.model

data class GameSettings(
    val masterVolume: Float = 1f,
    val effectsVolume: Float = 0.75f,
    val ambientVolume: Float = 0.22f,
    val vibrationEnabled: Boolean = true,
    val subtitlesEnabled: Boolean = true
) {
    fun normalized(): GameSettings = copy(
        masterVolume = masterVolume.coerceIn(0f, 1f),
        effectsVolume = effectsVolume.coerceIn(0f, 1f),
        ambientVolume = ambientVolume.coerceIn(0f, 1f)
    )
}
