package com.halahasneen.theguest.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class InteractionButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onInteraction: (() -> Unit)? = null

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.rgb(234, 234, 234)
        alpha = 150
    }
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.rgb(234, 234, 234)
        alpha = 180
    }
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(232, 176, 75)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) * 0.38f
        ringPaint.alpha = if (isPressed) 230 else 140
        eyePaint.alpha = if (isPressed) 255 else 175
        canvas.drawCircle(cx, cy, radius, ringPaint)
        canvas.drawOval(
            cx - radius * 0.55f,
            cy - radius * 0.30f,
            cx + radius * 0.55f,
            cy + radius * 0.30f,
            eyePaint
        )
        canvas.drawCircle(cx, cy, radius * 0.13f, pupilPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val trigger = isPressed && event.x in 0f..width.toFloat() && event.y in 0f..height.toFloat()
                isPressed = false
                invalidate()
                if (trigger) performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        onInteraction?.invoke()
        return true
    }
}
