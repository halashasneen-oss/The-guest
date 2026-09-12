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

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(80, 234, 234, 234) }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(180, 234, 234, 234) }
    private var knobX = 0f
    private var knobY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        resetKnob()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) * 0.42f
        canvas.drawCircle(cx, cy, radius, basePaint)
        canvas.drawCircle(knobX, knobY, radius * 0.38f, knobPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> updateFromTouch(event.x, event.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
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
        val maxRadius = minOf(width, height) * 0.42f
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
