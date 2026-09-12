package com.halahasneen.theguest.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class LightingSystem {
    private val darknessPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }
    private val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(232, 176, 75)
    }
    private val middleGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(232, 176, 75)
    }
    private val innerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(246, 206, 123)
    }

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        normalizedX: Float,
        normalizedY: Float,
        tensionLevel: Float,
        extraDarkness: Float = 0f
    ) {
        if (width <= 0 || height <= 0) return

        val normalizedTension = (tensionLevel / 100f).coerceIn(0f, 1f)
        val blackout = extraDarkness.coerceIn(0f, 1f)

        // Keep the environment readable during normal exploration. Darkness rises
        // with tension and temporary horror events instead of hiding the art at rest.
        darknessPaint.alpha = (54 + normalizedTension * 64f + blackout * 112f)
            .toInt()
            .coerceIn(0, 232)
        outerGlowPaint.alpha = (24 - normalizedTension * 7f - blackout * 8f)
            .toInt()
            .coerceIn(6, 26)
        middleGlowPaint.alpha = (34 - normalizedTension * 9f - blackout * 10f)
            .toInt()
            .coerceIn(8, 36)
        innerGlowPaint.alpha = (46 - normalizedTension * 12f - blackout * 16f)
            .toInt()
            .coerceIn(10, 48)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), darknessPaint)

        val x = normalizedX * width
        val y = normalizedY * height
        val unit = minOf(width, height).toFloat()
        canvas.drawCircle(x, y, unit * 0.27f, outerGlowPaint)
        canvas.drawCircle(x, y, unit * 0.18f, middleGlowPaint)
        canvas.drawCircle(x, y, unit * 0.095f, innerGlowPaint)
    }
}
