package com.halahasneen.theguest.data.model

enum class RoomId {
    ENTRANCE,
    LIVING_ROOM
}

enum class HotspotType {
    INSPECT,
    COLLECT,
    DOOR
}

data class Hotspot(
    val id: String,
    val type: HotspotType,
    val position: NormalizedPoint,
    val radius: Float,
    val label: String,
    val message: String,
    val targetRoom: RoomId? = null,
    val memoryItemId: String? = null
)

data class MemoryItem(
    val id: String,
    val title: String,
    val description: String,
    val relatedRoom: RoomId,
    val narrativeWeight: Int
)

data class RoomData(
    val id: RoomId,
    val name: String,
    val walkableBounds: NormalizedRect,
    val collisionRects: List<NormalizedRect>,
    val hotspots: List<Hotspot>,
    val memoryItemId: String? = null
)
