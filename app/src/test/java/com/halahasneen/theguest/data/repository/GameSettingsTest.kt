package com.halahasneen.theguest.data.repository

import com.halahasneen.theguest.data.model.GameSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class GameSettingsTest {
    @Test
    fun normalizationClampsVolumes() {
        val settings = GameSettings(masterVolume = 2f, effectsVolume = -1f, ambientVolume = 0.4f).normalized()
        assertEquals(1f, settings.masterVolume)
        assertEquals(0f, settings.effectsVolume)
        assertEquals(0.4f, settings.ambientVolume)
    }
}
