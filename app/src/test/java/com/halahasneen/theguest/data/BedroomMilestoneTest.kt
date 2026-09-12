package com.halahasneen.theguest.data

import com.halahasneen.theguest.data.event.HorrorEventCatalog
import com.halahasneen.theguest.data.model.HorrorAction
import com.halahasneen.theguest.data.model.HotspotType
import com.halahasneen.theguest.data.model.RoomId
import com.halahasneen.theguest.data.room.RoomCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BedroomMilestoneTest {
    @Test
    fun bedroomHasMemoryAndReturnDoor() {
        val bedroom = RoomCatalog.bedroom
        assertEquals(RoomId.BEDROOM, bedroom.id)
        assertEquals(RoomCatalog.bedroomMemory.id, bedroom.memoryItemId)
        assertTrue(bedroom.hotspots.any { it.type == HotspotType.DOOR && it.targetRoom == RoomId.KITCHEN })
    }

    @Test
    fun kitchenConnectsToBedroom() {
        assertTrue(RoomCatalog.kitchen.hotspots.any { it.type == HotspotType.DOOR && it.targetRoom == RoomId.BEDROOM })
    }

    @Test
    fun bedroomEventsCoverRequiredHorrorBeats() {
        val actions = HorrorEventCatalog.events.filter { it.roomId == RoomId.BEDROOM }.map { it.action }.toSet()
        assertTrue(HorrorAction.BEDROOM_SHADOW in actions)
        assertTrue(HorrorAction.BEDROOM_MESSAGE_CHANGE in actions)
        assertTrue(HorrorAction.BEDROOM_DOOR_MOVE in actions)
        assertTrue(HorrorAction.BEDROOM_WHISPER in actions)
    }
}
