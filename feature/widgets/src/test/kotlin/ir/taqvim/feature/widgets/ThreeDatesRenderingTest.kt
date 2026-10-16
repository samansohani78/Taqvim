/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.github.takahirom.roborazzi.captureRoboImage
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real RemoteViews inflation, not just the Glance semantics tree. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class ThreeDatesRenderingTest {
    @Test
    @Config(qualifiers = "en-mdpi")
    fun english(): Unit = capture("en", listOf("18 Mehr 1405", "10 October 2026", "28 Rabiʻ II 1448"), SATURDAY_EN)

    @Test
    @Config(qualifiers = "fa-mdpi")
    fun persianWithLatinGregorian(): Unit =
        capture("fa", listOf("۱۸ مهر ۱۴۰۵", "10 October 2026", "۲۸ ربیع‌الثانی ۱۴۴۸"), SATURDAY_FA)

    @Test
    @Config(qualifiers = "ar-mdpi")
    fun arabic(): Unit = capture("ar", listOf("١٨ مهر ١٤٠٥", "١٠ أكتوبر ٢٠٢٦", "٢٨ ربيع الآخر ١٤٤٨"), SATURDAY_AR)

    @Test
    @Config(qualifiers = "en-mdpi")
    fun englishLargeText(): Unit =
        capture(
            "en_fs200",
            listOf("18 Mehr 1405", "10 October 2026", "28 Rabiʻ II 1448"),
            SATURDAY_EN,
            2f,
            expectedLines = ONE_DATE_ONLY,
        )

    @Test
    @Config(qualifiers = "fa-mdpi")
    fun persianLargeText(): Unit =
        capture(
            "fa_fs200",
            listOf("۱۸ مهر ۱۴۰۵", "10 October 2026", "۲۸ ربیع‌الثانی ۱۴۴۸"),
            SATURDAY_FA,
            2f,
            expectedLines = ONE_DATE_ONLY,
        )

    @Test
    @Config(qualifiers = "ar-mdpi")
    fun arabicLargeText(): Unit =
        capture(
            "ar_fs200",
            listOf("١٨ مهر ١٤٠٥", "١٠ أكتوبر ٢٠٢٦", "٢٨ ربيع الآخر ١٤٤٨"),
            SATURDAY_AR,
            2f,
            expectedLines = ONE_DATE_ONLY,
        )

    /** The widget resized up to three cells still has to lay out for that span, not for its default. */
    @Test
    @Config(qualifiers = "en-mdpi")
    fun englishResizedToThreeCells(): Unit =
        capture(
            "en_3x3",
            listOf("18 Mehr 1405", "10 October 2026", "28 Rabiʻ II 1448"),
            SATURDAY_EN,
            size = WidgetSize.MEDIUM_LARGE,
        )

    private fun capture(
        language: String,
        dates: List<String>,
        weekday: String,
        fontScale: Float = 1f,
        size: WidgetSize = WidgetSize.MEDIUM,
        expectedLines: Int = WEEKDAY_AND_THREE_DATES,
    ) {
        val widthDp = size.widthDp
        val heightDp = size.heightDp
        RuntimeEnvironment.setFontScale(fontScale)
        val context = Robolectric.buildActivity(Activity::class.java).setup().get()
        val config = WidgetConfig.defaultFor(WidgetKind.THREE_DATES)
        val data =
            WidgetSamples
                .data()
                .copy(calendarDates = persistentListOf(*dates.toTypedArray()), weekday = weekday)
        val remoteViews =
            runBlocking {
                GlanceRemoteViews()
                    .compose(context, DpSize(widthDp.dp, heightDp.dp)) {
                        GlanceTheme {
                            WidgetFrame(config, onClick = null) { style ->
                                ThreeDatesWidget().Content(data, config, style, size)
                            }
                        }
                    }.remoteViews
            }
        val parent = FrameLayout(context)
        context.setContentView(parent)
        parent.addView(remoteViews.apply(context, parent))
        parent.measure(exactly(widthDp), exactly(heightDp))
        parent.layout(0, 0, widthDp, heightDp)
        val bitmap = Bitmap.createBitmap(widthDp, heightDp, Bitmap.Config.ARGB_8888)
        parent.draw(Canvas(bitmap))
        bitmap.captureRoboImage("src/test/screenshots/three_dates/$language.png")
        assertDatesFit(parent, heightDp, expectedLines)
        if (fontScale == 1f) assertDatesAreNotWrapped(parent)
    }

    private fun exactly(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)

    /**
     * What is shown fits — whatever that turns out to be.
     *
     * The widget drops calendars rather than clip them when two cells cannot hold three dates at a large font
     * scale, so the count is an outcome, not a constant: [expectedLines] says what this case should produce and
     * the rest of the assertions hold regardless. Asserting a fixed count here would have made the degradation
     * look like a failure and the clipping it replaced look like a pass.
     *
     * The count includes the weekday, which the widget drops first when the box is crowded.
     */
    private fun assertDatesFit(
        parent: FrameLayout,
        heightDp: Int,
        expectedLines: Int,
    ) {
        val labels = texts(parent).filter { it.text.isNotBlank() }
        labels.size shouldBe expectedLines
        labels.forEach { label ->
            val bounds = Rect()
            label.getDrawingRect(bounds)
            parent.offsetDescendantRectToMyCoords(label, bounds)
            check(bounds.top >= 0 && bounds.bottom <= heightDp) { "Clipped date: ${label.text}, $bounds" }
            val layout = requireNotNull(label.layout)
            check(layout.height <= label.height) {
                "Vertically truncated date: ${label.text}; " +
                    "layout=${layout.height}, view=${label.height}, bounds=$bounds"
            }
            (0 until layout.lineCount).forEach { line -> layout.getEllipsisCount(line) shouldBe 0 }
        }
    }

    /**
     * At the default font scale every date is one line.
     *
     * [assertDatesFit] only refuses clipping and ellipsis, and a wrapped headline satisfies both while looking
     * like a mistake: raising the primary to 30 sp split "18 Mehr 1405" across two lines and the suite stayed
     * green. Wrapping is still allowed at large font scales, where fitting the text matters more than its shape.
     */
    private fun assertDatesAreNotWrapped(parent: FrameLayout) {
        texts(parent).filter { it.text.isNotBlank() }.forEach { label ->
            val layout = requireNotNull(label.layout)
            withClue("wrapped onto ${layout.lineCount} lines: ${label.text}") { layout.lineCount shouldBe 1 }
        }
    }

    private fun texts(view: View): List<TextView> =
        when (view) {
            is TextView -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
            else -> emptyList()
        }

    private companion object {
        /** 10 October 2026, in each reference image's own language. */
        const val SATURDAY_EN = "Saturday"
        const val SATURDAY_FA = "شنبه"
        const val SATURDAY_AR = "السبت"

        /** The weekday plus the user's three calendars: what two cells hold at the default font scale. */
        const val WEEKDAY_AND_THREE_DATES = 4

        /** At font scale 2.0 two cells hold one date, and the weekday gives way to it. */
        const val ONE_DATE_ONLY = 1
    }
}
