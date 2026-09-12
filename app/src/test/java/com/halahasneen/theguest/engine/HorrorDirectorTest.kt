package com.halahasneen.theguest.engine

import com.halahasneen.theguest.data.model.HorrorAction
import com.halahasneen.theguest.data.model.HorrorContext
import com.halahasneen.theguest.data.model.HorrorEvent
import com.halahasneen.theguest.data.model.HorrorEventCategory
import com.halahasneen.theguest.data.model.RoomId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HorrorDirectorTest {
    private val subtle = HorrorEvent(
        id = "subtle",
        roomId = RoomId.LIVING_ROOM,
        minVisitCount = 1,
        minMemories = 0,
        minTension = 10f,
        weight = 1f,
        cooldownSeconds = 3f,
        oneShot = true,
        category = HorrorEventCategory.SUBTLE,
        action = HorrorAction.LIVING_PICTURE_TILT
    )

    @Test
    fun eligibilityRespectsRoomAndTension() {
        val director = HorrorDirector { 0f }
        val lowTension = HorrorContext(RoomId.LIVING_ROOM, 1, 0, 5f)
        assertTrue(director.eligibleEvents(lowTension, listOf(subtle)).isEmpty())

        val ready = HorrorContext(RoomId.LIVING_ROOM, 1, 0, 15f)
        assertEquals(listOf(subtle), director.eligibleEvents(ready, listOf(subtle)))
    }

    @Test
    fun oneShotEventDoesNotBecomeEligibleAgain() {
        val director = HorrorDirector { 0f }
        val context = HorrorContext(RoomId.LIVING_ROOM, 1, 0, 20f)

        var fired: HorrorEvent? = null
        repeat(4) {
            fired = fired ?: director.update(1f, context, listOf(subtle), 1f)
        }
        assertEquals("subtle", fired?.id)

        repeat(20) { director.update(1f, context, listOf(subtle), 1f) }
        assertTrue(director.eligibleEvents(context, listOf(subtle)).isEmpty())
    }
}
