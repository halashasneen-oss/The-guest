package com.halahasneen.theguest.engine

import com.halahasneen.theguest.data.model.HorrorContext
import com.halahasneen.theguest.data.model.HorrorEvent
import com.halahasneen.theguest.data.model.HorrorEventCategory
import kotlin.random.Random

class HorrorDirector(
    private val nextRandomFloat: () -> Float = { Random.nextFloat() }
) {
    private val triggeredOneShots = mutableSetOf<String>()
    private val eventCooldowns = mutableMapOf<String, Float>()
    private var lastEventId: String? = null
    private var lastCategory: HorrorEventCategory? = null
    private var evaluationAccumulator = 0f
    private var globalCooldown = 0f

    fun update(
        deltaSeconds: Float,
        context: HorrorContext,
        events: List<HorrorEvent>,
        intensityMultiplier: Float
    ): HorrorEvent? {
        val dt = deltaSeconds.coerceIn(0f, 1f)
        advanceCooldowns(dt)
        evaluationAccumulator += dt
        if (evaluationAccumulator < EVALUATION_INTERVAL_SECONDS) return null
        evaluationAccumulator = 0f
        if (globalCooldown > 0f) return null

        val eligible = eligibleEvents(context, events)
        if (eligible.isEmpty()) return null

        val attemptChance = (BASE_ATTEMPT_CHANCE * intensityMultiplier).coerceIn(0.06f, 0.36f)
        if (nextRandomFloat().coerceIn(0f, 0.999999f) > attemptChance) return null

        val selected = weightedSelect(eligible, intensityMultiplier) ?: return null
        record(selected)
        return selected
    }

    fun eligibleEvents(context: HorrorContext, events: List<HorrorEvent>): List<HorrorEvent> =
        events.filter { event ->
            event.roomId == context.roomId &&
                context.visitCount >= event.minVisitCount &&
                context.memoriesCollected >= event.minMemories &&
                context.tensionLevel >= event.minTension &&
                event.id != lastEventId &&
                !(event.oneShot && event.id in triggeredOneShots) &&
                (eventCooldowns[event.id] ?: 0f) <= 0f &&
                !(lastCategory == HorrorEventCategory.STRONG && event.category == HorrorEventCategory.STRONG)
        }

    fun triggeredOneShots(): Set<String> = triggeredOneShots.toSet()

    fun restoreTriggeredOneShots(ids: Set<String>) {
        triggeredOneShots.clear()
        triggeredOneShots.addAll(ids)
        eventCooldowns.clear()
        lastEventId = null
        lastCategory = null
        evaluationAccumulator = 0f
        globalCooldown = 0f
    }

    private fun weightedSelect(events: List<HorrorEvent>, intensityMultiplier: Float): HorrorEvent? {
        if (events.isEmpty()) return null
        val weights = events.map { event ->
            val categoryMultiplier = when (event.category) {
                HorrorEventCategory.SUBTLE -> 1f
                HorrorEventCategory.AUDIO -> intensityMultiplier.coerceIn(0.8f, 1.25f)
                HorrorEventCategory.VISUAL -> intensityMultiplier.coerceIn(0.7f, 1.35f)
                HorrorEventCategory.STRONG -> (intensityMultiplier - 0.2f).coerceIn(0.45f, 1.25f)
            }
            (event.weight * categoryMultiplier).coerceAtLeast(0f)
        }
        val totalWeight = weights.sum()
        if (totalWeight <= 0f) return null

        var roll = nextRandomFloat().coerceIn(0f, 0.999999f) * totalWeight
        events.indices.forEach { index ->
            roll -= weights[index]
            if (roll <= 0f) return events[index]
        }
        return events.last()
    }

    private fun record(event: HorrorEvent) {
        lastEventId = event.id
        lastCategory = event.category
        if (event.oneShot) triggeredOneShots.add(event.id)
        eventCooldowns[event.id] = event.cooldownSeconds.coerceAtLeast(0f)
        globalCooldown = when (event.category) {
            HorrorEventCategory.SUBTLE -> 10f
            HorrorEventCategory.AUDIO -> 14f
            HorrorEventCategory.VISUAL -> 18f
            HorrorEventCategory.STRONG -> 40f
        }
    }

    private fun advanceCooldowns(deltaSeconds: Float) {
        globalCooldown = (globalCooldown - deltaSeconds).coerceAtLeast(0f)
        val iterator = eventCooldowns.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val remaining = entry.value - deltaSeconds
            if (remaining <= 0f) iterator.remove() else entry.setValue(remaining)
        }
    }

    private companion object {
        const val EVALUATION_INTERVAL_SECONDS = 4f
        const val BASE_ATTEMPT_CHANCE = 0.18f
    }
}
