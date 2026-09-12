package com.halahasneen.theguest.engine

import com.halahasneen.theguest.data.model.NormalizedPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioMathTest {
    @Test
    fun centeredSourceProducesBalancedStereo() {
        val volumes = AudioMath.spatialVolumes(
            source = NormalizedPoint(0.5f, 0.5f),
            listener = NormalizedPoint(0.5f, 0.5f),
            baseVolume = 0.8f,
            maxDistance = 1f
        )
        assertEquals(volumes.left, volumes.right, 0.0001f)
        assertEquals(0.8f, volumes.left, 0.0001f)
    }

    @Test
    fun rightSideSourceFavorsRightChannel() {
        val volumes = AudioMath.spatialVolumes(
            source = NormalizedPoint(0.8f, 0.5f),
            listener = NormalizedPoint(0.4f, 0.5f),
            baseVolume = 1f,
            maxDistance = 1f
        )
        assertTrue(volumes.right > volumes.left)
    }

    @Test
    fun distantSourceIsSilent() {
        val volumes = AudioMath.spatialVolumes(
            source = NormalizedPoint(1f, 1f),
            listener = NormalizedPoint(0f, 0f),
            baseVolume = 1f,
            maxDistance = 0.5f
        )
        assertEquals(0f, volumes.left)
        assertEquals(0f, volumes.right)
    }
}
