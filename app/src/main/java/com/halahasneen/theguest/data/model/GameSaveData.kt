package com.halahasneen.theguest.data.model

data class RoomSaveState(
    val physicalFlags: Set<String> = emptySet(),
    val perceivedFlags: Set<String> = emptySet()
)

data class GameSaveData(
    val saveVersion: Int = CURRENT_SAVE_VERSION,
    val currentRoom: RoomId = RoomId.ENTRANCE,
    val playerPosition: NormalizedPoint = NormalizedPoint(0.18f, 0.50f),
    val collectedMemories: Set<String> = emptySet(),
    val hiddenHotspots: Set<String> = emptySet(),
    val tensionLevel: Float = 0f,
    val roomStates: Map<RoomId, RoomSaveState> = emptyMap(),
    val roomVisitCounts: Map<RoomId, Int> = mapOf(RoomId.ENTRANCE to 1),
    val acceptanceScore: Int = 0,
    val denialScore: Int = 0,
    val curiosityScore: Int = 0,
    val avoidanceScore: Int = 0,
    val finalChoice: FinalChoice? = null,
    val endingPath: EndingPath? = null,
    val triggeredOneShots: Set<String> = emptySet()
) {
    companion object {
        const val CURRENT_SAVE_VERSION = 1
    }
}
