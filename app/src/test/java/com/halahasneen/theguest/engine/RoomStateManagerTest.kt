package com.halahasneen.theguest.engine

import com.halahasneen.theguest.data.model.RoomId
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoomStateManagerTest {
    @Test
    fun physicalStatePersistsAcrossRepeatedAccess() {
        val manager = RoomStateManager()
        manager.markPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_PICTURE_TILTED)
        assertTrue(manager.hasPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_PICTURE_TILTED))
        assertTrue(RoomStateManager.Flags.LIVING_PICTURE_TILTED in manager.state(RoomId.LIVING_ROOM).physicalFlags)
    }

    @Test
    fun clearingPerceivedStateDoesNotErasePhysicalState() {
        val manager = RoomStateManager()
        manager.markPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_PICTURE_TILTED)
        manager.markPerceived(RoomId.LIVING_ROOM, "temporary_shadow")
        manager.clearPerceived(RoomId.LIVING_ROOM)
        assertFalse(manager.hasPerceived(RoomId.LIVING_ROOM, "temporary_shadow"))
        assertTrue(manager.hasPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_PICTURE_TILTED))
    }
}
