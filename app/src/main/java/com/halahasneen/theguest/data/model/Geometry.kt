package com.halahasneen.theguest.data.model

data class NormalizedPoint(val x: Float, val y: Float)

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    init {
        require(left <= right && top <= bottom)
    }

    fun contains(point: NormalizedPoint, radius: Float = 0f): Boolean =
        point.x - radius >= left && point.x + radius <= right &&
            point.y - radius >= top && point.y + radius <= bottom

    fun overlapsCircle(point: NormalizedPoint, radius: Float): Boolean {
        val closestX = point.x.coerceIn(left, right)
        val closestY = point.y.coerceIn(top, bottom)
        val dx = point.x - closestX
        val dy = point.y - closestY
        return dx * dx + dy * dy < radius * radius
    }
}
