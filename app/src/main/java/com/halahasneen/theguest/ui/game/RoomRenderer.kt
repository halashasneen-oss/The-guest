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

/**
 * Draws the complete offline world using reusable Canvas primitives only.
 * The art direction is deliberately flat-vector horror, but every room has a
 * readable identity, furniture silhouettes, environmental storytelling and
 * low-cost decorative detail. No bitmaps are allocated during rendering.
 */
class RoomRenderer {
    private val backgroundPaint = fill(13, 11, 20)
    private val floorPaint = fill(24, 20, 31)
    private val floorAltPaint = fill(29, 24, 36)
    private val wallPaint = fill(19, 16, 26)
    private val wallTrimPaint = fill(42, 35, 49)
    private val woodPaint = fill(73, 53, 47)
    private val woodDarkPaint = fill(48, 36, 35)
    private val upholsteryPaint = fill(52, 49, 64)
    private val upholsteryLightPaint = fill(67, 63, 79)
    private val metalPaint = fill(72, 82, 98)
    private val metalDarkPaint = fill(41, 47, 59)
    private val paperPaint = fill(203, 186, 147)
    private val warmPaint = fill(232, 176, 75)
    private val coldPaint = fill(59, 75, 107)
    private val dangerPaint = fill(139, 30, 63)
    private val shadowPaint = fill(5, 4, 8).apply { alpha = 150 }
    private val playerPaint = fill(226, 224, 219)
    private val skinPaint = fill(181, 139, 91)
    private val glassPaint = fill(78, 94, 112).apply { alpha = 130 }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.rgb(59, 75, 107)
    }
    private val fineEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.rgb(91, 84, 104)
        alpha = 170
    }
    private val floorLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.rgb(52, 44, 60)
        alpha = 125
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(234, 234, 234)
        textAlign = Paint.Align.CENTER
    }
    private val dimTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(234, 234, 234)
        alpha = 175
        textAlign = Paint.Align.CENTER
    }
    private val hudPaint = fill(8, 7, 12).apply { alpha = 195 }
    private val endingPaint = fill(7, 6, 11).apply { alpha = 235 }

    private val rect = RectF()
    private val path = Path()

    fun drawWorld(
        canvas: Canvas,
        width: Int,
        height: Int,
        room: RoomData,
        roomStateManager: RoomStateManager,
        hiddenHotspots: Set<String>,
        shadowSeconds: Float,
        basementPresenceSeconds: Float
    ) {
        canvas.drawPaint(backgroundPaint)
        drawRoomShell(canvas, width, height, room)

        when (room.id) {
            RoomId.ENTRANCE -> drawEntrance(canvas, width, height, room, hiddenHotspots)
            RoomId.LIVING_ROOM -> drawLiving(canvas, width, height, room, roomStateManager, hiddenHotspots)
            RoomId.KITCHEN -> drawKitchen(canvas, width, height, room, roomStateManager, hiddenHotspots)
            RoomId.BEDROOM -> drawBedroom(canvas, width, height, room, roomStateManager, hiddenHotspots, shadowSeconds)
            RoomId.BASEMENT -> drawBasement(canvas, width, height, room, hiddenHotspots, basementPresenceSeconds)
        }

        drawDoors(canvas, width, height, room)
        drawAmbientCorners(canvas, width, height)
    }

    fun drawPlayer(canvas: Canvas, width: Int, height: Int, player: PlayerState) {
        val x = player.position.x * width
        val y = player.position.y * height
        val unit = minOf(width, height).toFloat()
        val bodyW = unit * 0.034f
        val bodyH = unit * 0.076f
        val headR = unit * 0.017f

        shadowPaint.alpha = 95
        rect.set(x - bodyW * 0.90f, y + bodyH * 0.58f, x + bodyW * 0.90f, y + bodyH * 0.92f)
        canvas.drawOval(rect, shadowPaint)
        shadowPaint.alpha = 150

        rect.set(x - bodyW, y - bodyH * 0.02f, x + bodyW, y + bodyH)
        canvas.drawRoundRect(rect, bodyW * 0.52f, bodyW * 0.52f, playerPaint)

        // Coat split and arms make the avatar readable as a person at small scale.
        canvas.drawLine(x, y + bodyH * 0.18f, x, y + bodyH * 0.88f, fineEdgePaint)
        canvas.drawLine(x - bodyW * 0.86f, y + bodyH * 0.22f, x - bodyW * 1.25f, y + bodyH * 0.61f, playerPaint)
        canvas.drawLine(x + bodyW * 0.86f, y + bodyH * 0.22f, x + bodyW * 1.25f, y + bodyH * 0.61f, playerPaint)

        canvas.drawCircle(x, y - bodyH * 0.33f, headR, skinPaint)
        woodDarkPaint.alpha = 235
        rect.set(x - headR, y - bodyH * 0.49f, x + headR, y - bodyH * 0.32f)
        canvas.drawArc(rect, 180f, 180f, true, woodDarkPaint)
        woodDarkPaint.alpha = 255

        // Small warm chest light ties the player to the LightingSystem halo.
        warmPaint.alpha = 175
        canvas.drawCircle(x, y + bodyH * 0.18f, unit * 0.006f, warmPaint)
        warmPaint.alpha = 255
    }

    fun drawHud(
        canvas: Canvas,
        width: Int,
        height: Int,
        roomName: String,
        message: String?,
        prompt: String?,
        memories: Int
    ) {
        val unit = minOf(width, height).toFloat()
        rect.set(width * 0.34f, height * 0.015f, width * 0.66f, height * 0.09f)
        canvas.drawRoundRect(rect, unit * 0.018f, unit * 0.018f, hudPaint)

        rect.set(width * 0.80f, height * 0.015f, width * 0.985f, height * 0.09f)
        canvas.drawRoundRect(rect, unit * 0.018f, unit * 0.018f, hudPaint)

        dimTextPaint.textSize = unit * 0.027f
        canvas.drawText(roomName, width * 0.50f, height * 0.064f, dimTextPaint)
        canvas.drawText("الذكريات  $memories/5", width * 0.892f, height * 0.064f, dimTextPaint)

        val line = message ?: prompt
        if (line != null) {
            rect.set(width * 0.20f, height * 0.865f, width * 0.80f, height * 0.975f)
            canvas.drawRoundRect(rect, unit * 0.024f, unit * 0.024f, hudPaint)
            textPaint.textSize = if (message != null) unit * 0.029f else unit * 0.026f
            canvas.drawText(line, width * 0.50f, height * 0.932f, if (message != null) textPaint else dimTextPaint)
        }
    }

    fun drawEnding(canvas: Canvas, width: Int, height: Int, endingPath: EndingPath) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), endingPaint)
        val unit = minOf(width, height).toFloat()
        textPaint.textSize = unit * 0.072f
        dimTextPaint.textSize = unit * 0.031f

        dangerPaint.alpha = 70
        canvas.drawCircle(width * 0.50f, height * 0.43f, unit * 0.23f, dangerPaint)
        dangerPaint.alpha = 255

        val title: String
        val first: String
        val second: String
        if (endingPath == EndingPath.TRUTH) {
            title = "الحقيقة"
            first = "البيت لم يكن يخفي زائرًا عنك."
            second = "أنت من عاد إلى مكان لم يعد يعتبرك واحدًا منه."
        } else {
            title = "الإنكار"
            first = "تركت القبو خلفك، لكن البيت لم يتركك."
            second = "بعض الأبواب تبقى مفتوحة عندما نرفض النظر خلفها."
        }
        canvas.drawText(title, width * 0.5f, height * 0.40f, textPaint)
        canvas.drawText(first, width * 0.5f, height * 0.52f, dimTextPaint)
        canvas.drawText(second, width * 0.5f, height * 0.59f, dimTextPaint)
    }

    private fun drawRoomShell(canvas: Canvas, width: Int, height: Int, room: RoomData) {
        drawNRect(canvas, width, height, room.walkableBounds.left, room.walkableBounds.top, room.walkableBounds.right, room.walkableBounds.bottom, floorPaint, 10f)

        // Thick room border, top wall and skirting.
        drawNRect(canvas, width, height, room.walkableBounds.left, room.walkableBounds.top, room.walkableBounds.right, room.walkableBounds.top + 0.055f, wallPaint, 0f)
        drawNRect(canvas, width, height, room.walkableBounds.left, room.walkableBounds.bottom - 0.042f, room.walkableBounds.right, room.walkableBounds.bottom, wallPaint, 0f)
        drawNRect(canvas, width, height, room.walkableBounds.left, room.walkableBounds.top, room.walkableBounds.left + 0.018f, room.walkableBounds.bottom, wallPaint, 0f)
        drawNRect(canvas, width, height, room.walkableBounds.right - 0.018f, room.walkableBounds.top, room.walkableBounds.right, room.walkableBounds.bottom, wallPaint, 0f)
        drawNRect(canvas, width, height, room.walkableBounds.left + 0.018f, room.walkableBounds.top + 0.055f, room.walkableBounds.right - 0.018f, room.walkableBounds.top + 0.068f, wallTrimPaint, 0f)

        when (room.id) {
            RoomId.KITCHEN -> drawKitchenTiles(canvas, width, height)
            RoomId.BASEMENT -> drawBasementFloor(canvas, width, height)
            else -> drawFloorBoards(canvas, width, height)
        }
    }

    private fun drawFloorBoards(canvas: Canvas, width: Int, height: Int) {
        var y = 0.19f
        while (y < 0.88f) {
            canvas.drawLine(width * 0.09f, height * y, width * 0.91f, height * y, floorLinePaint)
            y += 0.085f
        }
        var x = 0.16f
        var offset = false
        while (x < 0.91f) {
            val top = if (offset) 0.19f else 0.145f
            canvas.drawLine(width * x, height * top, width * x, height * 0.88f, floorLinePaint)
            x += 0.12f
            offset = !offset
        }
    }

    private fun drawKitchenTiles(canvas: Canvas, width: Int, height: Int) {
        var x = 0.10f
        while (x < 0.92f) {
            canvas.drawLine(width * x, height * 0.16f, width * x, height * 0.86f, floorLinePaint)
            x += 0.09f
        }
        var y = 0.16f
        while (y < 0.88f) {
            canvas.drawLine(width * 0.09f, height * y, width * 0.91f, height * y, floorLinePaint)
            y += 0.10f
        }
    }

    private fun drawBasementFloor(canvas: Canvas, width: Int, height: Int) {
        floorAltPaint.alpha = 125
        drawNRect(canvas, width, height, 0.09f, 0.16f, 0.91f, 0.88f, floorAltPaint, 0f)
        floorAltPaint.alpha = 255

        // Long cracks, deliberately asymmetrical.
        floorLinePaint.alpha = 165
        path.reset()
        path.moveTo(width * 0.18f, height * 0.34f)
        path.lineTo(width * 0.30f, height * 0.42f)
        path.lineTo(width * 0.26f, height * 0.54f)
        path.lineTo(width * 0.39f, height * 0.61f)
        canvas.drawPath(path, floorLinePaint)
        path.reset()
        path.moveTo(width * 0.68f, height * 0.21f)
        path.lineTo(width * 0.63f, height * 0.39f)
        path.lineTo(width * 0.75f, height * 0.51f)
        path.lineTo(width * 0.70f, height * 0.78f)
        canvas.drawPath(path, floorLinePaint)
        floorLinePaint.alpha = 125
    }

    private fun drawDoors(canvas: Canvas, width: Int, height: Int, room: RoomData) {
        room.hotspots.asSequence().filter { it.type == HotspotType.DOOR }.forEach { door ->
            val x = door.position.x
            val y = door.position.y
            when {
                x < 0.20f -> drawSideDoor(canvas, width, height, 0.075f, y, true)
                x > 0.80f -> drawSideDoor(canvas, width, height, 0.925f, y, false)
                else -> drawHorizontalDoor(canvas, width, height, x, y)
            }
        }
    }

    private fun drawSideDoor(canvas: Canvas, width: Int, height: Int, x: Float, y: Float, opensRight: Boolean) {
        val left = if (opensRight) x else x - 0.055f
        val right = if (opensRight) x + 0.055f else x
        drawNRect(canvas, width, height, left - 0.008f, y - 0.145f, right + 0.008f, y + 0.145f, woodDarkPaint, 3f)
        drawNRect(canvas, width, height, left, y - 0.132f, right, y + 0.132f, woodPaint, 3f)
        drawNRectOutline(canvas, width, height, left + 0.008f, y - 0.115f, right - 0.008f, y - 0.015f, fineEdgePaint, 2f)
        drawNRectOutline(canvas, width, height, left + 0.008f, y + 0.020f, right - 0.008f, y + 0.113f, fineEdgePaint, 2f)
        warmPaint.alpha = 205
        canvas.drawCircle(width * (if (opensRight) right - 0.010f else left + 0.010f), height * y, minOf(width, height) * 0.006f, warmPaint)
        warmPaint.alpha = 255
    }

    private fun drawHorizontalDoor(canvas: Canvas, width: Int, height: Int, x: Float, y: Float) {
        drawNRect(canvas, width, height, x - 0.11f, y - 0.045f, x + 0.11f, y + 0.045f, woodDarkPaint, 3f)
        drawNRect(canvas, width, height, x - 0.095f, y - 0.035f, x + 0.095f, y + 0.035f, woodPaint, 2f)
    }

    private fun drawEntrance(canvas: Canvas, width: Int, height: Int, room: RoomData, hidden: Set<String>) {
        // Long worn runner rug.
        dangerPaint.alpha = 55
        drawNRect(canvas, width, height, 0.36f, 0.39f, 0.72f, 0.66f, dangerPaint, 18f)
        dangerPaint.alpha = 255
        drawNRectOutline(canvas, width, height, 0.38f, 0.41f, 0.70f, 0.64f, fineEdgePaint, 14f)

        // Left shoe bench aligned to collision geometry.
        drawNRect(canvas, width, height, 0.15f, 0.60f, 0.34f, 0.70f, upholsteryPaint, 9f)
        drawNRect(canvas, width, height, 0.17f, 0.70f, 0.19f, 0.77f, woodDarkPaint, 2f)
        drawNRect(canvas, width, height, 0.30f, 0.70f, 0.32f, 0.77f, woodDarkPaint, 2f)
        drawNRect(canvas, width, height, 0.17f, 0.625f, 0.32f, 0.645f, upholsteryLightPaint, 4f)

        // Console table and mirror on the far wall.
        drawNRect(canvas, width, height, 0.67f, 0.18f, 0.82f, 0.27f, woodPaint, 5f)
        drawNRect(canvas, width, height, 0.69f, 0.27f, 0.71f, 0.36f, woodDarkPaint, 2f)
        drawNRect(canvas, width, height, 0.78f, 0.27f, 0.80f, 0.36f, woodDarkPaint, 2f)
        drawNRect(canvas, width, height, 0.70f, 0.105f, 0.79f, 0.17f, metalDarkPaint, 6f)
        glassPaint.alpha = 90
        drawNRect(canvas, width, height, 0.712f, 0.115f, 0.778f, 0.158f, glassPaint, 4f)
        glassPaint.alpha = 130

        // Coat rack silhouette.
        canvas.drawLine(width * 0.865f, height * 0.22f, width * 0.865f, height * 0.46f, woodPaint)
        canvas.drawLine(width * 0.835f, height * 0.27f, width * 0.895f, height * 0.27f, woodPaint)
        canvas.drawLine(width * 0.845f, height * 0.46f, width * 0.885f, height * 0.46f, woodPaint)
        shadowPaint.alpha = 105
        path.reset()
        path.moveTo(width * 0.84f, height * 0.28f)
        path.lineTo(width * 0.81f, height * 0.42f)
        path.lineTo(width * 0.87f, height * 0.42f)
        path.close()
        canvas.drawPath(path, shadowPaint)
        shadowPaint.alpha = 150

        drawClock(canvas, width, height, 0.20f, 0.24f)

        if ("entrance_key" !in hidden) drawKey(canvas, width, height, 0.25f, 0.55f)
    }

    private fun drawLiving(
        canvas: Canvas,
        width: Int,
        height: Int,
        room: RoomData,
        states: RoomStateManager,
        hidden: Set<String>
    ) {
        // Rug under the sofa/coffee zone.
        coldPaint.alpha = 35
        drawNRect(canvas, width, height, 0.29f, 0.30f, 0.69f, 0.72f, coldPaint, 22f)
        coldPaint.alpha = 255
        drawNRectOutline(canvas, width, height, 0.31f, 0.32f, 0.67f, 0.70f, fineEdgePaint, 20f)

        // Sofa matching the central collision block.
        drawNRect(canvas, width, height, 0.37f, 0.39f, 0.63f, 0.60f, upholsteryPaint, 16f)
        drawNRect(canvas, width, height, 0.39f, 0.405f, 0.49f, 0.48f, upholsteryLightPaint, 11f)
        drawNRect(canvas, width, height, 0.51f, 0.405f, 0.61f, 0.48f, upholsteryLightPaint, 11f)
        drawNRect(canvas, width, height, 0.355f, 0.43f, 0.39f, 0.59f, upholsteryLightPaint, 10f)
        drawNRect(canvas, width, height, 0.61f, 0.43f, 0.645f, 0.59f, upholsteryLightPaint, 10f)

        // Coffee table in front of sofa, visual-only to avoid changing collision rules.
        drawNRect(canvas, width, height, 0.43f, 0.64f, 0.58f, 0.70f, woodPaint, 7f)
        drawNRect(canvas, width, height, 0.45f, 0.70f, 0.47f, 0.755f, woodDarkPaint, 2f)
        drawNRect(canvas, width, height, 0.54f, 0.70f, 0.56f, 0.755f, woodDarkPaint, 2f)
        paperPaint.alpha = 125
        drawNRect(canvas, width, height, 0.485f, 0.655f, 0.525f, 0.685f, paperPaint, 2f)
        paperPaint.alpha = 255

        // Armchair on the top-left collision.
        drawNRect(canvas, width, height, 0.16f, 0.18f, 0.29f, 0.29f, upholsteryPaint, 14f)
        drawNRect(canvas, width, height, 0.175f, 0.185f, 0.275f, 0.225f, upholsteryLightPaint, 10f)

        // Low sideboard and a weak lamp.
        drawNRect(canvas, width, height, 0.72f, 0.59f, 0.86f, 0.69f, woodPaint, 6f)
        canvas.drawLine(width * 0.785f, height * 0.59f, width * 0.785f, height * 0.48f, metalPaint)
        warmPaint.alpha = 55
        canvas.drawCircle(width * 0.785f, height * 0.475f, minOf(width, height) * 0.06f, warmPaint)
        warmPaint.alpha = 255
        path.reset()
        path.moveTo(width * 0.755f, height * 0.50f)
        path.lineTo(width * 0.815f, height * 0.50f)
        path.lineTo(width * 0.797f, height * 0.445f)
        path.lineTo(width * 0.773f, height * 0.445f)
        path.close()
        canvas.drawPath(path, paperPaint)

        drawFamilyPortrait(canvas, width, height, states)
        if ("living_birthday_card" !in hidden) drawPaper(canvas, width, height, 0.79f, 0.55f, true)
    }

    private fun drawFamilyPortrait(canvas: Canvas, width: Int, height: Int, states: RoomStateManager) {
        val centerX = 0.75f * width
        val centerY = 0.27f * height
        val tilted = states.hasPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_PICTURE_TILTED)
        val extraPerson = states.hasPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_EXTRA_PERSON)

        canvas.save()
        if (tilted) canvas.rotate(-7f, centerX, centerY)
        rect.set(0.685f * width, 0.17f * height, 0.815f * width, 0.37f * height)
        canvas.drawRoundRect(rect, 7f, 7f, woodDarkPaint)
        rect.set(0.697f * width, 0.188f * height, 0.803f * width, 0.35f * height)
        canvas.drawRoundRect(rect, 4f, 4f, coldPaint)
        rect.set(0.705f * width, 0.198f * height, 0.795f * width, 0.342f * height)
        canvas.drawRoundRect(rect, 3f, 3f, wallPaint)

        val count = if (extraPerson) 5 else 4
        val spacing = minOf(width, height) * 0.027f
        val startX = centerX - (count - 1) * spacing * 0.5f
        repeat(count) { index ->
            val px = startX + index * spacing
            val isPresence = extraPerson && index == count - 1
            val paint = if (isPresence) dangerPaint else playerPaint
            paint.alpha = if (isPresence) 110 else 170
            canvas.drawCircle(px, centerY - 9f, 5f, paint)
            rect.set(px - 5f, centerY - 2f, px + 5f, centerY + 15f)
            canvas.drawRoundRect(rect, 5f, 5f, paint)
            paint.alpha = 255
        }
        canvas.restore()
    }

    private fun drawKitchen(
        canvas: Canvas,
        width: Int,
        height: Int,
        room: RoomData,
        states: RoomStateManager,
        hidden: Set<String>
    ) {
        // Left counter, drawers and old stove.
        drawNRect(canvas, width, height, 0.16f, 0.17f, 0.38f, 0.30f, woodDarkPaint, 5f)
        drawNRect(canvas, width, height, 0.17f, 0.18f, 0.37f, 0.215f, metalPaint, 3f)
        drawNRect(canvas, width, height, 0.18f, 0.225f, 0.26f, 0.29f, woodPaint, 3f)
        drawNRect(canvas, width, height, 0.275f, 0.225f, 0.36f, 0.29f, woodPaint, 3f)
        repeat(4) { i ->
            canvas.drawCircle(width * (0.195f + i * 0.045f), height * 0.195f, minOf(width, height) * 0.009f, metalDarkPaint)
        }

        // Central table with visible top, legs and one chair.
        drawNRect(canvas, width, height, 0.43f, 0.44f, 0.62f, 0.57f, woodPaint, 7f)
        drawNRect(canvas, width, height, 0.445f, 0.57f, 0.465f, 0.68f, woodDarkPaint, 2f)
        drawNRect(canvas, width, height, 0.585f, 0.57f, 0.605f, 0.68f, woodDarkPaint, 2f)
        drawNRect(canvas, width, height, 0.50f, 0.685f, 0.56f, 0.735f, upholsteryPaint, 5f)
        canvas.drawLine(width * 0.51f, height * 0.73f, width * 0.50f, height * 0.79f, woodDarkPaint)
        canvas.drawLine(width * 0.55f, height * 0.73f, width * 0.56f, height * 0.79f, woodDarkPaint)

        // Sink counter + cabinet doors.
        drawNRect(canvas, width, height, 0.70f, 0.17f, 0.88f, 0.30f, woodDarkPaint, 5f)
        drawNRect(canvas, width, height, 0.71f, 0.18f, 0.87f, 0.215f, metalPaint, 3f)
        drawNRect(canvas, width, height, 0.72f, 0.225f, 0.79f, 0.29f, woodPaint, 3f)
        drawNRect(canvas, width, height, 0.80f, 0.225f, 0.86f, 0.29f, woodPaint, 3f)
        drawSink(canvas, width, height)

        // Hanging cabinets and extractor shadow.
        drawNRect(canvas, width, height, 0.17f, 0.105f, 0.36f, 0.155f, woodDarkPaint, 3f)
        canvas.drawLine(width * 0.265f, height * 0.11f, width * 0.265f, height * 0.15f, fineEdgePaint)
        shadowPaint.alpha = 80
        drawNRect(canvas, width, height, 0.76f, 0.105f, 0.84f, 0.15f, shadowPaint, 2f)
        shadowPaint.alpha = 150

        if ("kitchen_chipped_cup" !in hidden) drawCup(canvas, width, height, 0.72f, 0.57f)

        val moved = states.hasPhysical(RoomId.KITCHEN, RoomStateManager.Flags.KITCHEN_ITEM_MOVED)
        drawJar(canvas, width, height, if (moved) 0.30f else 0.57f, if (moved) 0.72f else 0.73f)

        // One crooked ceiling bulb pool with a weak hanging cord.
        canvas.drawLine(width * 0.50f, height * 0.10f, width * 0.50f, height * 0.175f, metalDarkPaint)
        warmPaint.alpha = 55
        canvas.drawCircle(width * 0.50f, height * 0.19f, minOf(width, height) * 0.05f, warmPaint)
        warmPaint.alpha = 255
        canvas.drawCircle(width * 0.50f, height * 0.18f, minOf(width, height) * 0.009f, warmPaint)
    }

    private fun drawSink(canvas: Canvas, width: Int, height: Int) {
        rect.set(width * 0.755f, height * 0.185f, width * 0.835f, height * 0.225f)
        canvas.drawOval(rect, metalDarkPaint)
        rect.set(width * 0.765f, height * 0.19f, width * 0.825f, height * 0.218f)
        canvas.drawOval(rect, glassPaint)
        canvas.drawLine(width * 0.795f, height * 0.185f, width * 0.795f, height * 0.155f, metalPaint)
        canvas.drawArc(width * 0.785f, height * 0.145f, width * 0.815f, height * 0.175f, 180f, 180f, false, metalPaint)
        coldPaint.alpha = 150
        canvas.drawCircle(width * 0.79f, height * 0.33f, minOf(width, height) * 0.007f, coldPaint)
        coldPaint.alpha = 255
    }

    private fun drawBedroom(
        canvas: Canvas,
        width: Int,
        height: Int,
        room: RoomData,
        states: RoomStateManager,
        hidden: Set<String>,
        shadowSeconds: Float
    ) {
        // Bed occupies the main collision block.
        drawNRect(canvas, width, height, 0.48f, 0.35f, 0.76f, 0.68f, woodDarkPaint, 12f)
        drawNRect(canvas, width, height, 0.50f, 0.37f, 0.74f, 0.66f, upholsteryPaint, 11f)
        drawNRect(canvas, width, height, 0.515f, 0.39f, 0.615f, 0.47f, playerPaint, 9f)
        playerPaint.alpha = 190
        drawNRect(canvas, width, height, 0.625f, 0.39f, 0.725f, 0.47f, playerPaint, 9f)
        playerPaint.alpha = 255
        coldPaint.alpha = 65
        drawNRect(canvas, width, height, 0.50f, 0.50f, 0.74f, 0.65f, coldPaint, 7f)
        coldPaint.alpha = 255
        drawNRect(canvas, width, height, 0.475f, 0.33f, 0.765f, 0.37f, woodPaint, 4f)

        // Wardrobe.
        drawNRect(canvas, width, height, 0.16f, 0.17f, 0.31f, 0.31f, woodDarkPaint, 5f)
        drawNRect(canvas, width, height, 0.17f, 0.18f, 0.235f, 0.30f, woodPaint, 3f)
        drawNRect(canvas, width, height, 0.24f, 0.18f, 0.30f, 0.30f, woodPaint, 3f)
        warmPaint.alpha = 180
        canvas.drawCircle(width * 0.228f, height * 0.24f, minOf(width, height) * 0.0045f, warmPaint)
        canvas.drawCircle(width * 0.248f, height * 0.24f, minOf(width, height) * 0.0045f, warmPaint)
        warmPaint.alpha = 255

        // Writing desk / chest.
        drawNRect(canvas, width, height, 0.18f, 0.67f, 0.31f, 0.76f, woodPaint, 5f)
        drawNRect(canvas, width, height, 0.19f, 0.76f, 0.21f, 0.82f, woodDarkPaint, 2f)
        drawNRect(canvas, width, height, 0.28f, 0.76f, 0.30f, 0.82f, woodDarkPaint, 2f)
        canvas.drawLine(width * 0.245f, height * 0.68f, width * 0.245f, height * 0.75f, fineEdgePaint)

        // Window with weak moonlight.
        drawNRect(canvas, width, height, 0.37f, 0.105f, 0.52f, 0.235f, woodDarkPaint, 4f)
        drawNRect(canvas, width, height, 0.382f, 0.118f, 0.508f, 0.222f, coldPaint, 2f)
        coldPaint.alpha = 80
        drawNRect(canvas, width, height, 0.39f, 0.125f, 0.50f, 0.215f, coldPaint, 2f)
        coldPaint.alpha = 255
        canvas.drawLine(width * 0.445f, height * 0.12f, width * 0.445f, height * 0.22f, fineEdgePaint)
        canvas.drawLine(width * 0.385f, height * 0.17f, width * 0.505f, height * 0.17f, fineEdgePaint)

        val changed = states.hasPhysical(RoomId.BEDROOM, RoomStateManager.Flags.BEDROOM_MESSAGE_CHANGED)
        drawPaper(canvas, width, height, 0.72f, 0.25f, changed)
        if ("bedroom_letter_fragment" !in hidden) drawTornPaper(canvas, width, height, 0.30f, 0.58f)

        if (states.hasPhysical(RoomId.BEDROOM, RoomStateManager.Flags.BEDROOM_DOOR_SHIFTED)) {
            dangerPaint.alpha = 58
            drawNRect(canvas, width, height, 0.82f, 0.60f, 0.90f, 0.84f, dangerPaint, 4f)
            dangerPaint.alpha = 255
        }

        if (shadowSeconds > 0f) {
            val alpha = ((shadowSeconds / 2.2f) * 95f).toInt().coerceIn(25, 95)
            shadowPaint.alpha = alpha
            path.reset()
            path.moveTo(width * 0.955f, height * 0.17f)
            path.lineTo(width * 0.89f, height * 0.47f)
            path.lineTo(width * 0.945f, height * 0.82f)
            path.lineTo(width * 0.985f, height * 0.82f)
            path.lineTo(width * 0.985f, height * 0.17f)
            path.close()
            canvas.drawPath(path, shadowPaint)
            shadowPaint.alpha = 150
        }
    }

    private fun drawBasement(
        canvas: Canvas,
        width: Int,
        height: Int,
        room: RoomData,
        hidden: Set<String>,
        presenceSeconds: Float
    ) {
        // Pipes along the top wall.
        metalDarkPaint.alpha = 230
        canvas.drawLine(width * 0.12f, height * 0.19f, width * 0.88f, height * 0.19f, metalDarkPaint)
        canvas.drawLine(width * 0.12f, height * 0.215f, width * 0.70f, height * 0.215f, metalPaint)
        canvas.drawLine(width * 0.69f, height * 0.215f, width * 0.69f, height * 0.36f, metalPaint)
        metalDarkPaint.alpha = 255

        // Crates on left collision.
        drawNRect(canvas, width, height, 0.15f, 0.17f, 0.35f, 0.30f, woodDarkPaint, 4f)
        drawNRectOutline(canvas, width, height, 0.16f, 0.18f, 0.25f, 0.29f, fineEdgePaint, 2f)
        drawNRectOutline(canvas, width, height, 0.255f, 0.18f, 0.34f, 0.29f, fineEdgePaint, 2f)
        canvas.drawLine(width * 0.17f, height * 0.19f, width * 0.24f, height * 0.28f, fineEdgePaint)
        canvas.drawLine(width * 0.33f, height * 0.19f, width * 0.265f, height * 0.28f, fineEdgePaint)

        // Boiler / furnace central block.
        drawNRect(canvas, width, height, 0.42f, 0.40f, 0.60f, 0.64f, metalDarkPaint, 15f)
        drawNRect(canvas, width, height, 0.445f, 0.43f, 0.575f, 0.54f, metalPaint, 8f)
        canvas.drawCircle(width * 0.51f, height * 0.485f, minOf(width, height) * 0.035f, wallPaint)
        dangerPaint.alpha = 95
        canvas.drawCircle(width * 0.51f, height * 0.485f, minOf(width, height) * 0.016f, dangerPaint)
        dangerPaint.alpha = 255
        drawNRect(canvas, width, height, 0.455f, 0.56f, 0.565f, 0.62f, wallPaint, 5f)
        repeat(4) { i ->
            canvas.drawLine(width * (0.47f + i * 0.025f), height * 0.575f, width * (0.47f + i * 0.025f), height * 0.605f, fineEdgePaint)
        }

        // Metal shelving on right collision.
        drawNRect(canvas, width, height, 0.70f, 0.18f, 0.86f, 0.33f, metalDarkPaint, 3f)
        canvas.drawLine(width * 0.71f, height * 0.225f, width * 0.85f, height * 0.225f, metalPaint)
        canvas.drawLine(width * 0.71f, height * 0.275f, width * 0.85f, height * 0.275f, metalPaint)
        drawNRect(canvas, width, height, 0.72f, 0.19f, 0.76f, 0.22f, woodPaint, 2f)
        drawNRect(canvas, width, height, 0.79f, 0.235f, 0.84f, 0.27f, woodPaint, 2f)

        // Family box near inspect hotspot.
        drawNRect(canvas, width, height, 0.19f, 0.31f, 0.33f, 0.40f, woodPaint, 5f)
        drawNRectOutline(canvas, width, height, 0.205f, 0.325f, 0.315f, 0.385f, fineEdgePaint, 3f)
        warmPaint.alpha = 140
        drawNRect(canvas, width, height, 0.248f, 0.34f, 0.272f, 0.365f, warmPaint, 2f)
        warmPaint.alpha = 255

        // Dramatic stain/presence zone, still subtle and ambiguous.
        dangerPaint.alpha = 35
        canvas.drawCircle(width * 0.52f, height * 0.47f, minOf(width, height) * 0.20f, dangerPaint)
        dangerPaint.alpha = 255

        if ("basement_tape" !in hidden) drawTape(canvas, width, height, 0.53f, 0.70f)

        if (presenceSeconds > 0f) {
            val alpha = ((presenceSeconds / 2.6f) * 105f).toInt().coerceIn(30, 105)
            shadowPaint.alpha = alpha
            path.reset()
            path.moveTo(width * 0.50f, height * 0.145f)
            path.cubicTo(width * 0.44f, height * 0.25f, width * 0.43f, height * 0.56f, width * 0.42f, height * 0.82f)
            path.lineTo(width * 0.58f, height * 0.82f)
            path.cubicTo(width * 0.57f, height * 0.56f, width * 0.56f, height * 0.25f, width * 0.50f, height * 0.145f)
            path.close()
            canvas.drawPath(path, shadowPaint)
            shadowPaint.alpha = 150
        }
    }

    private fun drawClock(canvas: Canvas, width: Int, height: Int, xNorm: Float, yNorm: Float) {
        val x = xNorm * width
        val y = yNorm * height
        val r = minOf(width, height) * 0.047f
        canvas.drawCircle(x, y, r, woodDarkPaint)
        canvas.drawCircle(x, y, r * 0.88f, wallPaint)
        canvas.drawCircle(x, y, r * 0.76f, floorAltPaint)
        warmPaint.alpha = 190
        canvas.drawCircle(x, y, r * 0.06f, warmPaint)
        canvas.drawLine(x, y, x, y - r * 0.47f, warmPaint)
        canvas.drawLine(x, y, x + r * 0.38f, y + r * 0.18f, warmPaint)
        warmPaint.alpha = 255
    }

    private fun drawKey(canvas: Canvas, width: Int, height: Int, xNorm: Float, yNorm: Float) {
        val x = xNorm * width
        val y = yNorm * height
        val unit = minOf(width, height).toFloat()
        warmPaint.alpha = 230
        canvas.drawCircle(x, y, unit * 0.012f, warmPaint)
        canvas.drawCircle(x, y, unit * 0.006f, floorPaint)
        canvas.drawRect(x + unit * 0.010f, y - unit * 0.003f, x + unit * 0.055f, y + unit * 0.003f, warmPaint)
        canvas.drawRect(x + unit * 0.042f, y, x + unit * 0.048f, y + unit * 0.012f, warmPaint)
        canvas.drawRect(x + unit * 0.053f, y, x + unit * 0.059f, y + unit * 0.009f, warmPaint)
        warmPaint.alpha = 255
    }

    private fun drawCup(canvas: Canvas, width: Int, height: Int, xNorm: Float, yNorm: Float) {
        val x = xNorm * width
        val y = yNorm * height
        val unit = minOf(width, height).toFloat()
        rect.set(x - unit * 0.015f, y - unit * 0.022f, x + unit * 0.015f, y + unit * 0.021f)
        canvas.drawRoundRect(rect, unit * 0.008f, unit * 0.008f, paperPaint)
        fineEdgePaint.alpha = 180
        canvas.drawArc(x + unit * 0.006f, y - unit * 0.010f, x + unit * 0.030f, y + unit * 0.015f, -80f, 165f, false, fineEdgePaint)
        canvas.drawLine(x - unit * 0.004f, y - unit * 0.020f, x + unit * 0.003f, y + unit * 0.019f, dangerPaint)
        fineEdgePaint.alpha = 170
    }

    private fun drawJar(canvas: Canvas, width: Int, height: Int, xNorm: Float, yNorm: Float) {
        val x = xNorm * width
        val y = yNorm * height
        val unit = minOf(width, height).toFloat()
        rect.set(x - unit * 0.016f, y - unit * 0.026f, x + unit * 0.016f, y + unit * 0.026f)
        canvas.drawRoundRect(rect, unit * 0.007f, unit * 0.007f, glassPaint)
        canvas.drawRect(x - unit * 0.017f, y - unit * 0.026f, x + unit * 0.017f, y - unit * 0.019f, metalPaint)
        warmPaint.alpha = 85
        canvas.drawCircle(x, y + unit * 0.008f, unit * 0.010f, warmPaint)
        warmPaint.alpha = 255
    }

    private fun drawPaper(canvas: Canvas, width: Int, height: Int, xNorm: Float, yNorm: Float, alarming: Boolean) {
        val x = xNorm * width
        val y = yNorm * height
        val unit = minOf(width, height).toFloat()
        rect.set(x - unit * 0.030f, y - unit * 0.018f, x + unit * 0.030f, y + unit * 0.018f)
        canvas.drawRoundRect(rect, unit * 0.005f, unit * 0.005f, if (alarming) dangerPaint else paperPaint)
        val linePaint = if (alarming) playerPaint else woodDarkPaint
        linePaint.alpha = 115
        repeat(3) { i ->
            canvas.drawLine(x - unit * 0.020f, y - unit * (0.009f - i * 0.008f), x + unit * 0.018f, y - unit * (0.009f - i * 0.008f), linePaint)
        }
        linePaint.alpha = 255
    }

    private fun drawTornPaper(canvas: Canvas, width: Int, height: Int, xNorm: Float, yNorm: Float) {
        val x = xNorm * width
        val y = yNorm * height
        val u = minOf(width, height).toFloat()
        path.reset()
        path.moveTo(x - u * 0.030f, y - u * 0.020f)
        path.lineTo(x + u * 0.025f, y - u * 0.018f)
        path.lineTo(x + u * 0.030f, y + u * 0.007f)
        path.lineTo(x + u * 0.018f, y + u * 0.020f)
        path.lineTo(x + u * 0.008f, y + u * 0.014f)
        path.lineTo(x - u * 0.004f, y + u * 0.022f)
        path.lineTo(x - u * 0.030f, y + u * 0.015f)
        path.close()
        canvas.drawPath(path, paperPaint)
    }

    private fun drawTape(canvas: Canvas, width: Int, height: Int, xNorm: Float, yNorm: Float) {
        val x = xNorm * width
        val y = yNorm * height
        val u = minOf(width, height).toFloat()
        rect.set(x - u * 0.034f, y - u * 0.021f, x + u * 0.034f, y + u * 0.021f)
        canvas.drawRoundRect(rect, u * 0.006f, u * 0.006f, metalDarkPaint)
        drawNRect(canvas, width, height, xNorm - 0.020f, yNorm - 0.010f, xNorm + 0.020f, yNorm + 0.010f, coldPaint, 3f)
        canvas.drawCircle(x - u * 0.014f, y, u * 0.009f, wallPaint)
        canvas.drawCircle(x + u * 0.014f, y, u * 0.009f, wallPaint)
        canvas.drawCircle(x - u * 0.014f, y, u * 0.004f, metalPaint)
        canvas.drawCircle(x + u * 0.014f, y, u * 0.004f, metalPaint)
    }

    private fun drawAmbientCorners(canvas: Canvas, width: Int, height: Int) {
        shadowPaint.alpha = 45
        path.reset()
        path.moveTo(width * 0.07f, height * 0.10f)
        path.lineTo(width * 0.25f, height * 0.10f)
        path.lineTo(width * 0.07f, height * 0.30f)
        path.close()
        canvas.drawPath(path, shadowPaint)
        path.reset()
        path.moveTo(width * 0.93f, height * 0.90f)
        path.lineTo(width * 0.75f, height * 0.90f)
        path.lineTo(width * 0.93f, height * 0.70f)
        path.close()
        canvas.drawPath(path, shadowPaint)
        shadowPaint.alpha = 150
    }

    private fun fill(r: Int, g: Int, b: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(r, g, b)
    }

    private fun drawNRect(
        canvas: Canvas,
        width: Int,
        height: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint,
        radius: Float
    ) {
        rect.set(left * width, top * height, right * width, bottom * height)
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    private fun drawNRectOutline(
        canvas: Canvas,
        width: Int,
        height: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint,
        radius: Float
    ) {
        rect.set(left * width, top * height, right * width, bottom * height)
        canvas.drawRoundRect(rect, radius, radius, paint)
    }
}
