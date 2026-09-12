package com.halahasneen.theguest.engine

import com.halahasneen.theguest.data.model.Hotspot
import com.halahasneen.theguest.data.model.HotspotType
import com.halahasneen.theguest.data.model.NormalizedPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InteractionSystemTest {
    private val system = InteractionSystem(interactionPadding = 0.04f)

    private val near = Hotspot(
        id = "near",
        type = HotspotType.INSPECT,
        position = NormalizedPoint(0.50f, 0.50f),
        radius = 0.05f,
        label = "Inspect",
        message = "Near"
    )

    private val far = Hotspot(
        id = "far",
        type = HotspotType.INSPECT,
        position = NormalizedPoint(0.80f, 0.80f),
        radius = 0.05f,
        label = "Inspect",
        message = "Far"
    )

    @Test
    fun returnsNearestEligibleHotspot() {
        val result = system.nearest(
            playerPosition = NormalizedPoint(0.46f, 0.50f),
            hotspots = listOf(far, near)
        )
        assertEquals("near", result?.id)
    }

    @Test
    fun ignoresHiddenHotspots() {
        val result = system.nearest(
            playerPosition = NormalizedPoint(0.50f, 0.50f),
            hotspots = listOf(near),
            hiddenHotspotIds = setOf("near")
        )
        assertNull(result)
    }

    @Test
    fun returnsNullOutsideInteractionRange() {
        val result = system.nearest(
            playerPosition = NormalizedPoint(0.10f, 0.10f),
            hotspots = listOf(near)
        )
        assertNull(result)
    }
}
