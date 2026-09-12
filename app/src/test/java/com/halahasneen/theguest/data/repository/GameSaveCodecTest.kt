package com.halahasneen.theguest.data.repository

import com.halahasneen.theguest.data.model.EndingPath
import com.halahasneen.theguest.data.model.FinalChoice
import com.halahasneen.theguest.data.model.GameSaveData
import com.halahasneen.theguest.data.model.NormalizedPoint
import com.halahasneen.theguest.data.model.RoomId
import com.halahasneen.theguest.data.model.RoomSaveState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameSaveCodecTest {
    @Test
    fun roundTripPreservesProgress() {
        val original = GameSaveData(
            currentRoom = RoomId.BEDROOM,
            playerPosition = NormalizedPoint(0.34f, 0.62f),
            collectedMemories = setOf("entrance_key", "living_birthday_card"),
            hiddenHotspots = setOf("entrance_key", "living_birthday_card"),
            tensionLevel = 47.5f,
            roomStates = mapOf(
                RoomId.LIVING_ROOM to RoomSaveState(
                    physicalFlags = setOf("living_picture_tilted"),
                    perceivedFlags = setOf("fleeting_shadow")
                )
            ),
            roomVisitCounts = mapOf(RoomId.ENTRANCE to 2, RoomId.BEDROOM to 1),
            acceptanceScore = 4,
            denialScore = 1,
            curiosityScore = 3,
            avoidanceScore = 1,
            finalChoice = FinalChoice.CONFRONT,
            endingPath = EndingPath.TRUTH,
            triggeredOneShots = setOf("living_picture_tilt", "kitchen_object_drop")
        )

        val decoded = GameSaveCodec.decode(GameSaveCodec.encode(original))
        requireNotNull(decoded)
        assertEquals(original.copy(saveVersion = GameSaveData.CURRENT_SAVE_VERSION), decoded)
    }

    @Test
    fun oldSaveWithoutVersionMigratesUsingSafeDefaults() {
        val decoded = GameSaveCodec.decode("""{"currentRoom":"KITCHEN","tensionLevel":130}""")
        requireNotNull(decoded)
        assertEquals(RoomId.KITCHEN, decoded.currentRoom)
        assertEquals(100f, decoded.tensionLevel)
        assertTrue(decoded.roomVisitCounts.isNotEmpty())
    }

    @Test
    fun futureSaveVersionIsRejected() {
        assertNull(GameSaveCodec.decode("""{"saveVersion":999,"currentRoom":"ENTRANCE"}"""))
    }
}
