package com.halahasneen.theguest.engine

import com.halahasneen.theguest.data.model.RoomId

data class RoomRuntimeState(
    val physicalFlags: MutableSet<String> = mutableSetOf(),
    val perceivedFlags: MutableSet<String> = mutableSetOf()
)

class RoomStateManager {
    private val states = mutableMapOf<RoomId, RoomRuntimeState>()

    fun state(roomId: RoomId): RoomRuntimeState = states.getOrPut(roomId) { RoomRuntimeState() }
    fun markPhysical(roomId: RoomId, flag: String) { state(roomId).physicalFlags.add(flag) }
    fun hasPhysical(roomId: RoomId, flag: String): Boolean = flag in state(roomId).physicalFlags
    fun markPerceived(roomId: RoomId, flag: String) { state(roomId).perceivedFlags.add(flag) }
    fun hasPerceived(roomId: RoomId, flag: String): Boolean = flag in state(roomId).perceivedFlags
    fun clearPerceived(roomId: RoomId) { state(roomId).perceivedFlags.clear() }

    object Flags {
        const val LIVING_PICTURE_TILTED = "living_picture_tilted"
        const val LIVING_EXTRA_PERSON = "living_extra_person"
        const val LIVING_MEMORY_TAKEN = "living_memory_taken"
        const val KITCHEN_OBJECT_FALLEN = "kitchen_object_fallen"
        const val KITCHEN_ITEM_MOVED = "kitchen_item_moved"
        const val KITCHEN_MEMORY_TAKEN = "kitchen_memory_taken"
        const val BEDROOM_MESSAGE_CHANGED = "bedroom_message_changed"
        const val BEDROOM_DOOR_SHIFTED = "bedroom_door_shifted"
        const val BEDROOM_MEMORY_TAKEN = "bedroom_memory_taken"
        const val BASEMENT_MEMORY_TAKEN = "basement_memory_taken"
        const val BASEMENT_BLACKOUT_SEEN = "basement_blackout_seen"
        const val BASEMENT_PRESENCE_SEEN = "basement_presence_seen"
        const val BASEMENT_CHOICE_MADE = "basement_choice_made"
    }
}
