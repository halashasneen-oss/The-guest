package com.halahasneen.theguest.engine

import com.halahasneen.theguest.data.model.EndingPath
import com.halahasneen.theguest.data.model.FinalChoice
import com.halahasneen.theguest.data.model.NarrativeState
import kotlin.test.Test
import kotlin.test.assertEquals

class EndingResolverTest {
    @Test
    fun truthRequiresEvidenceAndConfrontationWeight() {
        val state = NarrativeState(acceptanceScore = 5, curiosityScore = 4, finalChoice = FinalChoice.CONFRONT)
        assertEquals(EndingPath.TRUTH, EndingResolver.resolve(state, memoriesCollected = 5))
    }

    @Test
    fun turningAwayCanResolveToDenial() {
        val state = NarrativeState(denialScore = 5, avoidanceScore = 3, finalChoice = FinalChoice.TURN_AWAY)
        assertEquals(EndingPath.DENIAL, EndingResolver.resolve(state, memoriesCollected = 3))
    }
}
