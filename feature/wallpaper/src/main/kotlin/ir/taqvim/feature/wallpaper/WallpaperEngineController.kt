/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.wallpaper

import android.os.Handler
import kotlin.time.Duration
import kotlin.time.Instant

/** Runs one action after a delay; a new schedule replaces the previous one. */
interface WallpaperTicker {
    fun schedule(
        delay: Duration,
        action: () -> Unit,
    )

    fun cancel()
}

/** [WallpaperTicker] on a [Handler] (the main thread of the wallpaper engine or the daydream). */
class HandlerWallpaperTicker(
    private val handler: Handler,
) : WallpaperTicker {
    private var pending: Runnable? = null

    override fun schedule(
        delay: Duration,
        action: () -> Unit,
    ) {
        cancel()
        val runnable = Runnable { action() }
        pending = runnable
        handler.postDelayed(runnable, delay.inWholeMilliseconds)
    }

    override fun cancel() {
        pending?.let(handler::removeCallbacks)
        pending = null
    }
}

/**
 * When the live wallpaper draws (T-1215), kept apart from the engine so it can be tested: a frame is requested only
 * while the wallpaper is visible and has a surface, and the next one is scheduled at the content's next change
 * ([WallpaperRedrawPolicy]). Hidden wallpapers hold no timer, so they cost nothing between screen-on sessions.
 */
class WallpaperEngineController(
    private val ticker: WallpaperTicker,
    private val requestFrame: () -> Unit,
) {
    private var visible = false
    private var surfaceReady = false

    /** Whether a frame can be drawn now. */
    val drawing: Boolean get() = visible && surfaceReady

    fun onVisibilityChanged(visible: Boolean) {
        this.visible = visible
        if (visible) frameIfDrawing() else ticker.cancel()
    }

    fun onSurfaceChanged() {
        surfaceReady = true
        frameIfDrawing()
    }

    fun onSurfaceDestroyed() {
        surfaceReady = false
        ticker.cancel()
    }

    /** A frame drawn at [now] shows content that changes at [nextChangeAt] (`null`: its content failed to load). */
    fun onFrameDrawn(
        now: Instant,
        nextChangeAt: Instant?,
    ) {
        if (drawing) ticker.schedule(WallpaperRedrawPolicy.delay(now, nextChangeAt), ::frameIfDrawing)
    }

    fun onDestroy() {
        visible = false
        surfaceReady = false
        ticker.cancel()
    }

    private fun frameIfDrawing() {
        if (drawing) {
            ticker.cancel()
            requestFrame()
        }
    }
}
