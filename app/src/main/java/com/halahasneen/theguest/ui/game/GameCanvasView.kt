package com.halahasneen.theguest.ui.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.halahasneen.theguest.data.event.HorrorEventCatalog
import com.halahasneen.theguest.data.model.HorrorAction
import com.halahasneen.theguest.data.model.HorrorContext
import com.halahasneen.theguest.data.model.HorrorEvent
import com.halahasneen.theguest.data.model.Hotspot
import com.halahasneen.theguest.data.model.HotspotType
import com.halahasneen.theguest.data.model.NormalizedPoint
import com.halahasneen.theguest.data.model.NormalizedRect
import com.halahasneen.theguest.data.model.PlayerState
import com.halahasneen.theguest.data.model.RoomData
import com.halahasneen.theguest.data.model.RoomId
import com.halahasneen.theguest.data.room.RoomCatalog
import com.halahasneen.theguest.engine.CollisionSystem
import com.halahasneen.theguest.engine.GameLoop
import com.halahasneen.theguest.engine.HorrorDirector
import com.halahasneen.theguest.engine.InteractionSystem
import com.halahasneen.theguest.engine.LightingSystem
import com.halahasneen.theguest.engine.RoomStateManager
import com.halahasneen.theguest.engine.TensionStage
import com.halahasneen.theguest.engine.TensionSystem
import kotlin.math.hypot

class GameCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onSpatialEffectRequested: ((source: NormalizedPoint, listener: NormalizedPoint) -> Unit)? = null
    var onTensionStageChanged: ((TensionStage) -> Unit)? = null

    private val player = PlayerState(position = NormalizedPoint(0.18f, 0.50f))
    private var inputX = 0f
    private var inputY = 0f

    private var room: RoomData = RoomCatalog.entrance
    private var collisionSystem = CollisionSystem(room.walkableBounds, room.collisionRects)
    private val interactionSystem = InteractionSystem()
    private val lightingSystem = LightingSystem()
    private val tensionSystem = TensionSystem()
    private val horrorDirector = HorrorDirector()
    private val roomStateManager = RoomStateManager()
    private var lastTensionStage = tensionSystem.stage

    private val roomVisitCounts = mutableMapOf(RoomId.ENTRANCE to 1)
    private val collectedMemories = mutableSetOf<String>()
    private val hiddenHotspots = mutableSetOf<String>()
    private var temporaryBlackoutSeconds = 0f
    private var prompt: String? = null
    private var message: String? = "عدت إلى البيت بعد غياب طويل."
    private var messageSeconds = 4f

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(13, 11, 20) }
    private val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(26, 22, 38) }
    private val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20, 17, 29) }
    private val furniturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(45, 40, 57) }
    private val furnitureDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(31, 27, 42) }
    private val furnitureEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.rgb(59, 75, 107)
    }
    private val warmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(232, 176, 75) }
    private val coldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(59, 75, 107) }
    private val dangerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(139, 30, 63) }
    private val playerBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(234, 234, 234) }
    private val playerHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(232, 176, 75) }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(234, 234, 234)
        textAlign = Paint.Align.CENTER
    }
    private val dimTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(234, 234, 234)
        alpha = 165
        textAlign = Paint.Align.CENTER
    }
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

    fun interact() {
        val hotspot = interactionSystem.nearest(player.position, room.hotspots, hiddenHotspots)
        if (hotspot == null) {
            showMessage("لا شيء يلفت الانتباه هنا.", 1.4f)
            return
        }

        when (hotspot.type) {
            HotspotType.INSPECT -> inspect(hotspot)
            HotspotType.COLLECT -> collect(hotspot)
            HotspotType.DOOR -> hotspot.targetRoom?.let { target ->
                onSpatialEffectRequested?.invoke(hotspot.position, player.position)
                if (target == RoomId.LIVING_ROOM) tensionSystem.addStimulus(3f)
                changeRoom(target)
            }
        }
        notifyTensionStageIfNeeded()
    }

    fun resumeGame() = gameLoop.start()
    fun pauseGame() = gameLoop.stop()

    private fun inspect(hotspot: Hotspot) {
        if (hotspot.id == "living_family_picture") {
            inspectFamilyPicture()
            return
        }
        tensionSystem.addStimulus(0.8f)
        showMessage(hotspot.message, 3.6f)
    }

    private fun inspectFamilyPicture() {
        val tilted = roomStateManager.hasPhysical(
            RoomId.LIVING_ROOM,
            RoomStateManager.Flags.LIVING_PICTURE_TILTED
        )
        val extraPerson = roomStateManager.hasPhysical(
            RoomId.LIVING_ROOM,
            RoomStateManager.Flags.LIVING_EXTRA_PERSON
        )

        when {
            extraPerson -> {
                tensionSystem.addStimulus(1.5f)
                showMessage("خمسة أشخاص. لا أستطيع تذكّر وجه الخامس.", 4f)
            }
            tilted -> {
                roomStateManager.markPhysical(
                    RoomId.LIVING_ROOM,
                    RoomStateManager.Flags.LIVING_EXTRA_PERSON
                )
                tensionSystem.addStimulus(6f)
                temporaryBlackoutSeconds = 0.7f
                showMessage("كانوا أربعة... من هذا الخامس؟", 4.5f)
            }
            else -> {
                tensionSystem.addStimulus(1f)
                showMessage("أربعة وجوه مألوفة. هكذا أتذكرها دائمًا.", 3.6f)
            }
        }
    }

    private fun collect(hotspot: Hotspot) {
        hotspot.memoryItemId?.let(collectedMemories::add)
        hiddenHotspots.add(hotspot.id)
        if (hotspot.id == "living_birthday_card") {
            roomStateManager.markPhysical(
                RoomId.LIVING_ROOM,
                RoomStateManager.Flags.LIVING_MEMORY_TAKEN
            )
            tensionSystem.addStimulus(5f)
            showMessage("ذكرى 2/5 — بطاقة عيد قديمة... توقيع خامس مطموس.", 4.5f)
        } else {
            tensionSystem.addStimulus(4f)
            showMessage(hotspot.message, 4f)
        }
    }

    private fun updateGame(deltaSeconds: Float) {
        val desired = NormalizedPoint(
            player.position.x + inputX * player.speedPerSecond * deltaSeconds,
            player.position.y + inputY * player.speedPerSecond * deltaSeconds
        )
        player.position = collisionSystem.resolve(player.position, desired, player.radius)
        prompt = interactionSystem.nearest(player.position, room.hotspots, hiddenHotspots)?.label

        val inSafeLight = isNearSafeLight()
        tensionSystem.update(
            deltaSeconds = deltaSeconds,
            inDarkness = !inSafeLight,
            inSafeLight = inSafeLight
        )
        notifyTensionStageIfNeeded()

        temporaryBlackoutSeconds = (temporaryBlackoutSeconds - deltaSeconds).coerceAtLeast(0f)
        val horrorEvent = horrorDirector.update(
            deltaSeconds = deltaSeconds,
            context = HorrorContext(
                roomId = room.id,
                visitCount = roomVisitCounts[room.id] ?: 1,
                memoriesCollected = collectedMemories.size,
                tensionLevel = tensionSystem.level
            ),
            events = HorrorEventCatalog.events,
            intensityMultiplier = tensionSystem.eventIntensityMultiplier
        )
        horrorEvent?.let(::triggerHorrorEvent)

        if (messageSeconds > 0f) {
            messageSeconds = (messageSeconds - deltaSeconds).coerceAtLeast(0f)
            if (messageSeconds == 0f) message = null
        }
    }

    private fun triggerHorrorEvent(event: HorrorEvent) {
        when (event.action) {
            HorrorAction.LIVING_PICTURE_TILT -> {
                roomStateManager.markPhysical(
                    RoomId.LIVING_ROOM,
                    RoomStateManager.Flags.LIVING_PICTURE_TILTED
                )
                tensionSystem.addStimulus(2.5f)
                showMessage("...هل كانت الصورة مائلة قبل قليل؟", 2.8f)
            }
            HorrorAction.LIVING_SINGLE_KNOCK -> {
                onSpatialEffectRequested?.invoke(NormalizedPoint(0.84f, 0.28f), player.position)
                tensionSystem.addStimulus(1.5f)
            }
            HorrorAction.LIVING_LIGHT_DIM -> {
                temporaryBlackoutSeconds = 2.8f
                tensionSystem.addStimulus(2f)
            }
        }
        notifyTensionStageIfNeeded()
    }

    private fun notifyTensionStageIfNeeded() {
        val currentStage = tensionSystem.stage
        if (currentStage != lastTensionStage) {
            lastTensionStage = currentStage
            onTensionStageChanged?.invoke(currentStage)
        }
    }

    private fun isNearSafeLight(): Boolean {
        if (room.id != RoomId.ENTRANCE) return false
        val dx = player.position.x - 0.25f
        val dy = player.position.y - 0.55f
        return dx * dx + dy * dy <= 0.16f * 0.16f
    }

    private fun changeRoom(target: RoomId) {
        room = RoomCatalog.room(target)
        collisionSystem = CollisionSystem(room.walkableBounds, room.collisionRects)
        roomVisitCounts[target] = (roomVisitCounts[target] ?: 0) + 1
        player.position = when (target) {
            RoomId.ENTRANCE -> NormalizedPoint(0.80f, 0.50f)
            RoomId.LIVING_ROOM -> NormalizedPoint(0.18f, 0.50f)
        }
        showMessage(
            if (target == RoomId.LIVING_ROOM) "دخلت الصالون. البيت أكثر هدوءًا مما ينبغي."
            else "عدت إلى المدخل.",
            3f
        )
    }

    private fun showMessage(value: String, seconds: Float) {
        message = value
        messageSeconds = seconds
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPaint(backgroundPaint)
        drawNormalizedRect(canvas, room.walkableBounds, floorPaint, 20f)

        when (room.id) {
            RoomId.ENTRANCE -> drawEntrance(canvas)
            RoomId.LIVING_ROOM -> drawLivingRoom(canvas)
        }

        drawPlayer(canvas)
        lightingSystem.draw(
            canvas = canvas,
            width = width,
            height = height,
            normalizedX = player.position.x,
            normalizedY = player.position.y,
            tensionLevel = tensionSystem.level,
            extraDarkness = (temporaryBlackoutSeconds / 2.8f).coerceIn(0f, 1f)
        )
        drawHud(canvas)
    }

    private fun drawEntrance(canvas: Canvas) {
        drawNormalizedRect(canvas, NormalizedRect(0.07f, 0.10f, 0.93f, 0.15f), wallPaint, 0f)
        drawNormalizedRect(canvas, NormalizedRect(0.07f, 0.85f, 0.93f, 0.90f), wallPaint, 0f)

        coldPaint.alpha = 90
        drawNormalizedRect(canvas, NormalizedRect(0.36f, 0.38f, 0.70f, 0.62f), coldPaint, 16f)
        coldPaint.alpha = 255

        drawNormalizedRect(canvas, NormalizedRect(0.15f, 0.58f, 0.34f, 0.72f), furniturePaint, 12f)
        drawNormalizedRectOutline(canvas, NormalizedRect(0.15f, 0.58f, 0.34f, 0.72f), furnitureEdgePaint, 12f)
        drawClock(canvas)

        drawNormalizedRect(canvas, NormalizedRect(0.67f, 0.15f, 0.82f, 0.29f), furniturePaint, 10f)
        drawNormalizedRectOutline(canvas, NormalizedRect(0.67f, 0.15f, 0.82f, 0.29f), furnitureEdgePaint, 10f)
        drawNormalizedRect(canvas, NormalizedRect(0.84f, 0.38f, 0.92f, 0.63f), wallPaint, 6f)

        warmPaint.alpha = 115
        canvas.drawCircle(0.855f * width, 0.505f * height, minOf(width, height) * 0.009f, warmPaint)
        warmPaint.alpha = 255

        if ("entrance_key" !in hiddenHotspots) drawEntranceKey(canvas)
    }

    private fun drawEntranceKey(canvas: Canvas) {
        val x = 0.25f * width
        val y = 0.55f * height
        canvas.drawCircle(x, y, minOf(width, height) * 0.012f, warmPaint)
        canvas.drawRect(x, y - 2f, x + minOf(width, height) * 0.045f, y + 2f, warmPaint)
    }

    private fun drawClock(canvas: Canvas) {
        val x = 0.20f * width
        val y = 0.24f * height
        val radius = minOf(width, height) * 0.045f
        canvas.drawCircle(x, y, radius, furniturePaint)
        canvas.drawCircle(x, y, radius, furnitureEdgePaint)
        canvas.drawLine(x, y, x, y - radius * 0.48f, warmPaint)
        canvas.drawLine(x, y, x + radius * 0.38f, y + radius * 0.15f, warmPaint)
    }

    private fun drawLivingRoom(canvas: Canvas) {
        drawNormalizedRect(canvas, NormalizedRect(0.07f, 0.10f, 0.93f, 0.15f), wallPaint, 0f)
        drawNormalizedRect(canvas, NormalizedRect(0.07f, 0.85f, 0.93f, 0.90f), wallPaint, 0f)

        coldPaint.alpha = 55
        drawNormalizedRect(canvas, NormalizedRect(0.29f, 0.27f, 0.70f, 0.72f), coldPaint, 28f)
        coldPaint.alpha = 255

        // Central sofa and side furniture.
        drawNormalizedRect(canvas, NormalizedRect(0.37f, 0.36f, 0.63f, 0.61f), furniturePaint, 18f)
        drawNormalizedRectOutline(canvas, NormalizedRect(0.37f, 0.36f, 0.63f, 0.61f), furnitureEdgePaint, 18f)
        drawNormalizedRect(canvas, NormalizedRect(0.72f, 0.58f, 0.86f, 0.70f), furnitureDarkPaint, 10f)
        drawNormalizedRect(canvas, NormalizedRect(0.16f, 0.17f, 0.29f, 0.29f), furnitureDarkPaint, 10f)

        // Entrance door.
        drawNormalizedRect(canvas, NormalizedRect(0.08f, 0.38f, 0.15f, 0.63f), wallPaint, 6f)
        drawFamilyPicture(canvas)

        if ("living_birthday_card" !in hiddenHotspots) {
            val x = 0.79f * width
            val y = 0.55f * height
            rect.set(x - 18f, y - 11f, x + 18f, y + 11f)
            canvas.drawRoundRect(rect, 4f, 4f, warmPaint)
            canvas.drawLine(x - 13f, y, x + 11f, y, furnitureDarkPaint)
        }
    }

    private fun drawFamilyPicture(canvas: Canvas) {
        val centerX = 0.75f * width
        val centerY = 0.27f * height
        val tilted = roomStateManager.hasPhysical(
            RoomId.LIVING_ROOM,
            RoomStateManager.Flags.LIVING_PICTURE_TILTED
        )
        val extraPerson = roomStateManager.hasPhysical(
            RoomId.LIVING_ROOM,
            RoomStateManager.Flags.LIVING_EXTRA_PERSON
        )

        canvas.save()
        if (tilted) canvas.rotate(-7f, centerX, centerY)
        drawNormalizedRect(canvas, NormalizedRect(0.69f, 0.18f, 0.81f, 0.36f), coldPaint, 5f)
        drawNormalizedRect(canvas, NormalizedRect(0.704f, 0.20f, 0.796f, 0.34f), wallPaint, 3f)

        val count = if (extraPerson) 5 else 4
        val startX = centerX - (count - 1) * 11f
        repeat(count) { index ->
            val personX = startX + index * 22f
            val personPaint = if (extraPerson && index == count - 1) dangerPaint else playerBodyPaint
            personPaint.alpha = if (extraPerson && index == count - 1) 130 else 175
            canvas.drawCircle(personX, centerY - 8f, 5f, personPaint)
            rect.set(personX - 5f, centerY - 2f, personX + 5f, centerY + 14f)
            canvas.drawRoundRect(rect, 5f, 5f, personPaint)
            personPaint.alpha = 255
        }
        canvas.restore()
    }

    private fun drawHud(canvas: Canvas) {
        val unit = minOf(width, height).toFloat()
        textPaint.textSize = unit * 0.036f
        dimTextPaint.textSize = unit * 0.028f

        canvas.drawText(room.name, width * 0.5f, height * 0.075f, dimTextPaint)
        message?.let { canvas.drawText(it, width * 0.5f, height * 0.93f, textPaint) }
        if (message == null) {
            prompt?.let { canvas.drawText(it, width * 0.5f, height * 0.90f, dimTextPaint) }
        }
    }

    private fun drawNormalizedRect(canvas: Canvas, area: NormalizedRect, paint: Paint, radius: Float) {
        rect.set(area.left * width, area.top * height, area.right * width, area.bottom * height)
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    private fun drawNormalizedRectOutline(canvas: Canvas, area: NormalizedRect, paint: Paint, radius: Float) {
        rect.set(area.left * width, area.top * height, area.right * width, area.bottom * height)
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    private fun drawPlayer(canvas: Canvas) {
        val x = player.position.x * width
        val y = player.position.y * height
        val unit = minOf(width, height).toFloat()
        val bodyWidth = unit * 0.035f
        val bodyHeight = unit * 0.07f
        rect.set(x - bodyWidth, y - bodyHeight * 0.2f, x + bodyWidth, y + bodyHeight)
        canvas.drawRoundRect(rect, bodyWidth, bodyWidth, playerBodyPaint)
        canvas.drawCircle(x, y - bodyHeight * 0.48f, bodyWidth * 0.78f, playerHeadPaint)
    }
}
