package com.halahasneen.theguest.ui.game

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.halahasneen.theguest.data.event.HorrorEventCatalog
import com.halahasneen.theguest.data.model.EndingPath
import com.halahasneen.theguest.data.model.FinalChoice
import com.halahasneen.theguest.data.model.GameSaveData
import com.halahasneen.theguest.data.model.GameSettings
import com.halahasneen.theguest.data.model.HorrorAction
import com.halahasneen.theguest.data.model.HorrorContext
import com.halahasneen.theguest.data.model.HorrorEvent
import com.halahasneen.theguest.data.model.Hotspot
import com.halahasneen.theguest.data.model.HotspotType
import com.halahasneen.theguest.data.model.NarrativeState
import com.halahasneen.theguest.data.model.NormalizedPoint
import com.halahasneen.theguest.data.model.PlayerState
import com.halahasneen.theguest.data.model.RoomData
import com.halahasneen.theguest.data.model.RoomId
import com.halahasneen.theguest.data.model.RoomSaveState
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

class GameCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onSpatialEffectRequested: ((NormalizedPoint, NormalizedPoint) -> Unit)? = null
    var onFootstepsRequested: ((NormalizedPoint, NormalizedPoint) -> Unit)? = null
    var onDropRequested: ((NormalizedPoint, NormalizedPoint) -> Unit)? = null
    var onWhisperRequested: ((NormalizedPoint, NormalizedPoint) -> Unit)? = null
    var onTensionStageChanged: ((TensionStage) -> Unit)? = null
    var onAutosaveRequested: ((GameSaveData) -> Unit)? = null
    var onHapticRequested: (() -> Unit)? = null

    private val player = PlayerState(position = NormalizedPoint(0.18f, 0.50f))
    private val interactionSystem = InteractionSystem()
    private val lightingSystem = LightingSystem()
    private val renderer = RoomRenderer()
    private val tensionSystem = TensionSystem()
    private val horrorDirector = HorrorDirector()
    private val roomStateManager = RoomStateManager()
    private val narrativeState = NarrativeState()

    private var inputX = 0f
    private var inputY = 0f
    private var room: RoomData = RoomCatalog.entrance
    private var collisionSystem = CollisionSystem(room.walkableBounds, room.collisionRects)
    private var activeHotspots: List<Hotspot> = room.hotspots
    private var lastTensionStage = tensionSystem.stage
    private var subtitlesEnabled = true
    private var vibrationEnabled = true

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

    fun applySettings(settings: GameSettings) {
        val normalized = settings.normalized()
        subtitlesEnabled = normalized.subtitlesEnabled
        vibrationEnabled = normalized.vibrationEnabled
    }

    fun resumeGame() = gameLoop.start()
    fun pauseGame() = gameLoop.stop()

    fun interact() {
        if (endingPath != null) return
        val hotspot = interactionSystem.nearest(player.position, activeHotspots, hiddenHotspots)
        if (hotspot == null) {
            showMessage("لا شيء يلفت الانتباه هنا.", 1.4f)
            return
        }
        when (hotspot.type) {
            HotspotType.INSPECT -> inspect(hotspot)
            HotspotType.COLLECT -> collect(hotspot)
            HotspotType.DOOR -> hotspot.targetRoom?.let { target -> handleDoor(hotspot, target) }
            HotspotType.CHOICE -> handleChoice(hotspot)
        }
        notifyTensionStageIfNeeded()
        autosave()
    }

    fun createSaveData(): GameSaveData = GameSaveData(
        currentRoom = room.id,
        playerPosition = player.position,
        collectedMemories = collectedMemories.toSet(),
        hiddenHotspots = hiddenHotspots.toSet(),
        tensionLevel = tensionSystem.level,
        roomStates = RoomId.values().associateWith { roomId ->
            RoomSaveState(
                physicalFlags = roomStateManager.physicalFlags(roomId),
                perceivedFlags = roomStateManager.perceivedFlags(roomId)
            )
        },
        roomVisitCounts = roomVisitCounts.toMap(),
        acceptanceScore = narrativeState.acceptanceScore,
        denialScore = narrativeState.denialScore,
        curiosityScore = narrativeState.curiosityScore,
        avoidanceScore = narrativeState.avoidanceScore,
        finalChoice = narrativeState.finalChoice,
        endingPath = endingPath,
        triggeredOneShots = horrorDirector.triggeredOneShots()
    )

    fun restore(save: GameSaveData) {
        collectedMemories.clear()
        collectedMemories.addAll(save.collectedMemories)
        hiddenHotspots.clear()
        hiddenHotspots.addAll(save.hiddenHotspots)
        roomVisitCounts.clear()
        roomVisitCounts.putAll(save.roomVisitCounts)
        if (roomVisitCounts.isEmpty()) roomVisitCounts[RoomId.ENTRANCE] = 1

        roomStateManager.reset()
        save.roomStates.forEach { (roomId, state) ->
            roomStateManager.restoreRoom(roomId, state.physicalFlags, state.perceivedFlags)
        }
        narrativeState.acceptanceScore = save.acceptanceScore
        narrativeState.denialScore = save.denialScore
        narrativeState.curiosityScore = save.curiosityScore
        narrativeState.avoidanceScore = save.avoidanceScore
        narrativeState.finalChoice = save.finalChoice
        endingPath = save.endingPath
        horrorDirector.restoreTriggeredOneShots(save.triggeredOneShots)
        tensionSystem.restore(save.tensionLevel)
        lastTensionStage = tensionSystem.stage

        room = RoomCatalog.room(save.currentRoom)
        collisionSystem = CollisionSystem(room.walkableBounds, room.collisionRects)
        player.position = collisionSystem.resolve(defaultSpawn(room.id), save.playerPosition, player.radius)
        temporaryBlackoutSeconds = 0f
        shadowSeconds = 0f
        basementPresenceSeconds = 0f
        message = "تمت استعادة آخر نقطة حفظ."
        messageSeconds = 2.2f
        refreshActiveHotspots()
        onTensionStageChanged?.invoke(tensionSystem.stage)
        invalidate()
    }

    private fun handleDoor(hotspot: Hotspot, target: RoomId) {
        val source = room.id
        if (
            source == RoomId.BASEMENT &&
            target == RoomId.BEDROOM &&
            RoomCatalog.basementMemory.id in collectedMemories &&
            narrativeState.finalChoice == null &&
            !roomStateManager.hasPhysical(RoomId.BASEMENT, RoomStateManager.Flags.BASEMENT_AVOIDANCE_RECORDED)
        ) {
            narrativeState.recordAvoidance(1)
            roomStateManager.markPhysical(RoomId.BASEMENT, RoomStateManager.Flags.BASEMENT_AVOIDANCE_RECORDED)
        }
        onSpatialEffectRequested?.invoke(hotspot.position, player.position)
        when (target) {
            RoomId.ENTRANCE -> Unit
            RoomId.LIVING_ROOM -> tensionSystem.addStimulus(3f)
            RoomId.KITCHEN -> tensionSystem.addStimulus(4f)
            RoomId.BEDROOM -> tensionSystem.addStimulus(5f)
            RoomId.BASEMENT -> tensionSystem.addStimulus(8f)
        }
        changeRoom(source, target)
    }

    private fun inspect(hotspot: Hotspot) {
        when (hotspot.id) {
            "living_family_picture" -> inspectFamilyPicture()
            "kitchen_loose_jar" -> inspectKitchenJar(hotspot)
            "bedroom_note" -> inspectBedroomNote(hotspot)
            "basement_family_box" -> inspectBasementBox()
            else -> {
                tensionSystem.addStimulus(0.8f)
                showMessage(hotspot.message, 3.6f)
            }
        }
    }

    private fun inspectFamilyPicture() {
        val tilted = roomStateManager.hasPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_PICTURE_TILTED)
        val extraPerson = roomStateManager.hasPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_EXTRA_PERSON)
        when {
            extraPerson -> {
                recordEvidenceOnce(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_EVIDENCE_RECORDED, 1)
                tensionSystem.addStimulus(1.5f)
                showMessage("خمسة أشخاص. لا أستطيع تذكّر وجه الخامس.", 4f)
            }
            tilted -> {
                roomStateManager.markPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_EXTRA_PERSON)
                narrativeState.recordConfrontation(1)
                tensionSystem.addStimulus(6f)
                temporaryBlackoutSeconds = 0.7f
                haptic()
                showMessage("كانوا أربعة... من هذا الخامس؟", 4.5f)
            }
            else -> {
                tensionSystem.addStimulus(1f)
                showMessage("أربعة وجوه مألوفة. هكذا أتذكرها دائمًا.", 3.6f)
            }
        }
    }

    private fun inspectKitchenJar(hotspot: Hotspot) {
        val moved = roomStateManager.hasPhysical(RoomId.KITCHEN, RoomStateManager.Flags.KITCHEN_ITEM_MOVED)
        if (moved) recordEvidenceOnce(RoomId.KITCHEN, RoomStateManager.Flags.KITCHEN_EVIDENCE_RECORDED, 1)
        tensionSystem.addStimulus(if (moved) 2.2f else 0.8f)
        showMessage(if (moved) "كان المرطبان قرب الطاولة... كيف وصل إلى هنا؟" else hotspot.message, 3.8f)
    }

    private fun inspectBedroomNote(hotspot: Hotspot) {
        val changed = roomStateManager.hasPhysical(RoomId.BEDROOM, RoomStateManager.Flags.BEDROOM_MESSAGE_CHANGED)
        if (changed && !roomStateManager.hasPhysical(RoomId.BEDROOM, RoomStateManager.Flags.BEDROOM_EVIDENCE_RECORDED)) {
            narrativeState.recordConfrontation(1)
            roomStateManager.markPhysical(RoomId.BEDROOM, RoomStateManager.Flags.BEDROOM_EVIDENCE_RECORDED)
        }
        tensionSystem.addStimulus(if (changed) 3f else 1f)
        showMessage(if (changed) "إذا عدت يومًا... أنت تعرف من ينتظرك." else hotspot.message, 4.2f)
    }

    private fun inspectBasementBox() {
        recordEvidenceOnce(RoomId.BASEMENT, RoomStateManager.Flags.BASEMENT_EVIDENCE_RECORDED, 2)
        tensionSystem.addStimulus(3f)
        showMessage("اسمي مكتوب على الصندوق من الخارج... وتحتَه كلمة: الزائر.", 4.8f)
    }

    private fun recordEvidenceOnce(roomId: RoomId, flag: String, weight: Int) {
        if (roomStateManager.hasPhysical(roomId, flag)) return
        narrativeState.recordEvidence(weight)
        roomStateManager.markPhysical(roomId, flag)
    }

    private fun collect(hotspot: Hotspot) {
        val newlyCollected = hotspot.memoryItemId?.let(collectedMemories::add) ?: false
        hiddenHotspots.add(hotspot.id)
        if (newlyCollected) narrativeState.recordEvidence(if (hotspot.id == "basement_tape") 2 else 1)
        when (hotspot.id) {
            "living_birthday_card" -> {
                roomStateManager.markPhysical(RoomId.LIVING_ROOM, RoomStateManager.Flags.LIVING_MEMORY_TAKEN)
                tensionSystem.addStimulus(5f)
                showMessage("ذكرى 2/5 — بطاقة عيد قديمة... توقيع خامس مطموس.", 4.5f)
            }
            "kitchen_chipped_cup" -> {
                roomStateManager.markPhysical(RoomId.KITCHEN, RoomStateManager.Flags.KITCHEN_MEMORY_TAKEN)
                tensionSystem.addStimulus(6f)
                showMessage("ذكرى 3/5 — خمسة خطوط على الكوب. لماذا خمسة؟", 4.5f)
            }
            "bedroom_letter_fragment" -> {
                roomStateManager.markPhysical(RoomId.BEDROOM, RoomStateManager.Flags.BEDROOM_MEMORY_TAKEN)
                tensionSystem.addStimulus(7f)
                showMessage("ذكرى 4/5 — القصاصة تحمل تاريخ الليلة التي غادرتُ فيها البيت.", 4.8f)
            }
            "basement_tape" -> {
                roomStateManager.markPhysical(RoomId.BASEMENT, RoomStateManager.Flags.BASEMENT_MEMORY_TAKEN)
                tensionSystem.addStimulus(10f)
                temporaryBlackoutSeconds = 1.2f
                haptic()
                showMessage("ذكرى 5/5 — التسجيل يقول: «إذا عاد... فهو الزائر هذه المرة.»", 5.5f)
            }
            else -> {
                tensionSystem.addStimulus(4f)
                showMessage(hotspot.message, 4f)
            }
        }
        refreshActiveHotspots()
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
        haptic()
        refreshActiveHotspots()
    }

    private fun updateGame(deltaSeconds: Float) {
        if (endingPath == null) {
            val desired = NormalizedPoint(
                player.position.x + inputX * player.speedPerSecond * deltaSeconds,
                player.position.y + inputY * player.speedPerSecond * deltaSeconds
            )
            player.position = collisionSystem.resolve(player.position, desired, player.radius)
            prompt = interactionSystem.nearest(player.position, activeHotspots, hiddenHotspots)?.label
            val inSafeLight = isNearSafeLight()
            tensionSystem.update(deltaSeconds, inDarkness = !inSafeLight, inSafeLight = inSafeLight)
            notifyTensionStageIfNeeded()

            horrorDirector.update(
                deltaSeconds = deltaSeconds,
                context = HorrorContext(
                    roomId = room.id,
                    visitCount = roomVisitCounts[room.id] ?: 1,
                    memoriesCollected = collectedMemories.size,
                    tensionLevel = tensionSystem.level
                ),
                events = HorrorEventCatalog.events,
                intensityMultiplier = tensionSystem.eventIntensityMultiplier
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
                tensionSystem.addStimulus(2.5f)
                showMessage("...هل كانت الصورة مائلة قبل قليل؟", 2.8f)
            }
            HorrorAction.LIVING_SINGLE_KNOCK -> {
                onSpatialEffectRequested?.invoke(NormalizedPoint(0.84f, 0.28f), player.position)
                audioSubtitle("[طرقة واحدة من جهة النافذة]")
                tensionSystem.addStimulus(1.5f)
            }
            HorrorAction.LIVING_LIGHT_DIM -> {
                temporaryBlackoutSeconds = 2.8f
                tensionSystem.addStimulus(2f)
            }
            HorrorAction.KITCHEN_FOOTSTEPS -> {
                onFootstepsRequested?.invoke(NormalizedPoint(0.88f, 0.23f), player.position)
                audioSubtitle("[خطوات بطيئة خلف الجدار]")
                tensionSystem.addStimulus(2f)
            }
            HorrorAction.KITCHEN_OBJECT_DROP -> {
                onDropRequested?.invoke(NormalizedPoint(0.70f, 0.24f), player.position)
                roomStateManager.markPhysical(RoomId.KITCHEN, RoomStateManager.Flags.KITCHEN_OBJECT_FALLEN)
                audioSubtitle("[صوت جسم يسقط في المطبخ]")
                tensionSystem.addStimulus(3f)
                haptic()
            }
            HorrorAction.KITCHEN_ITEM_MOVE -> {
                roomStateManager.markPhysical(RoomId.KITCHEN, RoomStateManager.Flags.KITCHEN_ITEM_MOVED)
                tensionSystem.addStimulus(2.5f)
                showMessage("شيء ما تغيّر في مكانه.", 2.6f)
                refreshActiveHotspots()
            }
            HorrorAction.BEDROOM_SHADOW -> {
                shadowSeconds = 2.2f
                tensionSystem.addStimulus(4f)
                haptic()
            }
            HorrorAction.BEDROOM_MESSAGE_CHANGE -> {
                roomStateManager.markPhysical(RoomId.BEDROOM, RoomStateManager.Flags.BEDROOM_MESSAGE_CHANGED)
                tensionSystem.addStimulus(3.5f)
            }
            HorrorAction.BEDROOM_DOOR_MOVE -> {
                roomStateManager.markPhysical(RoomId.BEDROOM, RoomStateManager.Flags.BEDROOM_DOOR_SHIFTED)
                onSpatialEffectRequested?.invoke(NormalizedPoint(0.87f, 0.73f), player.position)
                tensionSystem.addStimulus(2.5f)
            }
            HorrorAction.BEDROOM_WHISPER -> {
                onWhisperRequested?.invoke(NormalizedPoint(0.94f, 0.50f), player.position)
                audioSubtitle("[همسة قريبة: لا تنزل]")
                tensionSystem.addStimulus(3f)
            }
            HorrorAction.BASEMENT_CHAIN_RATTLE -> {
                onSpatialEffectRequested?.invoke(NormalizedPoint(0.74f, 0.22f), player.position)
                audioSubtitle("[صوت معدن يتحرك في الظلام]")
                tensionSystem.addStimulus(3f)
            }
            HorrorAction.BASEMENT_BLACKOUT -> {
                roomStateManager.markPhysical(RoomId.BASEMENT, RoomStateManager.Flags.BASEMENT_BLACKOUT_SEEN)
                temporaryBlackoutSeconds = 2.8f
                tensionSystem.addStimulus(5f)
                haptic()
            }
            HorrorAction.BASEMENT_PRESENCE -> {
                roomStateManager.markPhysical(RoomId.BASEMENT, RoomStateManager.Flags.BASEMENT_PRESENCE_SEEN)
                basementPresenceSeconds = 2.6f
                tensionSystem.addStimulus(7f)
                haptic()
            }
        }
        notifyTensionStageIfNeeded()
        if (event.oneShot) autosave()
    }

    private fun changeRoom(source: RoomId, target: RoomId) {
        roomStateManager.clearPerceived(source)
        room = RoomCatalog.room(target)
        collisionSystem = CollisionSystem(room.walkableBounds, room.collisionRects)
        roomVisitCounts[target] = (roomVisitCounts[target] ?: 0) + 1
        player.position = spawnFor(source, target)
        prompt = null
        refreshActiveHotspots()
        val text = when (target) {
            RoomId.ENTRANCE -> "عدت إلى المدخل."
            RoomId.LIVING_ROOM -> "دخلت الصالون. البيت أكثر هدوءًا مما ينبغي."
            RoomId.KITCHEN -> "دخلت المطبخ. لا شيء يتحرك... الآن."
            RoomId.BEDROOM -> "غرفة النوم ما زالت كما تركتها تقريبًا."
            RoomId.BASEMENT -> "نزلت إلى القبو. الهواء هنا أثقل."
        }
        showMessage(text, 3f)
    }

    private fun spawnFor(source: RoomId, target: RoomId): NormalizedPoint = when (target) {
        RoomId.ENTRANCE -> NormalizedPoint(0.80f, 0.50f)
        RoomId.LIVING_ROOM -> if (source == RoomId.KITCHEN) NormalizedPoint(0.80f, 0.72f) else NormalizedPoint(0.18f, 0.50f)
        RoomId.KITCHEN -> if (source == RoomId.BEDROOM) NormalizedPoint(0.80f, 0.52f) else NormalizedPoint(0.18f, 0.51f)
        RoomId.BEDROOM -> if (source == RoomId.BASEMENT) NormalizedPoint(0.80f, 0.72f) else NormalizedPoint(0.18f, 0.52f)
        RoomId.BASEMENT -> NormalizedPoint(0.18f, 0.50f)
    }

    private fun defaultSpawn(roomId: RoomId): NormalizedPoint = when (roomId) {
        RoomId.ENTRANCE, RoomId.LIVING_ROOM -> NormalizedPoint(0.18f, 0.50f)
        RoomId.KITCHEN -> NormalizedPoint(0.18f, 0.51f)
        RoomId.BEDROOM -> NormalizedPoint(0.18f, 0.52f)
        RoomId.BASEMENT -> NormalizedPoint(0.18f, 0.50f)
    }

    private fun refreshActiveHotspots() {
        val result = ArrayList<Hotspot>(room.hotspots.size)
        room.hotspots.forEach { hotspot ->
            if (room.id == RoomId.BASEMENT && hotspot.type == HotspotType.CHOICE) {
                val unlocked = RoomCatalog.basementMemory.id in collectedMemories
                val choiceMade = roomStateManager.hasPhysical(RoomId.BASEMENT, RoomStateManager.Flags.BASEMENT_CHOICE_MADE)
                if (!unlocked || choiceMade) return@forEach
            }
            if (room.id == RoomId.KITCHEN && hotspot.id == "kitchen_loose_jar" && roomStateManager.hasPhysical(RoomId.KITCHEN, RoomStateManager.Flags.KITCHEN_ITEM_MOVED)) {
                result.add(hotspot.copy(position = NormalizedPoint(0.30f, 0.72f)))
            } else {
                result.add(hotspot)
            }
        }
        activeHotspots = result
    }

    private fun isNearSafeLight(): Boolean {
        if (room.id != RoomId.ENTRANCE) return false
        val dx = player.position.x - 0.25f
        val dy = player.position.y - 0.55f
        return dx * dx + dy * dy <= 0.16f * 0.16f
    }

    private fun notifyTensionStageIfNeeded() {
        val currentStage = tensionSystem.stage
        if (currentStage != lastTensionStage) {
            lastTensionStage = currentStage
            onTensionStageChanged?.invoke(currentStage)
        }
    }

    private fun audioSubtitle(text: String) {
        if (subtitlesEnabled && message == null) showMessage(text, 2.0f)
    }

    private fun haptic() {
        if (vibrationEnabled) onHapticRequested?.invoke()
    }

    private fun autosave() {
        onAutosaveRequested?.invoke(createSaveData())
    }

    private fun showMessage(value: String, seconds: Float) {
        message = value
        messageSeconds = seconds
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.drawWorld(
            canvas = canvas,
            width = width,
            height = height,
            room = room,
            roomStateManager = roomStateManager,
            hiddenHotspots = hiddenHotspots,
            shadowSeconds = shadowSeconds,
            basementPresenceSeconds = basementPresenceSeconds
        )
        renderer.drawPlayer(canvas, width, height, player)
        lightingSystem.draw(
            canvas = canvas,
            width = width,
            height = height,
            normalizedX = player.position.x,
            normalizedY = player.position.y,
            tensionLevel = tensionSystem.level,
            extraDarkness = (temporaryBlackoutSeconds / 2.8f).coerceIn(0f, 1f)
        )
        renderer.drawHud(canvas, width, height, room.name, message, prompt, collectedMemories.size)
        endingPath?.let { renderer.drawEnding(canvas, width, height, it) }
    }
}
