package com.halahasneen.theguest.ui.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.halahasneen.theguest.data.model.EndingPath
import com.halahasneen.theguest.data.model.HotspotType
import com.halahasneen.theguest.data.model.PlayerState
import com.halahasneen.theguest.data.model.RoomData
import com.halahasneen.theguest.data.model.RoomId
import com.halahasneen.theguest.engine.RoomStateManager

class RoomRenderer {
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(13, 11, 20) }
    private val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(26, 22, 38) }
    private val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20, 17, 29) }
    private val furniturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(45, 40, 57) }
    private val furnitureDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(31, 27, 42) }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.rgb(59, 75, 107) }
    private val warmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(232, 176, 75) }
    private val coldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(59, 75, 107) }
    private val dangerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(139, 30, 63) }
    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(234, 234, 234) }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(234, 234, 234); textAlign = Paint.Align.CENTER }
    private val dimTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(234, 234, 234); alpha = 170; textAlign = Paint.Align.CENTER }
    private val endingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(232, 7, 6, 11) }
    private val rect = RectF()
    private val path = Path()

    fun drawWorld(canvas: Canvas, width: Int, height: Int, room: RoomData, roomStateManager: RoomStateManager, hiddenHotspots: Set<String>, shadowSeconds: Float, basementPresenceSeconds: Float) {
        canvas.drawPaint(backgroundPaint)
        drawNormalizedRect(canvas, width, height, room.walkableBounds.left, room.walkableBounds.top, room.walkableBounds.right, room.walkableBounds.bottom, floorPaint, 20f)
        drawNormalizedRect(canvas, width, height, room.walkableBounds.left, room.walkableBounds.top, room.walkableBounds.right, room.walkableBounds.top + 0.045f, wallPaint, 0f)
        drawNormalizedRect(canvas, width, height, room.walkableBounds.left, room.walkableBounds.bottom - 0.045f, room.walkableBounds.right, room.walkableBounds.bottom, wallPaint, 0f)
        room.collisionRects.forEach { obstacle ->
            drawNormalizedRect(canvas, width, height, obstacle.left, obstacle.top, obstacle.right, obstacle.bottom, furniturePaint, 12f)
            drawNormalizedRectOutline(canvas, width, height, obstacle.left, obstacle.top, obstacle.right, obstacle.bottom, edgePaint, 12f)
        }
        drawDoors(canvas, width, height, room)
        when (room.id) {
            RoomId.ENTRANCE -> drawEntrance(canvas, width, height, hiddenHotspots)
            RoomId.LIVING_ROOM -> drawLiving(canvas, width, height, roomStateManager, hiddenHotspots)
            RoomId.KITCHEN -> drawKitchen(canvas, width, height, roomStateManager, hiddenHotspots)
            RoomId.BEDROOM -> drawBedroom(canvas, width, height, roomStateManager, hiddenHotspots, shadowSeconds)
            RoomId.BASEMENT -> drawBasement(canvas, width, height, hiddenHotspots, basementPresenceSeconds)
        }
    }

    fun drawPlayer(canvas: Canvas, width: Int, height: Int, player: PlayerState) {
        val x = player.position.x * width
        val y = player.position.y * height
        val unit = minOf(width, height).toFloat()
        val halfWidth = unit * 0.027f
        val bodyHeight = unit * 0.071f
        rect.set(x - halfWidth, y - bodyHeight * 0.15f, x + halfWidth, y + bodyHeight)
        canvas.drawRoundRect(rect, halfWidth, halfWidth, playerPaint)
        canvas.drawCircle(x, y - bodyHeight * 0.44f, halfWidth * 0.80f, warmPaint)
    }

    fun drawHud(canvas: Canvas, width: Int, height: Int, roomName: String, message: String?, prompt: String?, memories: Int) {
        val unit = minOf(width, height).toFloat()
        textPaint.textSize = unit * 0.034f
        dimTextPaint.textSize = unit * 0.026f
        canvas.drawText(roomName, width * 0.5f, height * 0.065f, dimTextPaint)
        canvas.drawText("الذكريات $memories/5", width * 0.90f, height * 0.065f, dimTextPaint)
        if (message != null) canvas.drawText(message, width * 0.5f, height * 0.935f, textPaint)
        else if (prompt != null) canvas.drawText(prompt, width * 0.5f, height * 0.91f, dimTextPaint)
    }

    fun drawEnding(canvas: Canvas, width: Int, height: Int, endingPath: EndingPath) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), endingPaint)
        val unit = minOf(width, height).toFloat()
        textPaint.textSize = unit * 0.072f
        dimTextPaint.textSize = unit * 0.032f
        val title: String
        val first: String
        val second: String
        if (endingPath == EndingPath.TRUTH) {
            title = "الحقيقة"; first = "البيت لم يكن يخفي زائرًا عنك."; second = "أنت من عاد إلى مكان لم يعد يعتبرك واحدًا منه."
        } else {
            title = "الإنكار"; first = "تركت القبو خلفك، لكن البيت لم يتركك."; second = "بعض الأبواب تبقى مفتوحة عندما نرفض النظر خلفها."
        }
        canvas.drawText(title, width * 0.5f, height * 0.40f, textPaint)
        canvas.drawText(first, width * 0.5f, height * 0.52f, dimTextPaint)
        canvas.drawText(second, width * 0.5f, height * 0.59f, dimTextPaint)
    }

    private fun drawDoors(canvas: Canvas, width: Int, height: Int, room: RoomData) {
        room.hotspots.asSequence().filter { it.type == HotspotType.DOOR }.forEach { door ->
            val x = door.position.x; val y = door.position.y
            when {
                x < 0.20f -> drawNormalizedRect(canvas, width, height, 0.075f, y - 0.12f, 0.145f, y + 0.12f, wallPaint, 6f)
                x > 0.80f -> drawNormalizedRect(canvas, width, height, 0.855f, y - 0.12f, 0.925f, y + 0.12f, wallPaint, 6f)
                else -> drawNormalizedRect(canvas, width, height, x - 0.10f, y - 0.04f, x + 0.10f, y + 0.04f, wallPaint, 6f)
            }
        }
    }

    private fun drawEntrance(canvas: Canvas, width: Int, height: Int, hidden: Set<String>) {
        val x = 0.20f * width; val y = 0.24f * height; val radius = minOf(width, height) * 0.043f
        canvas.drawCircle(x, y, radius, furnitureDarkPaint); canvas.drawCircle(x, y, radius, edgePaint)
        canvas.drawLine(x, y, x, y - radius * 0.48f, warmPaint); canvas.drawLine(x, y, x + radius * 0.36f, y + radius * 0.16f, warmPaint)
        if ("entrance_key" !in hidden) { val keyX = 0.25f * width; val keyY = 0.55f * height; canvas.drawCircle(keyX, keyY, minOf(width, height) * 0.011f, warmPaint); canvas.drawRect(keyX, keyY - 2f, keyX + minOf(width, height) * 0.045f, keyY + 2f, warmPaint) }
    }

    private fun drawLiving(canvas: Canvas, width: Int, height: Int, states: RoomStateManager, hidden: Set<String>) {
        val centerX = 0.75f * width; val centerY = 0.27f * height
        val tilted = states.hasPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_PICTURE_TILTED)
        val extraPerson = states.hasPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_EXTRA_PERSON)
        canvas.save(); if (tilted) canvas.rotate(-7f, centerX, centerY)
        rect.set(0.69f * width, 0.18f * height, 0.81f * width, 0.36f * height); canvas.drawRoundRect(rect, 5f, 5f, coldPaint)
        rect.set(0.704f * width, 0.20f * height, 0.796f * width, 0.34f * height); canvas.drawRoundRect(rect, 3f, 3f, wallPaint)
        val count = if (extraPerson) 5 else 4; val startX = centerX - (count - 1) * 11f
        repeat(count) { index -> val px = startX + index * 22f; val paint = if (extraPerson && index == count - 1) dangerPaint else playerPaint; paint.alpha = if (extraPerson && index == count - 1) 135 else 180; canvas.drawCircle(px, centerY - 8f, 5f, paint); rect.set(px - 5f, centerY - 2f, px + 5f, centerY + 14f); canvas.drawRoundRect(rect, 5f, 5f, paint); paint.alpha = 255 }
        canvas.restore(); if ("living_birthday_card" !in hidden) drawSmallPaper(canvas, width, height, 0.79f, 0.55f)
    }

    private fun drawKitchen(canvas: Canvas, width: Int, height: Int, states: RoomStateManager, hidden: Set<String>) {
        coldPaint.alpha = 80; canvas.drawCircle(0.79f * width, 0.33f * height, minOf(width, height) * 0.045f, coldPaint); coldPaint.alpha = 255
        if ("kitchen_chipped_cup" !in hidden) { val x = 0.72f * width; val y = 0.57f * height; rect.set(x - 9f, y - 11f, x + 9f, y + 10f); canvas.drawRoundRect(rect, 5f, 5f, warmPaint); canvas.drawLine(x + 9f, y - 2f, x + 16f, y + 4f, warmPaint) }
        val moved = states.hasPhysical(RoomId.KITCHEN, RoomStateManager.Flags.KITCHEN_ITEM_MOVED); val jarX = (if (moved) 0.30f else 0.57f) * width; val jarY = (if (moved) 0.72f else 0.73f) * height
        rect.set(jarX - 10f, jarY - 14f, jarX + 10f, jarY + 14f); canvas.drawRoundRect(rect, 5f, 5f, furnitureDarkPaint); canvas.drawLine(jarX - 8f, jarY - 9f, jarX + 8f, jarY - 9f, edgePaint)
    }

    private fun drawBedroom(canvas: Canvas, width: Int, height: Int, states: RoomStateManager, hidden: Set<String>, shadowSeconds: Float) {
        val changed = states.hasPhysical(RoomId.BEDROOM, RoomStateManager.Flags.BEDROOM_MESSAGE_CHANGED); drawSmallPaper(canvas, width, height, 0.72f, 0.25f, if (changed) dangerPaint else warmPaint)
        if ("bedroom_letter_fragment" !in hidden) drawSmallPaper(canvas, width, height, 0.30f, 0.58f)
        if (states.hasPhysical(RoomId.BEDROOM, RoomStateManager.Flags.BEDROOM_DOOR_SHIFTED)) { dangerPaint.alpha = 70; drawNormalizedRect(canvas, width, height, 0.83f, 0.61f, 0.90f, 0.83f, dangerPaint, 4f); dangerPaint.alpha = 255 }
        if (shadowSeconds > 0f) { dangerPaint.alpha = ((shadowSeconds / 2.2f) * 90f).toInt().coerceIn(24, 90); path.reset(); path.moveTo(width * 0.965f, height * 0.18f); path.lineTo(width * 0.90f, height * 0.48f); path.lineTo(width * 0.96f, height * 0.82f); path.close(); canvas.drawPath(path, dangerPaint); dangerPaint.alpha = 255 }
    }

    private fun drawBasement(canvas: Canvas, width: Int, height: Int, hidden: Set<String>, presenceSeconds: Float) {
        dangerPaint.alpha = 55; canvas.drawCircle(width * 0.50f, height * 0.49f, minOf(width, height) * 0.18f, dangerPaint); dangerPaint.alpha = 255
        drawNormalizedRect(canvas, width, height, 0.18f, 0.29f, 0.34f, 0.40f, furnitureDarkPaint, 7f)
        if ("basement_tape" !in hidden) { val x = width * 0.53f; val y = height * 0.70f; rect.set(x - 18f, y - 10f, x + 18f, y + 10f); canvas.drawRoundRect(rect, 4f, 4f, coldPaint); canvas.drawCircle(x - 8f, y, 4f, wallPaint); canvas.drawCircle(x + 8f, y, 4f, wallPaint) }
        if (presenceSeconds > 0f) { dangerPaint.alpha = ((presenceSeconds / 2.6f) * 120f).toInt().coerceIn(35, 120); path.reset(); path.moveTo(width * 0.48f, height * 0.15f); path.lineTo(width * 0.42f, height * 0.82f); path.lineTo(width * 0.58f, height * 0.82f); path.close(); canvas.drawPath(path, dangerPaint); dangerPaint.alpha = 255 }
    }

    private fun drawSmallPaper(canvas: Canvas, width: Int, height: Int, xNorm: Float, yNorm: Float, paint: Paint = warmPaint) { val x = xNorm * width; val y = yNorm * height; rect.set(x - 17f, y - 10f, x + 17f, y + 10f); canvas.drawRoundRect(rect, 4f, 4f, paint) }
    private fun drawNormalizedRect(canvas: Canvas, width: Int, height: Int, left: Float, top: Float, right: Float, bottom: Float, paint: Paint, radius: Float) { rect.set(left * width, top * height, right * width, bottom * height); canvas.drawRoundRect(rect, radius, radius, paint) }
    private fun drawNormalizedRectOutline(canvas: Canvas, width: Int, height: Int, left: Float, top: Float, right: Float, bottom: Float, paint: Paint, radius: Float) { rect.set(left * width, top * height, right * width, bottom * height); canvas.drawRoundRect(rect, radius, radius, paint) }
}
