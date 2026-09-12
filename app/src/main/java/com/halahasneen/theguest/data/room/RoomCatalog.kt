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

    val kitchenMemory = MemoryItem(
        id = "kitchen_chipped_cup",
        title = "الكوب المشروخ",
        description = "كوب قديم عليه خمسة خطوط محفورة. أتذكر أن لكل واحد منا خطًا... لكننا كنا أربعة.",
        relatedRoom = RoomId.KITCHEN,
        narrativeWeight = 3
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
            Hotspot("entrance_clock", HotspotType.INSPECT, NormalizedPoint(0.20f, 0.24f), 0.09f, "افحص الساعة", "الساعة متوقفة عند 2:17... منذ سنوات، على ما أظن."),
            Hotspot("entrance_key", HotspotType.COLLECT, NormalizedPoint(0.25f, 0.55f), 0.08f, "التقط المفتاح", "ذكرى 1/5 — مفتاح البيت", memoryItemId = entranceMemory.id),
            Hotspot("entrance_to_living", HotspotType.DOOR, NormalizedPoint(0.87f, 0.50f), 0.09f, "افتح باب الصالون", "الصالون ينتظرك في الداخل.", targetRoom = RoomId.LIVING_ROOM)
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
            Hotspot("living_family_picture", HotspotType.INSPECT, NormalizedPoint(0.75f, 0.27f), 0.10f, "افحص الصورة العائلية", "أربعة وجوه مألوفة. هكذا أتذكرها دائمًا."),
            Hotspot("living_birthday_card", HotspotType.COLLECT, NormalizedPoint(0.79f, 0.55f), 0.08f, "التقط البطاقة", "ذكرى 2/5 — بطاقة عيد قديمة", memoryItemId = livingMemory.id),
            Hotspot("living_to_entrance", HotspotType.DOOR, NormalizedPoint(0.12f, 0.50f), 0.09f, "ارجع إلى المدخل", "عدت إلى المدخل.", targetRoom = RoomId.ENTRANCE),
            Hotspot("living_to_kitchen", HotspotType.DOOR, NormalizedPoint(0.88f, 0.73f), 0.09f, "اذهب إلى المطبخ", "رائحة معدن بارد تأتي من المطبخ.", targetRoom = RoomId.KITCHEN)
        ),
        memoryItemId = livingMemory.id
    )

    val kitchen = RoomData(
        id = RoomId.KITCHEN,
        name = "المطبخ",
        walkableBounds = NormalizedRect(0.07f, 0.10f, 0.93f, 0.90f),
        collisionRects = listOf(
            NormalizedRect(0.16f, 0.16f, 0.38f, 0.31f),
            NormalizedRect(0.43f, 0.43f, 0.62f, 0.68f),
            NormalizedRect(0.70f, 0.16f, 0.88f, 0.31f)
        ),
        hotspots = listOf(
            Hotspot("kitchen_sink", HotspotType.INSPECT, NormalizedPoint(0.79f, 0.33f), 0.09f, "افحص الحوض", "الحوض جاف، لكن هناك قطرة ماء جديدة على الحافة."),
            Hotspot("kitchen_chipped_cup", HotspotType.COLLECT, NormalizedPoint(0.72f, 0.57f), 0.08f, "التقط الكوب", "ذكرى 3/5 — الكوب المشروخ", memoryItemId = kitchenMemory.id),
            Hotspot("kitchen_loose_jar", HotspotType.INSPECT, NormalizedPoint(0.57f, 0.73f), 0.08f, "افحص المرطبان", "لا أتذكر أن هذا المرطبان كان هنا."),
            Hotspot("kitchen_to_living", HotspotType.DOOR, NormalizedPoint(0.12f, 0.51f), 0.09f, "ارجع إلى الصالون", "عدت إلى الصالون.", targetRoom = RoomId.LIVING_ROOM)
        ),
        memoryItemId = kitchenMemory.id
    )

    fun room(id: RoomId): RoomData = when (id) {
        RoomId.ENTRANCE -> entrance
        RoomId.LIVING_ROOM -> livingRoom
        RoomId.KITCHEN -> kitchen
    }
}
