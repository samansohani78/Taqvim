/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import android.app.Activity
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.renderer.TileRenderer
import com.github.takahirom.roborazzi.captureRoboImage
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.toJdn
import java.util.concurrent.Executor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * T-1600: the month and next tiles rendered by the ProtoLayout renderer on a small round watch, with a screenshot of
 * each. The same layouts are rendered on a real watch by `WearTileDeviceTest` (`:wear:wearApi34DebugAndroidTest`).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "fa-w227dp-h227dp-small-round-watch-xhdpi")
class WearTileScreenshotTest {
    private val setup = WearFixtures.setup()
    private val today = WearFixtures.calculator.today(setup, WearFixtures.NOWRUZ_MORNING)
    private val month =
        WearMonthBuilder.build(
            setup,
            WearFixtures.calculator.lookup(setup.islamicVariant),
            WearFixtures.NOWRUZ_MORNING.toJdn(setup.zone),
        )

    @Test
    fun `the month tile renders the month title, the weekday initials and every day`() {
        val view = render(TileLayouts.month(month))

        texts(view).first() shouldBe month.title
        texts(view) shouldContainAll
            month.weeks
                .flatten()
                .filterNotNull()
                .map { it.label }
        view.captureRoboImage("src/test/screenshots/wear_tile_month/round_fa.png")
    }

    @Test
    fun `the next tile renders today and the next prayer`() {
        val context = Robolectric.buildActivity(Activity::class.java).setup().get()
        val lines = nextLines(context, today)
        val view = render(TileLayouts.next(today.primaryDate, lines))

        texts(view) shouldBe listOf(today.primaryDate) + lines
        view.captureRoboImage("src/test/screenshots/wear_tile_next/round_fa.png")
    }

    /** Inflates [element] the way the system renders a tile, into a watch-sized parent that is measured and laid out. */
    private fun render(element: LayoutElementBuilders.LayoutElement): View {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val parent = FrameLayout(activity)
        activity.setContentView(parent)
        val renderer = TileRenderer(activity, Executor(Runnable::run)) { }
        val layout =
            TimelineBuilders.Timeline
                .fromLayoutElement(element)
                .timelineEntries
                .single()
                .layout
        val resources =
            ResourceBuilders.Resources
                .Builder()
                .setVersion(TileLayouts.RESOURCES_VERSION)
                .build()
        val inflated = renderer.inflateAsync(requireNotNull(layout), resources, parent)
        shadowOf(Looper.getMainLooper()).idle()
        checkNotNull(inflated.get()) { "the renderer returned no view" }
        val width = View.MeasureSpec.makeMeasureSpec(WATCH_PIXELS, View.MeasureSpec.EXACTLY)
        parent.measure(width, width)
        parent.layout(0, 0, WATCH_PIXELS, WATCH_PIXELS)
        return parent
    }

    /** Every text of the rendered view, in layout order. */
    private fun texts(view: View): List<String> =
        when (view) {
            is TextView -> listOf(view.text.toString())
            is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
            else -> emptyList()
        }

    private companion object {
        /** 227 dp at xhdpi: the small round watch of the screenshot qualifiers. */
        const val WATCH_PIXELS = 454
    }
}
