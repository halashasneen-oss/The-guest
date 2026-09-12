package com.halahasneen.theguest.engine

import com.halahasneen.theguest.data.model.Hotspot
import com.halahasneen.theguest.data.model.NormalizedPoint

class InteractionSystem(
    private val interactionPadding: Float = 0.045f
) {
    fun nearest(
        playerPosition: NormalizedPoint,
        hotspots: List<Hotspot>,
        hiddenHotspotIds: Set<String> = emptySet()
    ): Hotspot? = hotspots
        .asSequence()
        .filterNot { it.id in hiddenHotspotIds }
        .map { it to distanceSquared(playerPosition, it.position) }
        .filter { (hotspot, distanceSquared) ->
            val maxDistance = hotspot.radius + interactionPadding
            distanceSquared <= maxDistance * maxDistance
        }
        .minByOrNull { it.second }
        ?.first

    private fun distanceSquared(a: NormalizedPoint, b: NormalizedPoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy
    }
}
