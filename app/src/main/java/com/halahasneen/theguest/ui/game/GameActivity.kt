package com.halahasneen.theguest.ui.game

import android.app.Activity
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import com.halahasneen.theguest.R
import com.halahasneen.theguest.ui.components.JoystickView

class GameActivity : Activity() {
    private lateinit var gameCanvas: GameCanvasView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        gameCanvas = findViewById(R.id.gameCanvas)
        findViewById<JoystickView>(R.id.joystick).onDirectionChanged = gameCanvas::setInputDirection
        window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onResume() {
        super.onResume()
        gameCanvas.resumeGame()
    }

    override fun onPause() {
        gameCanvas.pauseGame()
        super.onPause()
    }
}
