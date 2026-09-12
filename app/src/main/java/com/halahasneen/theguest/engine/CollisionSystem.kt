package com.halahasneen.theguest.engine

import com.halahasneen.theguest.data.model.NormalizedPoint
import com.halahasneen.theguest.data.model.NormalizedRect

class CollisionSystem(
    private val walkableBounds: NormalizedRect,
    private val obstacles: List<NormalizedRect>
) {
    fun resolve(current: NormalizedPoint, desired: NormalizedPoint, radius: Float): NormalizedPoint {
        if (isValid(desired, radius)) return desired

        val xOnly = NormalizedPoint(desired.x, current.y)
        if (isValid(xOnly, radius)) return xOnly

        val yOnly = NormalizedPoint(current.x, desired.y)
        if (isValid(yOnly, radius)) return yOnly

        return current
    }

    fun isValid(point: NormalizedPoint, radius: Float): Boolean =
        walkableBounds.contains(point, radius) && obstacles.none { it.overlapsCircle(point, radius) }
}
