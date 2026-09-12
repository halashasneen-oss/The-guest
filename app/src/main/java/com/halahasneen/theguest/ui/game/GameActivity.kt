package com.halahasneen.theguest.ui.game

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import com.halahasneen.theguest.R
import com.halahasneen.theguest.data.repository.GameRepository
import com.halahasneen.theguest.data.repository.GameSettingsRepository
import com.halahasneen.theguest.engine.AudioManager
import com.halahasneen.theguest.ui.ImmersiveActivity
import com.halahasneen.theguest.ui.components.InteractionButtonView
import com.halahasneen.theguest.ui.components.JoystickView
import com.halahasneen.theguest.ui.menu.MainMenuActivity
import com.halahasneen.theguest.ui.settings.SettingsActivity

class GameActivity : ImmersiveActivity() {
    private lateinit var gameCanvas: GameCanvasView
    private lateinit var audioManager: AudioManager
    private lateinit var gameRepository: GameRepository
    private lateinit var settingsRepository: GameSettingsRepository
    private lateinit var pauseOverlay: View
    private var pausedByMenu = false
    private var initialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        enterImmersiveMode()

        gameRepository = GameRepository(applicationContext)
        settingsRepository = GameSettingsRepository(applicationContext)
        audioManager = AudioManager(applicationContext)
        gameCanvas = findViewById(R.id.gameCanvas)
        pauseOverlay = findViewById(R.id.pauseOverlay)

        findViewById<JoystickView>(R.id.joystick).onDirectionChanged = gameCanvas::setInputDirection
        findViewById<InteractionButtonView>(R.id.interactionButton).onInteraction = gameCanvas::interact
        gameCanvas.onSpatialEffectRequested = audioManager::playKnock
        gameCanvas.onFootstepsRequested = audioManager::playFootsteps
        gameCanvas.onDropRequested = audioManager::playDrop
        gameCanvas.onWhisperRequested = audioManager::playWhisper
        gameCanvas.onTensionStageChanged = audioManager::setTensionStage
        gameCanvas.onAutosaveRequested = gameRepository::save
        gameCanvas.onHapticRequested = {
            gameCanvas.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }

        findViewById<Button>(R.id.pauseButton).setOnClickListener { showPauseMenu() }
        findViewById<Button>(R.id.resumeButton).setOnClickListener { resumeFromPause() }
        findViewById<Button>(R.id.pauseSettingsButton).setOnClickListener {
            saveNow()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.mainMenuButton).setOnClickListener {
            saveNow()
            startActivity(Intent(this, MainMenuActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
        }

        val continueGame = intent.getBooleanExtra(EXTRA_CONTINUE, false)
        if (continueGame) {
            gameRepository.load()?.let(gameCanvas::restore)
        } else {
            gameRepository.clear()
        }
        applySettings()
        initialized = true
    }

    override fun onResume() {
        super.onResume()
        if (!initialized) return
        applySettings()
        if (!pausedByMenu) {
            gameCanvas.resumeGame()
            audioManager.resumeAmbient()
        }
        enterImmersiveMode()
    }

    override fun onPause() {
        if (initialized) {
            saveNow()
            gameCanvas.pauseGame()
            audioManager.pauseAmbient()
        }
        super.onPause()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (pausedByMenu) resumeFromPause() else showPauseMenu()
    }

    override fun onDestroy() {
        audioManager.release()
        super.onDestroy()
    }

    private fun showPauseMenu() {
        if (pausedByMenu) return
        pausedByMenu = true
        saveNow()
        gameCanvas.pauseGame()
        audioManager.pauseAmbient()
        pauseOverlay.visibility = View.VISIBLE
    }

    private fun resumeFromPause() {
        if (!pausedByMenu) return
        pausedByMenu = false
        pauseOverlay.visibility = View.GONE
        applySettings()
        gameCanvas.resumeGame()
        audioManager.resumeAmbient()
        enterImmersiveMode()
    }

    private fun saveNow() {
        gameRepository.save(gameCanvas.createSaveData())
    }

    private fun applySettings() {
        val settings = settingsRepository.load()
        audioManager.setMasterVolume(settings.masterVolume)
        audioManager.setEffectsVolume(settings.effectsVolume)
        audioManager.setAmbientVolume(settings.ambientVolume)
        gameCanvas.applySettings(settings)
    }

    companion object {
        const val EXTRA_CONTINUE = "continue_game"
    }
}
