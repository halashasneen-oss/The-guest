package com.halahasneen.theguest.data.event

import com.halahasneen.theguest.data.model.HorrorAction
import com.halahasneen.theguest.data.model.HorrorEvent
import com.halahasneen.theguest.data.model.HorrorEventCategory
import com.halahasneen.theguest.data.model.RoomId

object HorrorEventCatalog {
    val events = listOf(
        HorrorEvent("living_picture_tilt", RoomId.LIVING_ROOM, 1, 0, 7f, 5f, 999f, true, HorrorEventCategory.SUBTLE, HorrorAction.LIVING_PICTURE_TILT),
        HorrorEvent("living_single_knock", RoomId.LIVING_ROOM, 1, 0, 12f, 3f, 25f, false, HorrorEventCategory.AUDIO, HorrorAction.LIVING_SINGLE_KNOCK),
        HorrorEvent("living_light_dim", RoomId.LIVING_ROOM, 1, 0, 20f, 2f, 999f, true, HorrorEventCategory.VISUAL, HorrorAction.LIVING_LIGHT_DIM),
        HorrorEvent("kitchen_footsteps", RoomId.KITCHEN, 1, 1, 13f, 4f, 24f, false, HorrorEventCategory.AUDIO, HorrorAction.KITCHEN_FOOTSTEPS),
        HorrorEvent("kitchen_object_drop", RoomId.KITCHEN, 1, 2, 21f, 3f, 999f, true, HorrorEventCategory.AUDIO, HorrorAction.KITCHEN_OBJECT_DROP),
        HorrorEvent("kitchen_item_move", RoomId.KITCHEN, 1, 2, 27f, 2f, 999f, true, HorrorEventCategory.SUBTLE, HorrorAction.KITCHEN_ITEM_MOVE),
        HorrorEvent("bedroom_shadow", RoomId.BEDROOM, 1, 2, 24f, 4f, 999f, true, HorrorEventCategory.VISUAL, HorrorAction.BEDROOM_SHADOW),
        HorrorEvent("bedroom_whisper", RoomId.BEDROOM, 1, 2, 27f, 3f, 28f, false, HorrorEventCategory.AUDIO, HorrorAction.BEDROOM_WHISPER),
        HorrorEvent("bedroom_message_change", RoomId.BEDROOM, 1, 3, 31f, 3f, 999f, true, HorrorEventCategory.SUBTLE, HorrorAction.BEDROOM_MESSAGE_CHANGE),
        HorrorEvent("bedroom_door_move", RoomId.BEDROOM, 1, 3, 36f, 2f, 999f, true, HorrorEventCategory.STRONG, HorrorAction.BEDROOM_DOOR_MOVE),
        HorrorEvent("basement_chain_rattle", RoomId.BASEMENT, 1, 3, 34f, 4f, 20f, false, HorrorEventCategory.AUDIO, HorrorAction.BASEMENT_CHAIN_RATTLE),
        HorrorEvent("basement_blackout", RoomId.BASEMENT, 1, 4, 42f, 3f, 999f, true, HorrorEventCategory.STRONG, HorrorAction.BASEMENT_BLACKOUT),
        HorrorEvent("basement_presence", RoomId.BASEMENT, 1, 4, 48f, 2f, 999f, true, HorrorEventCategory.VISUAL, HorrorAction.BASEMENT_PRESENCE)
    )
}
