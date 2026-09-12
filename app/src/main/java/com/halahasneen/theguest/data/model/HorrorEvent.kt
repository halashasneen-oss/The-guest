package com.halahasneen.theguest.data.model

enum class HorrorEventCategory {
    SUBTLE,
    AUDIO,
    VISUAL,
    STRONG
}

enum class HorrorAction {
    LIVING_PICTURE_TILT,
    LIVING_SINGLE_KNOCK,
    LIVING_LIGHT_DIM
}

data class HorrorEvent(
    val id: String,
    val roomId: RoomId,
    val minVisitCount: Int,
    val minMemories: Int,
    val minTension: Float,
    val weight: Float,
    val cooldownSeconds: Float,
    val oneShot: Boolean,
    val category: HorrorEventCategory,
    val action: HorrorAction
)

data class HorrorContext(
    val roomId: RoomId,
    val visitCount: Int,
    val memoriesCollected: Int,
    val tensionLevel: Float
)
