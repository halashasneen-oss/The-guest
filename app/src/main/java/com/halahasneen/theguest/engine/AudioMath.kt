package com.halahasneen.theguest.engine

import com.halahasneen.theguest.data.model.NormalizedPoint
import kotlin.math.sqrt

data class StereoVolumes(val left: Float, val right: Float)

object AudioMath {
    fun spatialVolumes(
        source: NormalizedPoint,
        listener: NormalizedPoint,
        baseVolume: Float,
        maxDistance: Float
    ): StereoVolumes {
        if (maxDistance <= 0f) return StereoVolumes(0f, 0f)

        val dx = source.x - listener.x
        val dy = source.y - listener.y
        val distance = sqrt(dx * dx + dy * dy)
        val attenuation = (1f - distance / maxDistance).coerceIn(0f, 1f)
        val pan = (dx / maxDistance).coerceIn(-1f, 1f)
        val volume = baseVolume.coerceIn(0f, 1f) * attenuation
        val left = volume * (1f - pan.coerceAtLeast(0f))
        val right = volume * (1f - (-pan).coerceAtLeast(0f))
        return StereoVolumes(left.coerceIn(0f, 1f), right.coerceIn(0f, 1f))
    }
}
