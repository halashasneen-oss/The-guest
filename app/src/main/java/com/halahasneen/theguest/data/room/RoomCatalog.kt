package com.halahasneen.theguest.data.room

import com.halahasneen.theguest.data.model.Hotspot
import com.halahasneen.theguest.data.model.HotspotType
import com.halahasneen.theguest.data.model.MemoryItem
import com.halahasneen.theguest.data.model.NormalizedPoint
import com.halahasneen.theguest.data.model.NormalizedRect
import com.halahasneen.theguest.data.model.RoomData
import com.halahasneen.theguest.data.model.RoomId

object RoomCatalog {
    val entranceMemory = MemoryItem("entrance_key", "مفتاح البيت", "مفتاح نحاسي قديم. ما زالت عليه خدوش أعرفها، لكني لا أتذكر متى حدثت.", RoomId.ENTRANCE, 1)
    val livingMemory = MemoryItem("living_birthday_card", "بطاقة عيد قديمة", "بطاقة باسم العائلة. هناك توقيع خامس مطموس بالحبر، مع أنني أتذكر أربعة أسماء فقط.", RoomId.LIVING_ROOM, 2)
    val kitchenMemory = MemoryItem("kitchen_chipped_cup", "الكوب المشروخ", "كوب قديم عليه خمسة خطوط محفورة. أتذكر أن لكل واحد منا خطًا... لكننا كنا أربعة.", RoomId.KITCHEN, 3)
    val bedroomMemory = MemoryItem("bedroom_letter_fragment", "قصاصة رسالة", "جزء ممزق من رسالة قديمة. الخط مألوف، لكن الجملة الأخيرة تبدو وكأنها موجهة إليّ الآن.", RoomId.BEDROOM, 4)
    val basementMemory = MemoryItem("basement_tape", "شريط التسجيل", "شريط قديم بصوت أحد أفراد العائلة. التسجيل يذكر اسمي وكأنني الشخص الذي غادر البيت، لا الشخص الذي عاد إليه.", RoomId.BASEMENT, 5)

    val entrance = RoomData(
        RoomId.ENTRANCE, "المدخل", NormalizedRect(0.07f, 0.10f, 0.93f, 0.90f),
        listOf(NormalizedRect(0.15f, 0.58f, 0.34f, 0.72f), NormalizedRect(0.67f, 0.15f, 0.82f, 0.29f)),
        listOf(
            Hotspot("entrance_clock", HotspotType.INSPECT, NormalizedPoint(0.20f, 0.24f), 0.09f, "افحص الساعة", "الساعة متوقفة عند 2:17... منذ سنوات، على ما أظن."),
            Hotspot("entrance_key", HotspotType.COLLECT, NormalizedPoint(0.25f, 0.55f), 0.08f, "التقط المفتاح", "ذكرى 1/5 — مفتاح البيت", memoryItemId = entranceMemory.id),
            Hotspot("entrance_to_living", HotspotType.DOOR, NormalizedPoint(0.87f, 0.50f), 0.09f, "افتح باب الصالون", "الصالون ينتظرك في الداخل.", targetRoom = RoomId.LIVING_ROOM)
        ), entranceMemory.id
    )

    val livingRoom = RoomData(
        RoomId.LIVING_ROOM, "الصالون", NormalizedRect(0.07f, 0.10f, 0.93f, 0.90f),
        listOf(NormalizedRect(0.37f, 0.36f, 0.63f, 0.61f), NormalizedRect(0.72f, 0.58f, 0.86f, 0.70f), NormalizedRect(0.16f, 0.17f, 0.29f, 0.29f)),
        listOf(
            Hotspot("living_family_picture", HotspotType.INSPECT, NormalizedPoint(0.75f, 0.27f), 0.10f, "افحص الصورة العائلية", "أربعة وجوه مألوفة. هكذا أتذكرها دائمًا."),
            Hotspot("living_birthday_card", HotspotType.COLLECT, NormalizedPoint(0.79f, 0.55f), 0.08f, "التقط البطاقة", "ذكرى 2/5 — بطاقة عيد قديمة", memoryItemId = livingMemory.id),
            Hotspot("living_to_entrance", HotspotType.DOOR, NormalizedPoint(0.12f, 0.50f), 0.09f, "ارجع إلى المدخل", "عدت إلى المدخل.", targetRoom = RoomId.ENTRANCE),
            Hotspot("living_to_kitchen", HotspotType.DOOR, NormalizedPoint(0.88f, 0.73f), 0.09f, "اذهب إلى المطبخ", "رائحة معدن بارد تأتي من المطبخ.", targetRoom = RoomId.KITCHEN)
        ), livingMemory.id
    )

    val kitchen = RoomData(
        RoomId.KITCHEN, "المطبخ", NormalizedRect(0.07f, 0.10f, 0.93f, 0.90f),
        listOf(NormalizedRect(0.16f, 0.16f, 0.38f, 0.31f), NormalizedRect(0.43f, 0.43f, 0.62f, 0.68f), NormalizedRect(0.70f, 0.16f, 0.88f, 0.31f)),
        listOf(
            Hotspot("kitchen_sink", HotspotType.INSPECT, NormalizedPoint(0.79f, 0.33f), 0.09f, "افحص الحوض", "الحوض جاف، لكن هناك قطرة ماء جديدة على الحافة."),
            Hotspot("kitchen_chipped_cup", HotspotType.COLLECT, NormalizedPoint(0.72f, 0.57f), 0.08f, "التقط الكوب", "ذكرى 3/5 — الكوب المشروخ", memoryItemId = kitchenMemory.id),
            Hotspot("kitchen_loose_jar", HotspotType.INSPECT, NormalizedPoint(0.57f, 0.73f), 0.08f, "افحص المرطبان", "لا أتذكر أن هذا المرطبان كان هنا."),
            Hotspot("kitchen_to_living", HotspotType.DOOR, NormalizedPoint(0.12f, 0.51f), 0.09f, "ارجع إلى الصالون", "عدت إلى الصالون.", targetRoom = RoomId.LIVING_ROOM),
            Hotspot("kitchen_to_bedroom", HotspotType.DOOR, NormalizedPoint(0.88f, 0.52f), 0.09f, "اذهب إلى غرفة النوم", "باب غرفة النوم مفتوح قليلًا.", targetRoom = RoomId.BEDROOM)
        ), kitchenMemory.id
    )

    val bedroom = RoomData(
        RoomId.BEDROOM, "غرفة النوم", NormalizedRect(0.07f, 0.10f, 0.93f, 0.90f),
        listOf(
            NormalizedRect(0.48f, 0.34f, 0.76f, 0.69f),
            NormalizedRect(0.16f, 0.17f, 0.31f, 0.31f),
            NormalizedRect(0.18f, 0.66f, 0.31f, 0.79f)
        ),
        listOf(
            Hotspot("bedroom_note", HotspotType.INSPECT, NormalizedPoint(0.72f, 0.25f), 0.09f, "اقرأ الرسالة", "إذا عدت يومًا، لا تنزل إلى القبو."),
            Hotspot("bedroom_letter_fragment", HotspotType.COLLECT, NormalizedPoint(0.30f, 0.58f), 0.08f, "التقط القصاصة", "ذكرى 4/5 — قصاصة رسالة", memoryItemId = bedroomMemory.id),
            Hotspot("bedroom_to_kitchen", HotspotType.DOOR, NormalizedPoint(0.12f, 0.52f), 0.09f, "ارجع إلى المطبخ", "عدت إلى المطبخ.", targetRoom = RoomId.KITCHEN),
            Hotspot("bedroom_to_basement", HotspotType.DOOR, NormalizedPoint(0.86f, 0.73f), 0.09f, "انزل إلى القبو", "الهواء أسفل الدرج بارد وثقيل.", targetRoom = RoomId.BASEMENT)
        ), bedroomMemory.id
    )

    val basement = RoomData(
        RoomId.BASEMENT, "القبو", NormalizedRect(0.07f, 0.10f, 0.93f, 0.90f),
        listOf(
            NormalizedRect(0.15f, 0.16f, 0.35f, 0.31f),
            NormalizedRect(0.42f, 0.39f, 0.60f, 0.64f),
            NormalizedRect(0.70f, 0.18f, 0.86f, 0.33f)
        ),
        listOf(
            Hotspot("basement_family_box", HotspotType.INSPECT, NormalizedPoint(0.26f, 0.35f), 0.09f, "افحص الصندوق", "أغراض قديمة مرتبة بعناية... كأن أحدًا كان ينتظر عودتي."),
            Hotspot("basement_tape", HotspotType.COLLECT, NormalizedPoint(0.53f, 0.70f), 0.08f, "التقط شريط التسجيل", "ذكرى 5/5 — شريط التسجيل", memoryItemId = basementMemory.id),
            Hotspot("basement_confront", HotspotType.CHOICE, NormalizedPoint(0.78f, 0.52f), 0.10f, "واجه الحقيقة", "لن أهرب هذه المرة."),
            Hotspot("basement_turn_away", HotspotType.CHOICE, NormalizedPoint(0.22f, 0.72f), 0.10f, "ابتعد عن الحقيقة", "يكفي. أريد الخروج من هنا."),
            Hotspot("basement_to_bedroom", HotspotType.DOOR, NormalizedPoint(0.12f, 0.50f), 0.09f, "اصعد إلى غرفة النوم", "عدت إلى غرفة النوم.", targetRoom = RoomId.BEDROOM)
        ), basementMemory.id
    )

    fun room(id: RoomId): RoomData = when (id) {
        RoomId.ENTRANCE -> entrance
        RoomId.LIVING_ROOM -> livingRoom
        RoomId.KITCHEN -> kitchen
        RoomId.BEDROOM -> bedroom
        RoomId.BASEMENT -> basement
    }
}
