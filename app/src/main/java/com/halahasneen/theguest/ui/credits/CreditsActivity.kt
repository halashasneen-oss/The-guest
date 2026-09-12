package com.halahasneen.theguest.ui.credits

import android.os.Bundle
import android.widget.Button
import com.halahasneen.theguest.R
import com.halahasneen.theguest.ui.ImmersiveActivity

class CreditsActivity : ImmersiveActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credits)
        enterImmersiveMode()
        findViewById<Button>(R.id.creditsBackButton).setOnClickListener { finish() }
    }
}
