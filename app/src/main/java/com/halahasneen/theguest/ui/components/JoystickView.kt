package com.halahasneen.theguest.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onDirectionChanged: ((Float, Float) -> Unit)? = null

    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(115, 119, 126, 147)
    }
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(60, 17, 15, 24)
    }
    private val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(70, 180, 180, 190)
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(205, 214, 211, 205)
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(65, 232, 176, 75)
    }

    private var knobX = 0f
    private var knobY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        resetKnob()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) * 0.39f

        canvas.drawCircle(cx, cy, radius, innerPaint)
        canvas.drawCircle(cx, cy, radius, outerPaint)
        canvas.drawCircle(cx, cy, radius * 0.63f, guidePaint)

        val tick = radius * 0.16f
        canvas.drawLine(cx, cy - radius, cx, cy - radius + tick, guidePaint)
        canvas.drawLine(cx, cy + radius - tick, cx, cy + radius, guidePaint)
        canvas.drawLine(cx - radius, cy, cx - radius + tick, cy, guidePaint)
        canvas.drawLine(cx + radius - tick, cy, cx + radius, cy, guidePaint)

        if (isPressed) {
            canvas.drawCircle(knobX, knobY, radius * 0.31f, glowPaint)
        }
        canvas.drawCircle(knobX, knobY, radius * 0.23f, knobPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                updateFromTouch(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> updateFromTouch(event.x, event.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                resetKnob()
                onDirectionChanged?.invoke(0f, 0f)
                invalidate()
            }
        }
        return true
    }

    private fun updateFromTouch(x: Float, y: Float) {
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = minOf(width, height) * 0.39f
        val dx = x - cx
        val dy = y - cy
        val distance = hypot(dx, dy)
        val scale = if (distance > maxRadius && distance > 0f) maxRadius / distance else 1f
        knobX = cx + dx * scale
        knobY = cy + dy * scale
        onDirectionChanged?.invoke((knobX - cx) / maxRadius, (knobY - cy) / maxRadius)
        invalidate()
    }

    private fun resetKnob() {
        knobX = width / 2f
        knobY = height / 2f
    }
}
