package com.halahasneen.theguest.data.room

import com.halahasneen.theguest.data.model.Hotspot
import com.halahasneen.theguest.data.model.HotspotType
import com.halahasneen.theguest.data.model.MemoryItem
import com.halahasneen.theguest.data.model.NormalizedPoint
import com.halahasneen.theguest.data.model.NormalizedRect
import com.halahasneen.theguest.data.model.RoomData
import com.halahasneen.theguest.data.model.RoomId

object RoomCatalog {
    val entranceMemory = MemoryItem(
        id = "entrance_key",
        title = "مفتاح البيت",
        description = "مفتاح نحاسي قديم. ما زالت عليه خدوش أعرفها، لكني لا أتذكر متى حدثت.",
        relatedRoom = RoomId.ENTRANCE,
        narrativeWeight = 1
    )

    val livingMemory = MemoryItem(
        id = "living_birthday_card",
        title = "بطاقة عيد قديمة",
        description = "بطاقة باسم العائلة. هناك توقيع خامس مطموس بالحبر، مع أنني أتذكر أربعة أسماء فقط.",
        relatedRoom = RoomId.LIVING_ROOM,
        narrativeWeight = 2
    )

    val entrance = RoomData(
        id = RoomId.ENTRANCE,
        name = "المدخل",
        walkableBounds = NormalizedRect(0.07f, 0.10f, 0.93f, 0.90f),
        collisionRects = listOf(
            NormalizedRect(0.15f, 0.58f, 0.34f, 0.72f),
            NormalizedRect(0.67f, 0.15f, 0.82f, 0.29f)
        ),
        hotspots = listOf(
            Hotspot(
                id = "entrance_clock",
                type = HotspotType.INSPECT,
                position = NormalizedPoint(0.20f, 0.24f),
                radius = 0.09f,
                label = "افحص الساعة",
                message = "الساعة متوقفة عند 2:17... منذ سنوات، على ما أظن."
            ),
            Hotspot(
                id = "entrance_key",
                type = HotspotType.COLLECT,
                position = NormalizedPoint(0.25f, 0.55f),
                radius = 0.08f,
                label = "التقط المفتاح",
                message = "ذكرى 1/5 — مفتاح البيت",
                memoryItemId = entranceMemory.id
            ),
            Hotspot(
                id = "entrance_to_living",
                type = HotspotType.DOOR,
                position = NormalizedPoint(0.87f, 0.50f),
                radius = 0.09f,
                label = "افتح باب الصالون",
                message = "الصالون ينتظرك في الداخل.",
                targetRoom = RoomId.LIVING_ROOM
            )
        ),
        memoryItemId = entranceMemory.id
    )

    val livingRoom = RoomData(
        id = RoomId.LIVING_ROOM,
        name = "الصالون",
        walkableBounds = NormalizedRect(0.07f, 0.10f, 0.93f, 0.90f),
        collisionRects = listOf(
            NormalizedRect(0.37f, 0.36f, 0.63f, 0.61f),
            NormalizedRect(0.72f, 0.58f, 0.86f, 0.70f),
            NormalizedRect(0.16f, 0.17f, 0.29f, 0.29f)
        ),
        hotspots = listOf(
            Hotspot(
                id = "living_family_picture",
                type = HotspotType.INSPECT,
                position = NormalizedPoint(0.75f, 0.27f),
                radius = 0.10f,
                label = "افحص الصورة العائلية",
                message = "أربعة وجوه مألوفة. هكذا أتذكرها دائمًا."
            ),
            Hotspot(
                id = "living_birthday_card",
                type = HotspotType.COLLECT,
                position = NormalizedPoint(0.79f, 0.55f),
                radius = 0.08f,
                label = "التقط البطاقة",
                message = "ذكرى 2/5 — بطاقة عيد قديمة",
                memoryItemId = livingMemory.id
            ),
            Hotspot(
                id = "living_to_entrance",
                type = HotspotType.DOOR,
                position = NormalizedPoint(0.12f, 0.50f),
                radius = 0.09f,
                label = "ارجع إلى المدخل",
                message = "عدت إلى المدخل.",
                targetRoom = RoomId.ENTRANCE
            )
        ),
        memoryItemId = livingMemory.id
    )

    fun room(id: RoomId): RoomData = when (id) {
        RoomId.ENTRANCE -> entrance
        RoomId.LIVING_ROOM -> livingRoom
    }
}
