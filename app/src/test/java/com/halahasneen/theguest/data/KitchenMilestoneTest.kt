package com.halahasneen.theguest.data

import com.halahasneen.theguest.data.event.HorrorEventCatalog
import com.halahasneen.theguest.data.model.HorrorAction
import com.halahasneen.theguest.data.model.HotspotType
import com.halahasneen.theguest.data.model.RoomId
import com.halahasneen.theguest.data.room.RoomCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KitchenMilestoneTest {
    @Test
    fun kitchenHasMemoryAndReturnDoor() {
        val kitchen = RoomCatalog.kitchen
        assertEquals(RoomId.KITCHEN, kitchen.id)
        assertEquals(RoomCatalog.kitchenMemory.id, kitchen.memoryItemId)
        assertTrue(kitchen.hotspots.any { it.type == HotspotType.DOOR && it.targetRoom == RoomId.LIVING_ROOM })
    }

    @Test
    fun livingRoomConnectsToKitchen() {
        assertTrue(RoomCatalog.livingRoom.hotspots.any { it.targetRoom == RoomId.KITCHEN })
    }

    @Test
    fun kitchenEventsCoverAudioAndStateChanges() {
        val actions = HorrorEventCatalog.events.filter { it.roomId == RoomId.KITCHEN }.map { it.action }.toSet()
        assertTrue(HorrorAction.KITCHEN_FOOTSTEPS in actions)
        assertTrue(HorrorAction.KITCHEN_OBJECT_DROP in actions)
        assertTrue(HorrorAction.KITCHEN_ITEM_MOVE in actions)
    }
}
