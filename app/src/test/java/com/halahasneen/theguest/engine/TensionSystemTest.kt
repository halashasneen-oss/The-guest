package com.halahasneen.theguest.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TensionSystemTest {
    @Test
    fun stageThresholdsAreDeterministic() {
        val system = TensionSystem()
        assertEquals(TensionStage.CALM, system.stage)
        system.addStimulus(25f)
        assertEquals(TensionStage.UNEASY, system.stage)
        system.addStimulus(25f)
        assertEquals(TensionStage.DISTURBED, system.stage)
        system.addStimulus(25f)
        assertEquals(TensionStage.TERRIFIED, system.stage)
    }

    @Test
    fun safeLightReducesTension() {
        val system = TensionSystem(40f)
        system.update(deltaSeconds = 1f, inDarkness = false, inSafeLight = true)
        assertTrue(system.level < 40f)
    }

    @Test
    fun valuesStayClamped() {
        val system = TensionSystem(98f)
        system.addStimulus(20f)
        assertEquals(100f, system.level)
        system.calm(150f)
        assertEquals(0f, system.level)
    }
}
