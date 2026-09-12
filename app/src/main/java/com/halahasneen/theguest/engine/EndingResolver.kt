package com.halahasneen.theguest.engine

import com.halahasneen.theguest.data.model.EndingPath
import com.halahasneen.theguest.data.model.FinalChoice
import com.halahasneen.theguest.data.model.NarrativeState

object EndingResolver {
    fun resolve(state: NarrativeState, memoriesCollected: Int): EndingPath {
        val truthWeight = state.acceptanceScore + state.curiosityScore + memoriesCollected * 2 +
            if (state.finalChoice == FinalChoice.CONFRONT) 5 else 0
        val denialWeight = state.denialScore + state.avoidanceScore +
            if (state.finalChoice == FinalChoice.TURN_AWAY) 6 else 0
        return if (memoriesCollected >= 4 && truthWeight > denialWeight) EndingPath.TRUTH else EndingPath.DENIAL
    }
}
