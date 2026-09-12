package com.halahasneen.theguest.ui.menu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.halahasneen.theguest.R
import com.halahasneen.theguest.data.repository.GameRepository
import com.halahasneen.theguest.ui.ImmersiveActivity
import com.halahasneen.theguest.ui.credits.CreditsActivity
import com.halahasneen.theguest.ui.game.GameActivity
import com.halahasneen.theguest.ui.settings.SettingsActivity

class MainMenuActivity : ImmersiveActivity() {
    private lateinit var gameRepository: GameRepository
    private lateinit var continueButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
        enterImmersiveMode()

        gameRepository = GameRepository(applicationContext)
        continueButton = findViewById(R.id.continueButton)
        continueButton.setOnClickListener { startGame(continueGame = true) }
        findViewById<Button>(R.id.newGameButton).setOnClickListener {
            gameRepository.clear()
            startGame(continueGame = false)
        }
        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.creditsButton).setOnClickListener {
            startActivity(Intent(this, CreditsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        continueButton.isEnabled = gameRepository.hasValidSave()
        continueButton.alpha = if (continueButton.isEnabled) 1f else 0.42f
        enterImmersiveMode()
    }

    private fun startGame(continueGame: Boolean) {
        startActivity(
            Intent(this, GameActivity::class.java)
                .putExtra(GameActivity.EXTRA_CONTINUE, continueGame)
        )
    }
}
