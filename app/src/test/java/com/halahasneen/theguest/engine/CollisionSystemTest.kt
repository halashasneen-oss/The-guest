package com.halahasneen.theguest.engine

import com.halahasneen.theguest.data.model.NormalizedPoint
import com.halahasneen.theguest.data.model.NormalizedRect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollisionSystemTest {
    private val system = CollisionSystem(
        walkableBounds = NormalizedRect(0.1f, 0.1f, 0.9f, 0.9f),
        obstacles = listOf(NormalizedRect(0.4f, 0.4f, 0.6f, 0.6f))
    )

    @Test
    fun acceptsWalkablePoint() {
        assertTrue(system.isValid(NormalizedPoint(0.2f, 0.2f), 0.02f))
    }

    @Test
    fun rejectsObstacleOverlap() {
        assertFalse(system.isValid(NormalizedPoint(0.5f, 0.5f), 0.02f))
    }

    @Test
    fun resolvesMovementBySlidingOnFreeAxis() {
        val current = NormalizedPoint(0.35f, 0.5f)
        val desired = NormalizedPoint(0.45f, 0.39f)
        assertEquals(NormalizedPoint(0.35f, 0.39f), system.resolve(current, desired, 0.02f))
    }
}
