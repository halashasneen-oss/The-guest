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
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val loadedSoundIds = mutableSetOf<Int>()
    private var knockSoundId: Int = 0
    private var masterVolume = 1f
    private var effectsVolume = 0.75f
    private var ambientVolume = 0.22f
    private var tensionStage = TensionStage.CALM

    private val ambientPlayer: MediaPlayer? = MediaPlayer.create(appContext, R.raw.ambient_house)?.apply {
        isLooping = true
    }

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedSoundIds.add(sampleId)
        }
        knockSoundId = soundPool.load(appContext, R.raw.soft_knock, 1)
        updateAmbientVolume()
    }

    fun setMasterVolume(value: Float) {
        masterVolume = value.coerceIn(0f, 1f)
        updateAmbientVolume()
    }

    fun setEffectsVolume(value: Float) {
        effectsVolume = value.coerceIn(0f, 1f)
    }

    fun setAmbientVolume(value: Float) {
        ambientVolume = value.coerceIn(0f, 1f)
        updateAmbientVolume()
    }

    fun setTensionStage(stage: TensionStage) {
        if (tensionStage == stage) return
        tensionStage = stage
        updateAmbientVolume()
    }

    fun resumeAmbient() {
        ambientPlayer?.let { player ->
            updateAmbientVolume()
            if (!player.isPlaying) player.start()
        }
    }

    fun pauseAmbient() {
        ambientPlayer?.let { player ->
            if (player.isPlaying) player.pause()
        }
    }

    fun playKnock(source: NormalizedPoint, listener: NormalizedPoint) {
        if (knockSoundId == 0 || knockSoundId !in loadedSoundIds) return
        val stageBoost = when (tensionStage) {
            TensionStage.CALM -> 0.85f
            TensionStage.UNEASY -> 0.95f
            TensionStage.DISTURBED -> 1f
            TensionStage.TERRIFIED -> 1f
        }
        val volumes = AudioMath.spatialVolumes(
            source = source,
            listener = listener,
            baseVolume = masterVolume * effectsVolume * stageBoost,
            maxDistance = 0.85f
        )
        if (volumes.left <= 0f && volumes.right <= 0f) return
        soundPool.play(knockSoundId, volumes.left, volumes.right, 1, 0, 1f)
    }

    fun release() {
        ambientPlayer?.release()
        soundPool.release()
        loadedSoundIds.clear()
    }

    private fun updateAmbientVolume() {
        val tensionMultiplier = when (tensionStage) {
            TensionStage.CALM -> 1f
            TensionStage.UNEASY -> 1.08f
            TensionStage.DISTURBED -> 1.17f
            TensionStage.TERRIFIED -> 1.28f
        }
        val volume = (masterVolume * ambientVolume * tensionMultiplier).coerceIn(0f, 1f)
        ambientPlayer?.setVolume(volume, volume)
    }
}
