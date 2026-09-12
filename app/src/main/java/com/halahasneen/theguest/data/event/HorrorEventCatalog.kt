package com.halahasneen.theguest.data.event

import com.halahasneen.theguest.data.model.HorrorAction
import com.halahasneen.theguest.data.model.HorrorEvent
import com.halahasneen.theguest.data.model.HorrorEventCategory
import com.halahasneen.theguest.data.model.RoomId

object HorrorEventCatalog {
    val events = listOf(
        HorrorEvent(
            id = "living_picture_tilt",
            roomId = RoomId.LIVING_ROOM,
            minVisitCount = 1,
            minMemories = 0,
            minTension = 7f,
            weight = 5f,
            cooldownSeconds = 999f,
            oneShot = true,
            category = HorrorEventCategory.SUBTLE,
            action = HorrorAction.LIVING_PICTURE_TILT
        ),
        HorrorEvent(
            id = "living_single_knock",
            roomId = RoomId.LIVING_ROOM,
            minVisitCount = 1,
            minMemories = 0,
            minTension = 12f,
            weight = 3f,
            cooldownSeconds = 25f,
            oneShot = false,
            category = HorrorEventCategory.AUDIO,
            action = HorrorAction.LIVING_SINGLE_KNOCK
        ),
        HorrorEvent(
            id = "living_light_dim",
            roomId = RoomId.LIVING_ROOM,
            minVisitCount = 1,
            minMemories = 0,
            minTension = 20f,
            weight = 2f,
            cooldownSeconds = 999f,
            oneShot = true,
            category = HorrorEventCategory.VISUAL,
            action = HorrorAction.LIVING_LIGHT_DIM
        )
    )
}
