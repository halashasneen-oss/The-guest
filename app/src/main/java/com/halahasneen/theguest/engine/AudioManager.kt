package com.halahasneen.theguest.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.halahasneen.theguest.R
import com.halahasneen.theguest.data.model.NormalizedPoint

class AudioManager(context: Context) {
    private val appContext = context.applicationContext
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val loadedSoundIds = mutableSetOf<Int>()
    private var knockSoundId = 0
    private var footstepsSoundId = 0
    private var dropSoundId = 0
    private var whisperSoundId = 0
    private var masterVolume = 1f
    private var effectsVolume = 0.75f
    private var ambientVolume = 0.22f
    private var tensionStage = TensionStage.CALM

    private val ambientPlayer: MediaPlayer? = MediaPlayer.create(appContext, R.raw.ambient_house)?.apply { isLooping = true }

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) loadedSoundIds.add(sampleId) }
        knockSoundId = soundPool.load(appContext, R.raw.soft_knock, 1)
        footstepsSoundId = soundPool.load(appContext, R.raw.kitchen_footsteps, 1)
        dropSoundId = soundPool.load(appContext, R.raw.kitchen_drop, 1)
        whisperSoundId = soundPool.load(appContext, R.raw.bedroom_whisper, 1)
        updateAmbientVolume()
    }

    fun setMasterVolume(value: Float) { masterVolume = value.coerceIn(0f, 1f); updateAmbientVolume() }
    fun setEffectsVolume(value: Float) { effectsVolume = value.coerceIn(0f, 1f) }
    fun setAmbientVolume(value: Float) { ambientVolume = value.coerceIn(0f, 1f); updateAmbientVolume() }
    fun setTensionStage(stage: TensionStage) { if (tensionStage != stage) { tensionStage = stage; updateAmbientVolume() } }
    fun resumeAmbient() { ambientPlayer?.let { updateAmbientVolume(); if (!it.isPlaying) it.start() } }
    fun pauseAmbient() { ambientPlayer?.let { if (it.isPlaying) it.pause() } }

    fun playKnock(source: NormalizedPoint, listener: NormalizedPoint) = playSpatial(knockSoundId, source, listener, 0.85f, 1f)
    fun playFootsteps(source: NormalizedPoint, listener: NormalizedPoint) = playSpatial(footstepsSoundId, source, listener, 0.95f, 0.9f)
    fun playDrop(source: NormalizedPoint, listener: NormalizedPoint) = playSpatial(dropSoundId, source, listener, 0.90f, 1f)
    fun playWhisper(source: NormalizedPoint, listener: NormalizedPoint) = playSpatial(whisperSoundId, source, listener, 0.70f, 0.8f)

    private fun playSpatial(soundId: Int, source: NormalizedPoint, listener: NormalizedPoint, maxDistance: Float, gain: Float) {
        if (soundId == 0 || soundId !in loadedSoundIds) return
        val stageBoost = when (tensionStage) {
            TensionStage.CALM -> 0.85f
            TensionStage.UNEASY -> 0.95f
            TensionStage.DISTURBED, TensionStage.TERRIFIED -> 1f
        }
        val volumes = AudioMath.spatialVolumes(source, listener, masterVolume * effectsVolume * stageBoost * gain, maxDistance)
        if (volumes.left <= 0f && volumes.right <= 0f) return
        soundPool.play(soundId, volumes.left, volumes.right, 1, 0, 1f)
    }

    fun release() { ambientPlayer?.release(); soundPool.release(); loadedSoundIds.clear() }

    private fun updateAmbientVolume() {
        val multiplier = when (tensionStage) {
            TensionStage.CALM -> 1f
            TensionStage.UNEASY -> 1.08f
            TensionStage.DISTURBED -> 1.17f
            TensionStage.TERRIFIED -> 1.28f
        }
        val volume = (masterVolume * ambientVolume * multiplier).coerceIn(0f, 1f)
        ambientPlayer?.setVolume(volume, volume)
    }
}
