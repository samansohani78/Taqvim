/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.wallpaper

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.view.View
import ir.taqvim.core.ui.painter.PainterEnvironment
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

/**
 * Live wallpaper (T-1215): today's date, the month and the Moon on a day or night sky. It draws a single frame when it
 * becomes visible and again only when its content changes; there is no animation. Only the system can bind it
 * (`BIND_WALLPAPER`).
 */
class TaqvimWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = CalendarEngine()

    /** The engine of one placed wallpaper (home and lock screen previews get their own). */
    inner class CalendarEngine : Engine() {
        private val scope = MainScope()
        private val controller =
            WallpaperEngineController(HandlerWallpaperTicker(Handler(Looper.getMainLooper())), ::drawFrame)
        private var holder: SurfaceHolder? = null
        private var renderer: WallpaperRenderer? = null
        private var width = 0
        private var height = 0

        /** How many frames were posted to the surface. */
        var frames: Int = 0
            private set

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            controller.onVisibilityChanged(visible)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int,
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            this.holder = holder
            this.width = width
            this.height = height
            renderer = WallpaperRenderer(environmentOf(this@TaqvimWallpaperService))
            controller.onSurfaceChanged()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            controller.onSurfaceDestroyed()
            this.holder = null
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            controller.onDestroy()
            scope.cancel()
            super.onDestroy()
        }

        private fun drawFrame() {
            val source = GlobalContext.getOrNull()?.getOrNull<WallpaperContentSource>()
            scope.launch {
                val now = Clock.System.now()
                val content =
                    source?.let {
                        withContext(Dispatchers.Default) { WallpaperRedrawPolicy.loadOrNull { it.load(now) } }
                    }
                if (content != null && post(content)) frames++
                controller.onFrameDrawn(now, content?.nextChangeAt)
            }
        }

        /** Draws [content] on the surface; the canvas is always posted back, even when drawing fails. */
        private fun post(content: WallpaperContent): Boolean {
            val surface = holder?.takeIf { width > 0 && height > 0 }
            val current = renderer
            val canvas = if (current != null) surface?.lockCanvas() else null
            if (surface == null || current == null || canvas == null) return false
            val drawn = runCatching { current.draw(canvas, content, width, height) }
            surface.unlockCanvasAndPost(canvas)
            drawn.getOrThrow()
            return true
        }
    }

    internal companion object {
        /** Density, font scale and writing direction of [context]. */
        fun environmentOf(context: Context): PainterEnvironment {
            val resources = context.resources
            return PainterEnvironment(
                density = resources.displayMetrics.density,
                fontScale = resources.configuration.fontScale,
                rtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL,
            )
        }
    }
}
