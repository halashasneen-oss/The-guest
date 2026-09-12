package com.halahasneen.theguest.engine

enum class TensionStage {
    CALM,
    UNEASY,
    DISTURBED,
    TERRIFIED
}

class TensionSystem(initialLevel: Float = 0f) {
    var level: Float = initialLevel.coerceIn(0f, 100f)
        private set

    val stage: TensionStage
        get() = when {
            level < 25f -> TensionStage.CALM
            level < 50f -> TensionStage.UNEASY
            level < 75f -> TensionStage.DISTURBED
            else -> TensionStage.TERRIFIED
        }

    val eventIntensityMultiplier: Float
        get() = when (stage) {
            TensionStage.CALM -> 0.65f
            TensionStage.UNEASY -> 0.90f
            TensionStage.DISTURBED -> 1.15f
            TensionStage.TERRIFIED -> 1.40f
        }

    fun update(deltaSeconds: Float, inDarkness: Boolean, inSafeLight: Boolean) {
        val dt = deltaSeconds.coerceIn(0f, 0.1f)
        val ratePerSecond = when {
            inSafeLight -> -1.15f
            inDarkness -> 0.58f
            else -> -0.18f
        }
        level = (level + ratePerSecond * dt).coerceIn(0f, 100f)
    }

    fun addStimulus(amount: Float) {
        level = (level + amount.coerceAtLeast(0f)).coerceIn(0f, 100f)
    }

    fun calm(amount: Float) {
        level = (level - amount.coerceAtLeast(0f)).coerceIn(0f, 100f)
    }
}
