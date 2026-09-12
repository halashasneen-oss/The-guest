package com.halahasneen.theguest.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class LightingSystem {
    private val darknessPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = 96
    }
    private val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(232, 176, 75)
        alpha = 15
    }
    private val innerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(232, 176, 75)
        alpha = 24
    }

    fun draw(canvas: Canvas, width: Int, height: Int, normalizedX: Float, normalizedY: Float) {
        if (width <= 0 || height <= 0) return
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), darknessPaint)

        val x = normalizedX * width
        val y = normalizedY * height
        val unit = minOf(width, height).toFloat()
        canvas.drawCircle(x, y, unit * 0.23f, outerGlowPaint)
        canvas.drawCircle(x, y, unit * 0.12f, innerGlowPaint)
    }
}
