package com.halahasneen.theguest.ui.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.halahasneen.theguest.data.model.NormalizedPoint
import com.halahasneen.theguest.data.model.NormalizedRect
import com.halahasneen.theguest.data.model.PlayerState
import com.halahasneen.theguest.engine.CollisionSystem
import com.halahasneen.theguest.engine.GameLoop
import kotlin.math.hypot

class GameCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val player = PlayerState()
    private var inputX = 0f
    private var inputY = 0f

    private val walkable = NormalizedRect(0.08f, 0.10f, 0.92f, 0.90f)
    private val obstacles = listOf(
        NormalizedRect(0.43f, 0.32f, 0.57f, 0.68f),
        NormalizedRect(0.72f, 0.16f, 0.86f, 0.30f)
    )
    private val collisionSystem = CollisionSystem(walkable, obstacles)

    private val roomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(26, 22, 38) }
    private val obstaclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(59, 75, 107) }
    private val playerBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(234, 234, 234) }
    private val playerHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(232, 176, 75) }
    private val rect = RectF()

    private val gameLoop = GameLoop(
        onUpdate = ::updateGame,
        onRenderRequested = ::postInvalidateOnAnimation
    )

    fun setInputDirection(x: Float, y: Float) {
        val length = hypot(x, y)
        if (length > 1f) {
            inputX = x / length
            inputY = y / length
        } else {
            inputX = x
            inputY = y
        }
    }

    fun resumeGame() = gameLoop.start()
    fun pauseGame() = gameLoop.stop()

    private fun updateGame(deltaSeconds: Float) {
        val desired = NormalizedPoint(
            player.position.x + inputX * player.speedPerSecond * deltaSeconds,
            player.position.y + inputY * player.speedPerSecond * deltaSeconds
        )
        player.position = collisionSystem.resolve(player.position, desired, player.radius)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(13, 11, 20))
        drawNormalizedRect(canvas, walkable, roomPaint)
        obstacles.forEach { drawNormalizedRect(canvas, it, obstaclePaint) }
        drawPlayer(canvas)
    }

    private fun drawNormalizedRect(canvas: Canvas, area: NormalizedRect, paint: Paint) {
        rect.set(area.left * width, area.top * height, area.right * width, area.bottom * height)
        canvas.drawRoundRect(rect, 18f, 18f, paint)
    }

    private fun drawPlayer(canvas: Canvas) {
        val x = player.position.x * width
        val y = player.position.y * height
        val unit = minOf(width, height)
        val bodyWidth = unit * 0.035f
        val bodyHeight = unit * 0.07f
        rect.set(x - bodyWidth, y - bodyHeight * 0.2f, x + bodyWidth, y + bodyHeight)
        canvas.drawRoundRect(rect, bodyWidth, bodyWidth, playerBodyPaint)
        canvas.drawCircle(x, y - bodyHeight * 0.48f, bodyWidth * 0.78f, playerHeadPaint)
    }
}
