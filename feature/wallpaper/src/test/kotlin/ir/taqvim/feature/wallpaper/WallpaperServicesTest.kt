/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.widget.TextClock
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.ui.painter.PainterEnvironment
import ir.taqvim.feature.wallpaper.WallpaperFixtures.NOW
import ir.taqvim.feature.wallpaper.WallpaperFixtures.SUNSET
import kotlin.time.Clock
import kotlin.time.Instant
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/** T-1215 (R): wallpaper smoke (engine, renderer, surface frames) and the daydream's attach and detach. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WallpaperServicesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** A surface that draws into a bitmap and counts posted frames. */
    private class BitmapHolder(
        width: Int,
        height: Int,
    ) : SurfaceHolder {
        val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        var posted = 0

        override fun lockCanvas(): Canvas = Canvas(bitmap)

        override fun lockCanvas(dirty: Rect?): Canvas = Canvas(bitmap)

        override fun unlockCanvasAndPost(canvas: Canvas) {
            posted++
        }

        override fun addCallback(callback: SurfaceHolder.Callback?) = Unit

        override fun removeCallback(callback: SurfaceHolder.Callback?) = Unit

        override fun isCreating(): Boolean = false

        @Deprecated("Deprecated in Java")
        override fun setType(type: Int) = Unit

        override fun setFixedSize(
            width: Int,
            height: Int,
        ) = Unit

        override fun setSizeFromLayout() = Unit

        override fun setFormat(format: Int) = Unit

        override fun setKeepScreenOn(screenOn: Boolean) = Unit

        override fun getSurface(): Surface? = null

        override fun getSurfaceFrame(): Rect = Rect(0, 0, bitmap.width, bitmap.height)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun waitFor(condition: () -> Boolean) {
        repeat(ATTEMPTS) {
            if (condition()) return
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(POLL_MILLIS)
        }
        condition() shouldBe true
    }

    @Test
    fun theRendererDrawsDayAndNightInBothOrientations() {
        val renderer = WallpaperRenderer(PainterEnvironment(density = 2f, rtl = true))
        listOf(true, false).forEach { daylight ->
            listOf(360 to 640, 640 to 360).forEach { (width, height) ->
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                renderer.draw(Canvas(bitmap), WallpaperFixtures.content(daylight), width, height)
                (bitmap.getPixel(width / 2, 1) != 0) shouldBe true
            }
        }
    }

    @Test
    fun aVisibleWallpaperEnginePostsAFrameAndAHiddenOneDoesNot() {
        startKoin {
            modules(
                module { single { WallpaperContentSource { WallpaperFixtures.content(daylight = false) } } },
            )
        }
        val service = Robolectric.buildService(TaqvimWallpaperService::class.java).create().get()
        val engine = service.onCreateEngine() as TaqvimWallpaperService.CalendarEngine
        val holder = BitmapHolder(360, 640)

        engine.onVisibilityChanged(false)
        engine.onSurfaceChanged(holder, 0, 360, 640)
        shadowOf(Looper.getMainLooper()).idle()
        holder.posted shouldBe 0

        engine.onVisibilityChanged(true)
        waitFor { engine.frames == 1 }
        holder.posted shouldBe 1
        (holder.bitmap.getPixel(180, 1) != 0) shouldBe true

        engine.onSurfaceDestroyed(holder)
        engine.onDestroy()
    }

    @Test
    fun withoutContentTheEngineDrawsNothing() {
        val service = Robolectric.buildService(TaqvimWallpaperService::class.java).create().get()
        val engine = service.onCreateEngine() as TaqvimWallpaperService.CalendarEngine
        val holder = BitmapHolder(100, 100)

        engine.onVisibilityChanged(true)
        engine.onSurfaceChanged(holder, 0, 100, 100)
        repeat(FAILING_POLLS) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(POLL_MILLIS)
        }

        engine.frames shouldBe 0
        engine.onDestroy()
    }

    @Test
    fun theDaydreamShowsTheClockAndDatesWhileAttachedAndStopsWhenDetached() {
        val ticker = RecordingTicker()
        val clock =
            object : Clock {
                override fun now(): Instant = NOW
            }
        val source = DreamContentSource { DreamContent("Sunday 22 Shahrivar 1405", "13 September 2026", SUNSET) }
        val session = DreamSession(context, source, ticker, clock)
        var shown: View? = null

        session.attach { shown = it }

        val root = shown.shouldNotBeNull()
        session.view shouldBe root
        root.findViewWithTag<TextClock>(DreamClockViews.TAG_CLOCK).shouldNotBeNull()
        waitFor { root.findViewWithTag<TextView>(DreamClockViews.TAG_DATE).text.isNotEmpty() }
        root.findViewWithTag<TextView>(DreamClockViews.TAG_OTHER_DATES).text.toString() shouldBe "13 September 2026"
        ticker.delays shouldBe listOf(SUNSET - NOW)

        session.detach()
        session.view.shouldBeNull()
        ticker.hasPending shouldBe false
    }

    @Test
    fun withoutDatesTheDaydreamShowsOnlyTheClock() {
        val root = DreamClockViews.create(context)

        DreamClockViews.bind(root, null)

        root.findViewWithTag<TextView>(DreamClockViews.TAG_DATE).visibility shouldBe View.GONE
        root.findViewWithTag<TextView>(DreamClockViews.TAG_OTHER_DATES).visibility shouldBe View.GONE
        Robolectric.buildService(TaqvimDreamService::class.java).create().destroy()
    }

    private companion object {
        const val ATTEMPTS = 300
        const val FAILING_POLLS = 10
        const val POLL_MILLIS = 10L
    }
}
