package com.halahasneen.theguest.data.repository

import com.halahasneen.theguest.data.model.EndingPath
import com.halahasneen.theguest.data.model.FinalChoice
import com.halahasneen.theguest.data.model.GameSaveData
import com.halahasneen.theguest.data.model.NormalizedPoint
import com.halahasneen.theguest.data.model.RoomId
import com.halahasneen.theguest.data.model.RoomSaveState
import org.json.JSONArray
import org.json.JSONObject

object GameSaveCodec {
    fun encode(save: GameSaveData): String {
        val root = JSONObject()
        root.put("saveVersion", GameSaveData.CURRENT_SAVE_VERSION)
        root.put("currentRoom", save.currentRoom.name)
        root.put("playerPosition", JSONObject().put("x", save.playerPosition.x).put("y", save.playerPosition.y))
        root.put("collectedMemories", stringArray(save.collectedMemories))
        root.put("hiddenHotspots", stringArray(save.hiddenHotspots))
        root.put("tensionLevel", save.tensionLevel.coerceIn(0f, 100f))

        val states = JSONObject()
        save.roomStates.forEach { (roomId, state) ->
            states.put(
                roomId.name,
                JSONObject()
                    .put("physicalFlags", stringArray(state.physicalFlags))
                    .put("perceivedFlags", stringArray(state.perceivedFlags))
            )
        }
        root.put("roomStates", states)

        val visits = JSONObject()
        save.roomVisitCounts.forEach { (roomId, count) -> visits.put(roomId.name, count.coerceAtLeast(0)) }
        root.put("roomVisitCounts", visits)

        root.put("acceptanceScore", save.acceptanceScore.coerceAtLeast(0))
        root.put("denialScore", save.denialScore.coerceAtLeast(0))
        root.put("curiosityScore", save.curiosityScore.coerceAtLeast(0))
        root.put("avoidanceScore", save.avoidanceScore.coerceAtLeast(0))
        root.put("finalChoice", save.finalChoice?.name ?: JSONObject.NULL)
        root.put("endingPath", save.endingPath?.name ?: JSONObject.NULL)
        root.put("triggeredEvents", stringArray(save.triggeredOneShots))
        return root.toString()
    }

    fun decode(json: String): GameSaveData? = runCatching {
        val root = JSONObject(json)
        val version = root.optInt("saveVersion", 0)
        if (version > GameSaveData.CURRENT_SAVE_VERSION) return null

        val room = enumValueOrDefault(root.optString("currentRoom"), RoomId.ENTRANCE)
        val positionJson = root.optJSONObject("playerPosition")
        val position = NormalizedPoint(
            positionJson?.optDouble("x", defaultSpawn(room).x.toDouble())?.toFloat() ?: defaultSpawn(room).x,
            positionJson?.optDouble("y", defaultSpawn(room).y.toDouble())?.toFloat() ?: defaultSpawn(room).y
        )

        val roomStatesJson = root.optJSONObject("roomStates")
        val roomStates = mutableMapOf<RoomId, RoomSaveState>()
        if (roomStatesJson != null) {
            val keys = roomStatesJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val roomId = enumValueOrNull<RoomId>(key) ?: continue
                val state = roomStatesJson.optJSONObject(key) ?: continue
                roomStates[roomId] = RoomSaveState(
                    physicalFlags = readStrings(state.optJSONArray("physicalFlags")),
                    perceivedFlags = readStrings(state.optJSONArray("perceivedFlags"))
                )
            }
        }

        val visitsJson = root.optJSONObject("roomVisitCounts")
        val visits = mutableMapOf<RoomId, Int>()
        if (visitsJson != null) {
            val keys = visitsJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val roomId = enumValueOrNull<RoomId>(key) ?: continue
                visits[roomId] = visitsJson.optInt(key, 0).coerceAtLeast(0)
            }
        }
        if (visits.isEmpty()) visits[RoomId.ENTRANCE] = 1

        GameSaveData(
            saveVersion = GameSaveData.CURRENT_SAVE_VERSION,
            currentRoom = room,
            playerPosition = NormalizedPoint(position.x.coerceIn(0f, 1f), position.y.coerceIn(0f, 1f)),
            collectedMemories = readStrings(root.optJSONArray("collectedMemories")),
            hiddenHotspots = readStrings(root.optJSONArray("hiddenHotspots")),
            tensionLevel = root.optDouble("tensionLevel", 0.0).toFloat().coerceIn(0f, 100f),
            roomStates = roomStates,
            roomVisitCounts = visits,
            acceptanceScore = root.optInt("acceptanceScore", 0).coerceAtLeast(0),
            denialScore = root.optInt("denialScore", 0).coerceAtLeast(0),
            curiosityScore = root.optInt("curiosityScore", 0).coerceAtLeast(0),
            avoidanceScore = root.optInt("avoidanceScore", 0).coerceAtLeast(0),
            finalChoice = enumValueOrNull<FinalChoice>(root.optNullableString("finalChoice")),
            endingPath = enumValueOrNull<EndingPath>(root.optNullableString("endingPath")),
            triggeredOneShots = readStrings(root.optJSONArray("triggeredEvents"))
        )
    }.getOrNull()

    private fun stringArray(values: Set<String>): JSONArray = JSONArray().also { array ->
        values.sorted().forEach { value -> array.put(value) }
    }

    private fun readStrings(array: JSONArray?): Set<String> {
        if (array == null) return emptySet()
        return buildSet {
            for (index in 0 until array.length()) {
                val value = array.optString(index)
                if (value.isNotBlank()) add(value)
            }
        }
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
        value?.let { candidate -> enumValues<T>().firstOrNull { it.name == candidate } }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        enumValueOrNull<T>(value) ?: default

    private fun defaultSpawn(room: RoomId): NormalizedPoint = when (room) {
        RoomId.ENTRANCE -> NormalizedPoint(0.18f, 0.50f)
        RoomId.LIVING_ROOM -> NormalizedPoint(0.18f, 0.50f)
        RoomId.KITCHEN -> NormalizedPoint(0.18f, 0.51f)
        RoomId.BEDROOM -> NormalizedPoint(0.18f, 0.52f)
        RoomId.BASEMENT -> NormalizedPoint(0.18f, 0.50f)
    }
}
