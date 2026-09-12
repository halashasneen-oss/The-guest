package com.halahasneen.theguest.ui.settings

import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import com.halahasneen.theguest.R
import com.halahasneen.theguest.data.model.GameSettings
import com.halahasneen.theguest.data.repository.GameSettingsRepository
import com.halahasneen.theguest.ui.ImmersiveActivity

class SettingsActivity : ImmersiveActivity() {
    private lateinit var repository: GameSettingsRepository
    private lateinit var master: SeekBar
    private lateinit var effects: SeekBar
    private lateinit var ambient: SeekBar
    private lateinit var vibration: Switch
    private lateinit var subtitles: Switch
    private var bindingValues = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        enterImmersiveMode()
        repository = GameSettingsRepository(applicationContext)

        master = findViewById(R.id.masterVolume)
        effects = findViewById(R.id.effectsVolume)
        ambient = findViewById(R.id.ambientVolume)
        vibration = findViewById(R.id.vibrationSwitch)
        subtitles = findViewById(R.id.subtitlesSwitch)

        bind(repository.load())
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !bindingValues) persist()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        }
        master.setOnSeekBarChangeListener(listener)
        effects.setOnSeekBarChangeListener(listener)
        ambient.setOnSeekBarChangeListener(listener)
        vibration.setOnCheckedChangeListener { _, _ -> if (!bindingValues) persist() }
        subtitles.setOnCheckedChangeListener { _, _ -> if (!bindingValues) persist() }
        findViewById<Button>(R.id.settingsBackButton).setOnClickListener { finish() }
    }

    private fun bind(settings: GameSettings) {
        bindingValues = true
        master.progress = (settings.masterVolume * 100f).toInt()
        effects.progress = (settings.effectsVolume * 100f).toInt()
        ambient.progress = (settings.ambientVolume * 100f).toInt()
        vibration.isChecked = settings.vibrationEnabled
        subtitles.isChecked = settings.subtitlesEnabled
        bindingValues = false
    }

    private fun persist() {
        repository.save(
            GameSettings(
                masterVolume = master.progress / 100f,
                effectsVolume = effects.progress / 100f,
                ambientVolume = ambient.progress / 100f,
                vibrationEnabled = vibration.isChecked,
                subtitlesEnabled = subtitles.isChecked
            )
        )
    }
}
