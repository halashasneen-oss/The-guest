package com.halahasneen.theguest.engine

import android.view.Choreographer

class GameLoop(
    private val onUpdate: (Float) -> Unit,
    private val onRenderRequested: () -> Unit
) : Choreographer.FrameCallback {

    private var running = false
    private var lastFrameNanos = 0L

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        if (lastFrameNanos != 0L) {
            val rawDelta = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
            onUpdate(rawDelta.coerceIn(0f, MAX_DELTA_SECONDS))
        }
        lastFrameNanos = frameTimeNanos
        onRenderRequested()
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun start() {
        if (running) return
        running = true
        lastFrameNanos = 0L
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stop() {
        if (!running) return
        running = false
        lastFrameNanos = 0L
        Choreographer.getInstance().removeFrameCallback(this)
    }

    companion object {
        const val MAX_DELTA_SECONDS = 0.05f
    }
}
