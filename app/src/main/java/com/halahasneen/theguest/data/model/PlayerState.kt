package com.halahasneen.theguest.data.model

data class PlayerState(
    var position: NormalizedPoint = NormalizedPoint(0.2f, 0.5f),
    val radius: Float = 0.025f,
    val speedPerSecond: Float = 0.28f
)
