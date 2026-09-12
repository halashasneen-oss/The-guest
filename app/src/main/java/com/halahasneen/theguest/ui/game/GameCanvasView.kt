package com.halahasneen.theguest.ui.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.halahasneen.theguest.data.event.HorrorEventCatalog
import com.halahasneen.theguest.data.model.EndingPath
import com.halahasneen.theguest.data.model.FinalChoice
import com.halahasneen.theguest.data.model.HorrorAction
import com.halahasneen.theguest.data.model.HorrorContext
import com.halahasneen.theguest.data.model.HorrorEvent
import com.halahasneen.theguest.data.model.Hotspot
import com.halahasneen.theguest.data.model.HotspotType
import com.halahasneen.theguest.data.model.NarrativeState
import com.halahasneen.theguest.data.model.NormalizedPoint
import com.halahasneen.theguest.data.model.NormalizedRect
import com.halahasneen.theguest.data.model.PlayerState
import com.halahasneen.theguest.data.model.RoomData
import com.halahasneen.theguest.data.model.RoomId
import com.halahasneen.theguest.data.room.RoomCatalog
import com.halahasneen.theguest.engine.CollisionSystem
import com.halahasneen.theguest.engine.EndingResolver
import com.halahasneen.theguest.engine.GameLoop
import com.halahasneen.theguest.engine.HorrorDirector
import com.halahasneen.theguest.engine.InteractionSystem
import com.halahasneen.theguest.engine.LightingSystem
import com.halahasneen.theguest.engine.RoomStateManager
import com.halahasneen.theguest.engine.TensionStage
import com.halahasneen.theguest.engine.TensionSystem
import kotlin.math.hypot

class GameCanvasView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var onSpatialEffectRequested: ((NormalizedPoint, NormalizedPoint) -> Unit)? = null
    var onFootstepsRequested: ((NormalizedPoint, NormalizedPoint) -> Unit)? = null
    var onDropRequested: ((NormalizedPoint, NormalizedPoint) -> Unit)? = null
    var onWhisperRequested: ((NormalizedPoint, NormalizedPoint) -> Unit)? = null
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
    private val narrativeState = NarrativeState()
    private var lastTensionStage = tensionSystem.stage

    private val roomVisitCounts = mutableMapOf(RoomId.ENTRANCE to 1)
    private val collectedMemories = mutableSetOf<String>()
    private val hiddenHotspots = mutableSetOf<String>()
    private var temporaryBlackoutSeconds = 0f
    private var shadowSeconds = 0f
    private var basementPresenceSeconds = 0f
    private var prompt: String? = null
    private var message: String? = "عدت إلى البيت بعد غياب طويل."
    private var messageSeconds = 4f
    private var endingPath: EndingPath? = null

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(13, 11, 20) }
    private val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(26, 22, 38) }
    private val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20, 17, 29) }
    private val furniturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(45, 40, 57) }
    private val furnitureDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(31, 27, 42) }
    private val furnitureEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.rgb(59, 75, 107) }
    private val warmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(232, 176, 75) }
    private val coldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(59, 75, 107) }
    private val dangerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(139, 30, 63) }
    private val playerBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(234, 234, 234) }
    private val playerHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(232, 176, 75) }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(234, 234, 234); textAlign = Paint.Align.CENTER }
    private val dimTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(234, 234, 234); alpha = 165; textAlign = Paint.Align.CENTER }
    private val endingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(228, 8, 7, 12) }
    private val rect = RectF()
    private val shadowPath = Path()

    private val gameLoop = GameLoop(onUpdate = ::updateGame, onRenderRequested = ::postInvalidateOnAnimation)

    fun setInputDirection(x: Float, y: Float) {
        val length = hypot(x, y)
        if (length > 1f) { inputX = x / length; inputY = y / length } else { inputX = x; inputY = y }
    }

    fun interact() {
        if (endingPath != null) return
        val hotspot = interactionSystem.nearest(player.position, currentHotspots(), hiddenHotspots)
        if (hotspot == null) { showMessage("لا شيء يلفت الانتباه هنا.", 1.4f); return }
        when (hotspot.type) {
            HotspotType.INSPECT -> inspect(hotspot)
            HotspotType.COLLECT -> collect(hotspot)
            HotspotType.DOOR -> hotspot.targetRoom?.let { target ->
                if (room.id == RoomId.BASEMENT && target == RoomId.BEDROOM && RoomCatalog.basementMemory.id in collectedMemories && narrativeState.finalChoice == null) {
                    narrativeState.recordAvoidance(1)
                }
                onSpatialEffectRequested?.invoke(hotspot.position, player.position)
                when (target) {
                    RoomId.LIVING_ROOM -> tensionSystem.addStimulus(3f)
                    RoomId.KITCHEN -> tensionSystem.addStimulus(4f)
                    RoomId.BEDROOM -> tensionSystem.addStimulus(5f)
                    RoomId.BASEMENT -> tensionSystem.addStimulus(8f)
                    RoomId.ENTRANCE -> Unit
                }
                changeRoom(target)
            }
            HotspotType.CHOICE -> handleChoice(hotspot)
        }
        notifyTensionStageIfNeeded()
    }

    fun resumeGame() = gameLoop.start()
    fun pauseGame() = gameLoop.stop()

    private fun currentHotspots(): List<Hotspot> {
        var hotspots = room.hotspots
        if (room.id == RoomId.KITCHEN && roomStateManager.hasPhysical(RoomId.KITCHEN, RoomStateManager.Flags.KITCHEN_ITEM_MOVED)) {
            hotspots = hotspots.map { if (it.id == "kitchen_loose_jar") it.copy(position = NormalizedPoint(0.30f, 0.72f)) else it }
        }
        if (room.id == RoomId.BASEMENT) {
            val memoryFound = RoomCatalog.basementMemory.id in collectedMemories
            val choiceMade = roomStateManager.hasPhysical(RoomId.BASEMENT, RoomStateManager.Flags.BASEMENT_CHOICE_MADE)
            hotspots = hotspots.filter { hotspot ->
                if (hotspot.type != HotspotType.CHOICE) true else memoryFound && !choiceMade
            }
        }
        return hotspots
    }

    private fun inspect(hotspot: Hotspot) {
        when (hotspot.id) {
            "living_family_picture" -> inspectFamilyPicture()
            "kitchen_loose_jar" -> {
                val moved = roomStateManager.hasPhysical(RoomId.KITCHEN, RoomStateManager.Flags.KITCHEN_ITEM_MOVED)
                if (moved) narrativeState.recordEvidence(1)
                tensionSystem.addStimulus(if (moved) 2.2f else 0.8f)
                showMessage(if (moved) "كان المرطبان قرب الطاولة... كيف وصل إلى هنا؟" else hotspot.message, 3.8f)
            }
            "bedroom_note" -> {
                val changed = roomStateManager.hasPhysical(RoomId.BEDROOM, RoomStateManager.Flags.BEDROOM_MESSAGE_CHANGED)
                if (changed) narrativeState.recordConfrontation(1)
                tensionSystem.addStimulus(if (changed) 3f else 1f)
                showMessage(if (changed) "إذا عدت يومًا... أنت تعرف من ينتظرك." else hotspot.message, 4.2f)
            }
            "basement_family_box" -> {
                narrativeState.recordEvidence(2)
                tensionSystem.addStimulus(3f)
                showMessage("اسمي مكتوب على الصندوق من الخارج... وتحتَه كلمة: الزائر.", 4.8f)
            }
            else -> { tensionSystem.addStimulus(0.8f); showMessage(hotspot.message, 3.6f) }
        }
    }

    private fun inspectFamilyPicture() {
        val tilted = roomStateManager.hasPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_PICTURE_TILTED)
        val extraPerson = roomStateManager.hasPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_EXTRA_PERSON)
        when {
            extraPerson -> {
                narrativeState.recordEvidence(1)
                tensionSystem.addStimulus(1.5f)
                showMessage("خمسة أشخاص. لا أستطيع تذكّر وجه الخامس.", 4f)
            }
            tilted -> {
                roomStateManager.markPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_EXTRA_PERSON)
                narrativeState.recordConfrontation(1)
                tensionSystem.addStimulus(6f); temporaryBlackoutSeconds = 0.7f
                showMessage("كانوا أربعة... من هذا الخامس؟", 4.5f)
            }
            else -> { tensionSystem.addStimulus(1f); showMessage("أربعة وجوه مألوفة. هكذا أتذكرها دائمًا.", 3.6f) }
        }
    }

    private fun collect(hotspot: Hotspot) {
        val newlyCollected = hotspot.memoryItemId?.let(collectedMemories::add) ?: false
        hiddenHotspots.add(hotspot.id)
        if (newlyCollected) narrativeState.recordEvidence(if (hotspot.id == "basement_tape") 2 else 1)
        when (hotspot.id) {
            "living_birthday_card" -> {
                roomStateManager.markPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_MEMORY_TAKEN)
                tensionSystem.addStimulus(5f); showMessage("ذكرى 2/5 — بطاقة عيد قديمة... توقيع خامس مطموس.", 4.5f)
            }
            "kitchen_chipped_cup" -> {
                roomStateManager.markPhysical(RoomId.KITCHEN, RoomStateManager.Flags.KITCHEN_MEMORY_TAKEN)
                tensionSystem.addStimulus(6f); showMessage("ذكرى 3/5 — خمسة خطوط على الكوب. لماذا خمسة؟", 4.5f)
            }
            "bedroom_letter_fragment" -> {
                roomStateManager.markPhysical(RoomId.BEDROOM, RoomStateManager.Flags.BEDROOM_MEMORY_TAKEN)
                tensionSystem.addStimulus(7f); showMessage("ذكرى 4/5 — القصاصة تحمل تاريخ الليلة التي غادرتُ فيها البيت.", 4.8f)
            }
            "basement_tape" -> {
                roomStateManager.markPhysical(RoomId.BASEMENT, RoomStateManager.Flags.BASEMENT_MEMORY_TAKEN)
                tensionSystem.addStimulus(10f)
                temporaryBlackoutSeconds = 1.2f
                showMessage("ذكرى 5/5 — التسجيل يقول: «إذا عاد... فهو الزائر هذه المرة.»", 5.5f)
            }
            else -> { tensionSystem.addStimulus(4f); showMessage(hotspot.message, 4f) }
        }
    }

    private fun handleChoice(hotspot: Hotspot) {
        if (RoomCatalog.basementMemory.id !in collectedMemories) {
            showMessage("ما زال هناك شيء يجب أن أسمعه أولًا.", 2.5f)
            return
        }
        when (hotspot.id) {
            "basement_confront" -> {
                narrativeState.finalChoice = FinalChoice.CONFRONT
                narrativeState.recordConfrontation(4)
            }
            "basement_turn_away" -> {
                narrativeState.finalChoice = FinalChoice.TURN_AWAY
                narrativeState.recordAvoidance(4)
            }
            else -> return
        }
        roomStateManager.markPhysical(RoomId.BASEMENT, RoomStateManager.Flags.BASEMENT_CHOICE_MADE)
        endingPath = EndingResolver.resolve(narrativeState, collectedMemories.size)
        tensionSystem.addStimulus(12f)
        temporaryBlackoutSeconds = 2.8f
    }

    private fun updateGame(deltaSeconds: Float) {
        if (endingPath == null) {
            val desired = NormalizedPoint(player.position.x + inputX * player.speedPerSecond * deltaSeconds, player.position.y + inputY * player.speedPerSecond * deltaSeconds)
            player.position = collisionSystem.resolve(player.position, desired, player.radius)
            prompt = interactionSystem.nearest(player.position, currentHotspots(), hiddenHotspots)?.label
            val inSafeLight = isNearSafeLight()
            tensionSystem.update(deltaSeconds, inDarkness = !inSafeLight, inSafeLight = inSafeLight)
            notifyTensionStageIfNeeded()

            horrorDirector.update(
                deltaSeconds,
                HorrorContext(room.id, roomVisitCounts[room.id] ?: 1, collectedMemories.size, tensionSystem.level),
                HorrorEventCatalog.events,
                tensionSystem.eventIntensityMultiplier
            )?.let(::triggerHorrorEvent)
        }

        temporaryBlackoutSeconds = (temporaryBlackoutSeconds - deltaSeconds).coerceAtLeast(0f)
        shadowSeconds = (shadowSeconds - deltaSeconds).coerceAtLeast(0f)
        basementPresenceSeconds = (basementPresenceSeconds - deltaSeconds).coerceAtLeast(0f)
        if (messageSeconds > 0f) {
            messageSeconds = (messageSeconds - deltaSeconds).coerceAtLeast(0f)
            if (messageSeconds == 0f) message = null
        }
    }

    private fun triggerHorrorEvent(event: HorrorEvent) {
        when (event.action) {
            HorrorAction.LIVING_PICTURE_TILT -> {
                roomStateManager.markPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_PICTURE_TILTED)
                tensionSystem.addStimulus(2.5f); showMessage("...هل كانت الصورة مائلة قبل قليل؟", 2.8f)
            }
            HorrorAction.LIVING_SINGLE_KNOCK -> { onSpatialEffectRequested?.invoke(NormalizedPoint(0.84f, 0.28f), player.position); tensionSystem.addStimulus(1.5f) }
            HorrorAction.LIVING_LIGHT_DIM -> { temporaryBlackoutSeconds = 2.8f; tensionSystem.addStimulus(2f) }
            HorrorAction.KITCHEN_FOOTSTEPS -> { onFootstepsRequested?.invoke(NormalizedPoint(0.88f, 0.22f), player.position); tensionSystem.addStimulus(2.5f); showMessage("خطوات قصيرة... ثم صمت.", 2.2f) }
            HorrorAction.KITCHEN_OBJECT_DROP -> {
                roomStateManager.markPhysical(RoomId.KITCHEN, RoomStateManager.Flags.KITCHEN_OBJECT_FALLEN)
                onDropRequested?.invoke(NormalizedPoint(0.73f, 0.66f), player.position); temporaryBlackoutSeconds = 0.45f
                tensionSystem.addStimulus(4f); showMessage("شيء سقط خلفي.", 2.5f)
            }
            HorrorAction.KITCHEN_ITEM_MOVE -> {
                roomStateManager.markPhysical(RoomId.KITCHEN, RoomStateManager.Flags.KITCHEN_ITEM_MOVED)
                tensionSystem.addStimulus(3.5f); showMessage("هناك شيء مختلف في المطبخ...", 2.5f)
            }
            HorrorAction.BEDROOM_SHADOW -> { shadowSeconds = 1.15f; tensionSystem.addStimulus(5f) }
            HorrorAction.BEDROOM_MESSAGE_CHANGE -> {
                roomStateManager.markPhysical(RoomId.BEDROOM, RoomStateManager.Flags.BEDROOM_MESSAGE_CHANGED)
                temporaryBlackoutSeconds = 0.65f; tensionSystem.addStimulus(4.5f)
                showMessage("الكلمات على الورقة لم تعد كما كانت.", 3f)
            }
            HorrorAction.BEDROOM_DOOR_MOVE -> {
                roomStateManager.markPhysical(RoomId.BEDROOM, RoomStateManager.Flags.BEDROOM_DOOR_SHIFTED)
                onSpatialEffectRequested?.invoke(NormalizedPoint(0.12f, 0.52f), player.position)
                tensionSystem.addStimulus(5f); showMessage("الباب تحرّك وحده.", 2.7f)
            }
            HorrorAction.BEDROOM_WHISPER -> {
                onWhisperRequested?.invoke(NormalizedPoint(0.91f, 0.42f), player.position)
                tensionSystem.addStimulus(3.5f); showMessage("همس خافت: «ارجع...»", 2.5f)
            }
            HorrorAction.BASEMENT_CHAIN_RATTLE -> {
                onDropRequested?.invoke(NormalizedPoint(0.82f, 0.24f), player.position)
                tensionSystem.addStimulus(4f); showMessage("صوت سلسلة تتحرك في الظلام.", 2.4f)
            }
            HorrorAction.BASEMENT_BLACKOUT -> {
                roomStateManager.markPhysical(RoomId.BASEMENT, RoomStateManager.Flags.BASEMENT_BLACKOUT_SEEN)
                temporaryBlackoutSeconds = 2.8f; tensionSystem.addStimulus(7f)
                showMessage("انطفأ كل شيء... لكن التنفس لم يتوقف.", 3.2f)
            }
            HorrorAction.BASEMENT_PRESENCE -> {
                roomStateManager.markPhysical(RoomId.BASEMENT, RoomStateManager.Flags.BASEMENT_PRESENCE_SEEN)
                basementPresenceSeconds = 1.5f; tensionSystem.addStimulus(8f)
                onWhisperRequested?.invoke(NormalizedPoint(0.86f, 0.44f), player.position)
            }
        }
        notifyTensionStageIfNeeded()
    }

    private fun notifyTensionStageIfNeeded() {
        val current = tensionSystem.stage
        if (current != lastTensionStage) { lastTensionStage = current; onTensionStageChanged?.invoke(current) }
    }

    private fun isNearSafeLight(): Boolean {
        if (room.id != RoomId.ENTRANCE) return false
        val dx = player.position.x - 0.25f; val dy = player.position.y - 0.55f
        return dx * dx + dy * dy <= 0.16f * 0.16f
    }

    private fun changeRoom(target: RoomId) {
        room = RoomCatalog.room(target)
        collisionSystem = CollisionSystem(room.walkableBounds, room.collisionRects)
        roomVisitCounts[target] = (roomVisitCounts[target] ?: 0) + 1
        player.position = when (target) {
            RoomId.ENTRANCE -> NormalizedPoint(0.80f, 0.50f)
            RoomId.LIVING_ROOM -> NormalizedPoint(0.18f, 0.60f)
            RoomId.KITCHEN -> NormalizedPoint(0.18f, 0.52f)
            RoomId.BEDROOM -> NormalizedPoint(0.18f, 0.52f)
            RoomId.BASEMENT -> NormalizedPoint(0.18f, 0.50f)
        }
        val entryMessage = when (target) {
            RoomId.ENTRANCE -> "عدت إلى المدخل."
            RoomId.LIVING_ROOM -> "دخلت الصالون. البيت أكثر هدوءًا مما ينبغي."
            RoomId.KITCHEN -> "المطبخ أبرد من بقية البيت."
            RoomId.BEDROOM -> "غرفة النوم ما زالت كما تركتها... تقريبًا."
            RoomId.BASEMENT -> "نزلت إلى القبو. هنا لا يبدو أنني وحدي."
        }
        showMessage(entryMessage, 3f)
    }

    private fun showMessage(value: String, seconds: Float) { message = value; messageSeconds = seconds }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPaint(backgroundPaint)
        drawNormalizedRect(canvas, room.walkableBounds, floorPaint, 20f)
        when (room.id) {
            RoomId.ENTRANCE -> drawEntrance(canvas)
            RoomId.LIVING_ROOM -> drawLivingRoom(canvas)
            RoomId.KITCHEN -> drawKitchen(canvas)
            RoomId.BEDROOM -> drawBedroom(canvas)
            RoomId.BASEMENT -> drawBasement(canvas)
        }
        if (room.id == RoomId.BEDROOM && shadowSeconds > 0f) drawBedroomShadow(canvas)
        if (room.id == RoomId.BASEMENT && basementPresenceSeconds > 0f) drawBasementPresence(canvas)
        drawPlayer(canvas)
        lightingSystem.draw(canvas, width, height, player.position.x, player.position.y, tensionSystem.level, (temporaryBlackoutSeconds / 2.8f).coerceIn(0f, 1f))
        drawHud(canvas)
        endingPath?.let { drawEnding(canvas, it) }
    }

    private fun drawEntrance(canvas: Canvas) {
        drawNormalizedRect(canvas, NormalizedRect(0.07f, 0.10f, 0.93f, 0.15f), wallPaint, 0f)
        drawNormalizedRect(canvas, NormalizedRect(0.07f, 0.85f, 0.93f, 0.90f), wallPaint, 0f)
        coldPaint.alpha = 90; drawNormalizedRect(canvas, NormalizedRect(0.36f, 0.38f, 0.70f, 0.62f), coldPaint, 16f); coldPaint.alpha = 255
        drawNormalizedRect(canvas, NormalizedRect(0.15f, 0.58f, 0.34f, 0.72f), furniturePaint, 12f)
        drawNormalizedRectOutline(canvas, NormalizedRect(0.15f, 0.58f, 0.34f, 0.72f), furnitureEdgePaint, 12f)
        drawClock(canvas)
        drawNormalizedRect(canvas, NormalizedRect(0.67f, 0.15f, 0.82f, 0.29f), furniturePaint, 10f)
        drawNormalizedRect(canvas, NormalizedRect(0.84f, 0.38f, 0.92f, 0.63f), wallPaint, 6f)
        if ("entrance_key" !in hiddenHotspots) drawEntranceKey(canvas)
    }

    private fun drawEntranceKey(canvas: Canvas) {
        val x = 0.25f * width; val y = 0.55f * height
        canvas.drawCircle(x, y, minOf(width, height) * 0.012f, warmPaint)
        canvas.drawRect(x, y - 2f, x + minOf(width, height) * 0.045f, y + 2f, warmPaint)
    }

    private fun drawClock(canvas: Canvas) {
        val x = 0.20f * width; val y = 0.24f * height; val radius = minOf(width, height) * 0.045f
        canvas.drawCircle(x, y, radius, furniturePaint); canvas.drawCircle(x, y, radius, furnitureEdgePaint)
        canvas.drawLine(x, y, x, y - radius * 0.48f, warmPaint); canvas.drawLine(x, y, x + radius * 0.38f, y + radius * 0.15f, warmPaint)
    }

    private fun drawLivingRoom(canvas: Canvas) {
        drawNormalizedRect(canvas, NormalizedRect(0.07f, 0.10f, 0.93f, 0.15f), wallPaint, 0f)
        drawNormalizedRect(canvas, NormalizedRect(0.07f, 0.85f, 0.93f, 0.90f), wallPaint, 0f)
        coldPaint.alpha = 55; drawNormalizedRect(canvas, NormalizedRect(0.29f, 0.27f, 0.70f, 0.72f), coldPaint, 28f); coldPaint.alpha = 255
        drawNormalizedRect(canvas, NormalizedRect(0.37f, 0.36f, 0.63f, 0.61f), furniturePaint, 18f)
        drawNormalizedRectOutline(canvas, NormalizedRect(0.37f, 0.36f, 0.63f, 0.61f), furnitureEdgePaint, 18f)
        drawNormalizedRect(canvas, NormalizedRect(0.72f, 0.58f, 0.86f, 0.70f), furnitureDarkPaint, 10f)
        drawNormalizedRect(canvas, NormalizedRect(0.16f, 0.17f, 0.29f, 0.29f), furnitureDarkPaint, 10f)
        drawNormalizedRect(canvas, NormalizedRect(0.08f, 0.38f, 0.15f, 0.63f), wallPaint, 6f)
        drawNormalizedRect(canvas, NormalizedRect(0.85f, 0.64f, 0.92f, 0.82f), wallPaint, 6f)
        drawFamilyPicture(canvas)
        if ("living_birthday_card" !in hiddenHotspots) {
            val x = 0.79f * width; val y = 0.55f * height
            rect.set(x - 18f, y - 11f, x + 18f, y + 11f); canvas.drawRoundRect(rect, 4f, 4f, warmPaint)
        }
    }

    private fun drawFamilyPicture(canvas: Canvas) {
        val centerX = 0.75f * width; val centerY = 0.27f * height
        val tilted = roomStateManager.hasPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_PICTURE_TILTED)
        val extra = roomStateManager.hasPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_EXTRA_PERSON)
        canvas.save(); if (tilted) canvas.rotate(-7f, centerX, centerY)
        drawNormalizedRect(canvas, NormalizedRect(0.69f, 0.18f, 0.81f, 0.36f), coldPaint, 5f)
        drawNormalizedRect(canvas, NormalizedRect(0.704f, 0.20f, 0.796f, 0.34f), wallPaint, 3f)
        val count = if (extra) 5 else 4; val startX = centerX - (count - 1) * 11f
        repeat(count) { index ->
            val x = startX + index * 22f; val paint = if (extra && index == count - 1) dangerPaint else playerBodyPaint
            paint.alpha = if (extra && index == count - 1) 130 else 175
            canvas.drawCircle(x, centerY - 8f, 5f, paint); rect.set(x - 5f, centerY - 2f, x + 5f, centerY + 14f); canvas.drawRoundRect(rect, 5f, 5f, paint); paint.alpha = 255
        }
        canvas.restore()
    }

    private fun drawKitchen(canvas: Canvas) {
        drawNormalizedRect(canvas, NormalizedRect(0.07f, 0.10f, 0.93f, 0.15f), wallPaint, 0f)
        drawNormalizedRect(canvas, NormalizedRect(0.07f, 0.85f, 0.93f, 0.90f), wallPaint, 0f)
        drawNormalizedRect(canvas, NormalizedRect(0.16f, 0.16f, 0.38f, 0.31f), furnitureDarkPaint, 8f)
        drawNormalizedRect(canvas, NormalizedRect(0.70f, 0.16f, 0.88f, 0.31f), furniturePaint, 8f)
        drawNormalizedRect(canvas, NormalizedRect(0.43f, 0.43f, 0.62f, 0.68f), furniturePaint, 14f)
        drawNormalizedRectOutline(canvas, NormalizedRect(0.43f, 0.43f, 0.62f, 0.68f), furnitureEdgePaint, 14f)
        drawNormalizedRect(canvas, NormalizedRect(0.08f, 0.39f, 0.15f, 0.63f), wallPaint, 6f)
        drawNormalizedRect(canvas, NormalizedRect(0.85f, 0.39f, 0.92f, 0.64f), wallPaint, 6f)
        if ("kitchen_chipped_cup" !in hiddenHotspots) {
            val x = 0.72f * width; val y = 0.57f * height; val u = minOf(width, height).toFloat()
            rect.set(x - u * 0.018f, y - u * 0.020f, x + u * 0.018f, y + u * 0.020f); canvas.drawRoundRect(rect, 5f, 5f, warmPaint)
        }
        val moved = roomStateManager.hasPhysical(RoomId.KITCHEN, RoomStateManager.Flags.KITCHEN_ITEM_MOVED)
        val jar = if (moved) NormalizedPoint(0.30f, 0.72f) else NormalizedPoint(0.57f, 0.73f)
        val jx = jar.x * width; val jy = jar.y * height; val u = minOf(width, height).toFloat()
        rect.set(jx - u * 0.015f, jy - u * 0.026f, jx + u * 0.015f, jy + u * 0.026f); canvas.drawRoundRect(rect, 4f, 4f, coldPaint)
        if (roomStateManager.hasPhysical(RoomId.KITCHEN, RoomStateManager.Flags.KITCHEN_OBJECT_FALLEN)) {
            dangerPaint.alpha = 125; rect.set(0.68f * width, 0.70f * height, 0.75f * width, 0.715f * height); canvas.drawOval(rect, dangerPaint); dangerPaint.alpha = 255
        }
    }

    private fun drawBedroom(canvas: Canvas) {
        drawNormalizedRect(canvas, NormalizedRect(0.07f, 0.10f, 0.93f, 0.15f), wallPaint, 0f)
        drawNormalizedRect(canvas, NormalizedRect(0.07f, 0.85f, 0.93f, 0.90f), wallPaint, 0f)
        coldPaint.alpha = 70; drawNormalizedRect(canvas, NormalizedRect(0.78f, 0.17f, 0.88f, 0.34f), coldPaint, 4f); coldPaint.alpha = 255
        drawNormalizedRect(canvas, NormalizedRect(0.48f, 0.34f, 0.76f, 0.69f), furniturePaint, 18f)
        drawNormalizedRect(canvas, NormalizedRect(0.51f, 0.37f, 0.73f, 0.47f), playerBodyPaint, 12f)
        drawNormalizedRect(canvas, NormalizedRect(0.16f, 0.17f, 0.31f, 0.31f), furnitureDarkPaint, 8f)
        drawNormalizedRect(canvas, NormalizedRect(0.18f, 0.66f, 0.31f, 0.79f), furnitureDarkPaint, 10f)
        val shifted = roomStateManager.hasPhysical(RoomId.BEDROOM, RoomStateManager.Flags.BEDROOM_DOOR_SHIFTED)
        val door = if (shifted) NormalizedRect(0.10f, 0.36f, 0.17f, 0.64f) else NormalizedRect(0.08f, 0.39f, 0.15f, 0.63f)
        drawNormalizedRect(canvas, door, wallPaint, 6f)
        drawNormalizedRect(canvas, NormalizedRect(0.84f, 0.64f, 0.92f, 0.82f), wallPaint, 6f)
        val changed = roomStateManager.hasPhysical(RoomId.BEDROOM, RoomStateManager.Flags.BEDROOM_MESSAGE_CHANGED)
        val notePaint = if (changed) dangerPaint else warmPaint
        notePaint.alpha = if (changed) 155 else 220; drawNormalizedRect(canvas, NormalizedRect(0.68f, 0.19f, 0.76f, 0.29f), notePaint, 3f); notePaint.alpha = 255
        if ("bedroom_letter_fragment" !in hiddenHotspots) {
            warmPaint.alpha = 180; drawNormalizedRect(canvas, NormalizedRect(0.27f, 0.55f, 0.33f, 0.61f), warmPaint, 2f); warmPaint.alpha = 255
        }
    }

    private fun drawBedroomShadow(canvas: Canvas) {
        dangerPaint.alpha = (70 + (shadowSeconds / 1.15f) * 35f).toInt().coerceIn(40, 105)
        shadowPath.reset(); shadowPath.moveTo(width * 0.92f, height * 0.22f); shadowPath.lineTo(width * 0.99f, height * 0.42f); shadowPath.lineTo(width * 0.93f, height * 0.73f); shadowPath.lineTo(width * 0.88f, height * 0.44f); shadowPath.close()
        canvas.drawPath(shadowPath, dangerPaint); dangerPaint.alpha = 255
    }

    private fun drawBasement(canvas: Canvas) {
        drawNormalizedRect(canvas, NormalizedRect(0.07f, 0.10f, 0.93f, 0.15f), wallPaint, 0f)
        drawNormalizedRect(canvas, NormalizedRect(0.07f, 0.85f, 0.93f, 0.90f), wallPaint, 0f)
        dangerPaint.alpha = 45; drawNormalizedRect(canvas, NormalizedRect(0.30f, 0.24f, 0.77f, 0.78f), dangerPaint, 32f); dangerPaint.alpha = 255
        drawNormalizedRect(canvas, NormalizedRect(0.15f, 0.16f, 0.35f, 0.31f), furnitureDarkPaint, 8f)
        drawNormalizedRect(canvas, NormalizedRect(0.42f, 0.39f, 0.60f, 0.64f), furniturePaint, 12f)
        drawNormalizedRectOutline(canvas, NormalizedRect(0.42f, 0.39f, 0.60f, 0.64f), furnitureEdgePaint, 12f)
        drawNormalizedRect(canvas, NormalizedRect(0.70f, 0.18f, 0.86f, 0.33f), furnitureDarkPaint, 8f)
        drawNormalizedRect(canvas, NormalizedRect(0.08f, 0.39f, 0.15f, 0.63f), wallPaint, 6f)
        if ("basement_tape" !in hiddenHotspots) {
            warmPaint.alpha = 190; drawNormalizedRect(canvas, NormalizedRect(0.49f, 0.67f, 0.57f, 0.73f), warmPaint, 4f); warmPaint.alpha = 255
        }
        if (RoomCatalog.basementMemory.id in collectedMemories && narrativeState.finalChoice == null) {
            dangerPaint.alpha = 110; canvas.drawCircle(0.78f * width, 0.52f * height, minOf(width, height) * 0.035f, dangerPaint)
            coldPaint.alpha = 110; canvas.drawCircle(0.22f * width, 0.72f * height, minOf(width, height) * 0.035f, coldPaint)
            dangerPaint.alpha = 255; coldPaint.alpha = 255
        }
    }

    private fun drawBasementPresence(canvas: Canvas) {
        dangerPaint.alpha = (65 + basementPresenceSeconds * 22f).toInt().coerceIn(45, 100)
        shadowPath.reset(); shadowPath.moveTo(width * 0.81f, height * 0.20f); shadowPath.lineTo(width * 0.91f, height * 0.45f); shadowPath.lineTo(width * 0.83f, height * 0.80f); shadowPath.lineTo(width * 0.75f, height * 0.43f); shadowPath.close()
        canvas.drawPath(shadowPath, dangerPaint); dangerPaint.alpha = 255
    }

    private fun drawEnding(canvas: Canvas, ending: EndingPath) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), endingPaint)
        val unit = minOf(width, height).toFloat()
        textPaint.textSize = unit * 0.065f
        dimTextPaint.textSize = unit * 0.032f
        val title = if (ending == EndingPath.TRUTH) "النهاية: الحقيقة" else "النهاية: الإنكار"
        val line1 = if (ending == EndingPath.TRUTH) "لم يكن البيت ينتظر زائرًا جديدًا." else "غادرت المنزل قبل أن تعرف من كان ينتظر من."
        val line2 = if (ending == EndingPath.TRUTH) "أنت من عاد إلى مكان لم يعد يعتبرك من أهله." else "أغلقت الباب خلفك... وبقي السؤال في الداخل."
        canvas.drawText(title, width * 0.5f, height * 0.40f, textPaint)
        canvas.drawText(line1, width * 0.5f, height * 0.52f, dimTextPaint)
        canvas.drawText(line2, width * 0.5f, height * 0.59f, dimTextPaint)
    }

    private fun drawHud(canvas: Canvas) {
        val unit = minOf(width, height).toFloat(); textPaint.textSize = unit * 0.036f; dimTextPaint.textSize = unit * 0.028f
        canvas.drawText(room.name, width * 0.5f, height * 0.075f, dimTextPaint)
        message?.let { canvas.drawText(it, width * 0.5f, height * 0.93f, textPaint) }
        if (message == null) prompt?.let { canvas.drawText(it, width * 0.5f, height * 0.90f, dimTextPaint) }
    }

    private fun drawNormalizedRect(canvas: Canvas, area: NormalizedRect, paint: Paint, radius: Float) {
        rect.set(area.left * width, area.top * height, area.right * width, area.bottom * height); canvas.drawRoundRect(rect, radius, radius, paint)
    }

    private fun drawNormalizedRectOutline(canvas: Canvas, area: NormalizedRect, paint: Paint, radius: Float) {
        rect.set(area.left * width, area.top * height, area.right * width, area.bottom * height); canvas.drawRoundRect(rect, radius, radius, paint)
    }

    private fun drawPlayer(canvas: Canvas) {
        val x = player.position.x * width; val y = player.position.y * height; val unit = minOf(width, height).toFloat(); val bw = unit * 0.035f; val bh = unit * 0.07f
        rect.set(x - bw, y - bh * 0.2f, x + bw, y + bh); canvas.drawRoundRect(rect, bw, bw, playerBodyPaint); canvas.drawCircle(x, y - bh * 0.48f, bw * 0.78f, playerHeadPaint)
    }
}
