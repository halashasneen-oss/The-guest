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
    private val innerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(232, 176, 75)
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
        darknessPaint.alpha = (92 + normalizedTension * 52f + blackout * 72f).toInt().coerceIn(0, 230)
        outerGlowPaint.alpha = (18 - normalizedTension * 6f - blackout * 6f).toInt().coerceIn(5, 24)
        innerGlowPaint.alpha = (28 - normalizedTension * 8f - blackout * 8f).toInt().coerceIn(8, 32)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), darknessPaint)

        val x = normalizedX * width
        val y = normalizedY * height
        val unit = minOf(width, height).toFloat()
        canvas.drawCircle(x, y, unit * 0.23f, outerGlowPaint)
        canvas.drawCircle(x, y, unit * 0.12f, innerGlowPaint)
    }
}
