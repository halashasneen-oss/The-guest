package com.halahasneen.theguest.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class InteractionButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onInteraction: (() -> Unit)? = null

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(72, 12, 10, 18)
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.rgb(118, 125, 145)
        alpha = 155
    }
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.rgb(224, 222, 216)
        alpha = 190
    }
    private val pupilGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(65, 232, 176, 75)
    }
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(232, 176, 75)
    }
    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) * 0.39f

        backgroundPaint.alpha = if (isPressed) 115 else 72
        ringPaint.alpha = if (isPressed) 225 else 145
        eyePaint.alpha = if (isPressed) 255 else 185

        canvas.drawCircle(cx, cy, radius, backgroundPaint)
        canvas.drawCircle(cx, cy, radius, ringPaint)

        path.reset()
        path.moveTo(cx - radius * 0.58f, cy)
        path.quadTo(cx, cy - radius * 0.42f, cx + radius * 0.58f, cy)
        path.quadTo(cx, cy + radius * 0.42f, cx - radius * 0.58f, cy)
        path.close()
        canvas.drawPath(path, eyePaint)

        canvas.drawCircle(cx, cy, radius * 0.25f, pupilGlowPaint)
        canvas.drawCircle(cx, cy, radius * 0.105f, pupilPaint)
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
