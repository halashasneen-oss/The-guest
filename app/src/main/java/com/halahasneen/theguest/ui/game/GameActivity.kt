package com.halahasneen.theguest.ui.game

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import com.halahasneen.theguest.R
import com.halahasneen.theguest.engine.AudioManager
import com.halahasneen.theguest.ui.components.InteractionButtonView
import com.halahasneen.theguest.ui.components.JoystickView

class GameActivity : Activity() {
    private lateinit var gameCanvas: GameCanvasView
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        audioManager = AudioManager(applicationContext)
        gameCanvas = findViewById(R.id.gameCanvas)
        findViewById<JoystickView>(R.id.joystick).onDirectionChanged = gameCanvas::setInputDirection
        findViewById<InteractionButtonView>(R.id.interactionButton).onInteraction = gameCanvas::interact
        gameCanvas.onSpatialEffectRequested = audioManager::playKnock
        gameCanvas.onFootstepsRequested = audioManager::playFootsteps
        gameCanvas.onDropRequested = audioManager::playDrop
        gameCanvas.onTensionStageChanged = audioManager::setTensionStage
        enterImmersiveMode()
    }

    @Suppress("DEPRECATION")
    private fun enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (hasFocus) enterImmersiveMode() }
    override fun onResume() { super.onResume(); gameCanvas.resumeGame(); audioManager.resumeAmbient() }
    override fun onPause() { gameCanvas.pauseGame(); audioManager.pauseAmbient(); super.onPause() }
    override fun onDestroy() { audioManager.release(); super.onDestroy() }
}
