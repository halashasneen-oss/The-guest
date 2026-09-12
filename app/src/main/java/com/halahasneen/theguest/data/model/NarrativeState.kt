package com.halahasneen.theguest.data.model

data class NarrativeState(
    var acceptanceScore: Int = 0,
    var denialScore: Int = 0,
    var curiosityScore: Int = 0,
    var avoidanceScore: Int = 0,
    var finalChoice: FinalChoice? = null
) {
    fun recordEvidence(weight: Int = 1) {
        curiosityScore += weight.coerceAtLeast(0)
        acceptanceScore += 1
    }

    fun recordConfrontation(weight: Int = 2) {
        acceptanceScore += weight.coerceAtLeast(0)
        curiosityScore += 1
    }

    fun recordAvoidance(weight: Int = 2) {
        denialScore += weight.coerceAtLeast(0)
        avoidanceScore += 1
    }
}

enum class FinalChoice {
    CONFRONT,
    TURN_AWAY
}
