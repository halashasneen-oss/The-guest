package com.halahasneen.theguest.data

import com.halahasneen.theguest.data.model.FinalChoice
import com.halahasneen.theguest.data.model.NarrativeState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NarrativeStateTest {
    @Test
    fun evidenceAndConfrontationFavorAcceptance() {
        val state = NarrativeState()
        state.recordEvidence(2)
        state.recordConfrontation(3)
        state.finalChoice = FinalChoice.CONFRONT
        assertTrue(state.acceptanceScore > state.denialScore)
        assertEquals(FinalChoice.CONFRONT, state.finalChoice)
    }

    @Test
    fun avoidanceFavorsDenial() {
        val state = NarrativeState()
        state.recordAvoidance(3)
        state.finalChoice = FinalChoice.TURN_AWAY
        assertTrue(state.denialScore > state.acceptanceScore)
        assertEquals(FinalChoice.TURN_AWAY, state.finalChoice)
    }
}
