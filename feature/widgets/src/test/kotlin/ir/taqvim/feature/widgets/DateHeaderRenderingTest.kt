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
import ir.taqvim.core.i18n.BidiText
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The date header every widget but T-1216 draws, rendered in the direction a user actually reads it.
 *
 * `theSharedDateHeaderIsolatesBothItsDates` proves the isolate controls are in the string. It cannot prove the
 * line is laid out correctly, because a Glance semantics tree holds the text, not its visual order — the only
 * thing that shows a reordered year is a picture. So this inflates the real `RemoteViews` and captures them.
 *
 * The configuration is the one where the bug is visible: a Persian widget whose secondary calendar is Gregorian,
 * so one line is RTL and the other LTR inside the same RTL box. Verified as a real guard by removing
 * `BidiText.isolate` from `WidgetDateLines` and watching `verifyRoborazziDebug` fail on `fa_gregorian.png`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class DateHeaderRenderingTest {
    @Test
    @Config(qualifiers = "fa-mdpi")
    fun persianDayWithALatinGregorianSecondary(): Unit =
        capture("fa_gregorian", title = "۱۸ مهر ۱۴۰۵", secondary = "10 October 2026", weekday = "شنبه")

    @Test
    @Config(qualifiers = "en-mdpi")
    fun englishDayWithAPersianSecondary(): Unit =
        capture("en_persian", title = "10 October 2026", secondary = "۱۸ مهر ۱۴۰۵", weekday = "Saturday")

    private fun capture(
        name: String,
        title: String,
        secondary: String,
        weekday: String,
    ) {
        // Two cells, not the 4x1 span: one cell tall shows the day alone, and both dates have to be on screen
        // for their direction to be the thing under test. This is the size the day summary draws the header at.
        val size = WidgetKind.DAY_SUMMARY_2X2.defaultSize
        val context = Robolectric.buildActivity(Activity::class.java).setup().get()
        val config = WidgetConfig.defaultFor(WidgetKind.DAY_SUMMARY_2X2)
        val data = WidgetSamples.data().copy(title = title, secondaryDate = secondary, weekday = weekday)
        val remoteViews =
            runBlocking {
                GlanceRemoteViews()
                    .compose(context, DpSize(size.widthDp.dp, size.heightDp.dp)) {
                        GlanceTheme {
                            WidgetFrame(config, onClick = null) { style ->
                                WidgetDateLines(data, config, style, compact = false, size = size)
                            }
                        }
                    }.remoteViews
            }
        val parent = FrameLayout(context)
        context.setContentView(parent)
        parent.addView(remoteViews.apply(context, parent))
        parent.measure(exactly(size.widthDp), exactly(size.heightDp))
        parent.layout(0, 0, size.widthDp, size.heightDp)
        val bitmap = Bitmap.createBitmap(size.widthDp, size.heightDp, Bitmap.Config.ARGB_8888)
        parent.draw(Canvas(bitmap))
        bitmap.captureRoboImage("src/test/screenshots/date_header/$name.png")
        assertBothDatesAreDrawnInFull(parent, title, secondary)
        assertNothingIsClipped(parent, size.heightDp)
    }

    /**
     * Both dates are drawn in full.
     *
     * The picture is what proves the reading order; this proves there is something in the picture, so a golden
     * recorded from an empty header could never be mistaken for a pass. The contract is that no date is cut
     * short: a date is a value the widget chose to show and knows the length of, unlike an event title, which may
     * be any length and is allowed to ellipsize. "10 October 2026" at 16 sp was losing five characters to "…" in
     * a two-cell box until the day line was allowed a second row.
     */
    private fun assertBothDatesAreDrawnInFull(
        parent: FrameLayout,
        title: String,
        secondary: String,
    ) {
        val labels = texts(parent).filter { it.text.isNotBlank() }
        listOf(title, secondary).forEach { date ->
            val label =
                withClue("no label carries '$date'; drew ${labels.map { it.text }}") {
                    checkNotNull(labels.firstOrNull { BidiText.strip(it.text.toString()) == date })
                }
            val layout = requireNotNull(label.layout)
            (0 until layout.lineCount).forEach { line ->
                withClue("'$date' is cut short by ${layout.getEllipsisCount(line)} characters") {
                    check(layout.getEllipsisCount(line) == 0)
                }
            }
        }
    }

    /**
     * Every line the widget drew is inside the box the launcher gave it.
     *
     * The same contract `ThreeDatesRenderingTest` holds T-1216 to. It is asserted here because the picture showed
     * the 4x1 header losing its secondary date off the bottom edge: three lines of 12, 16 and 12 sp plus the
     * frame's padding do not fit the 40 dp its `WIDE` bucket is.
     */
    private fun assertNothingIsClipped(
        parent: FrameLayout,
        heightDp: Int,
    ) {
        texts(parent).filter { it.text.isNotBlank() }.forEach { label ->
            val bounds = Rect()
            label.getDrawingRect(bounds)
            parent.offsetDescendantRectToMyCoords(label, bounds)
            withClue("clipped: '${label.text}' at $bounds in a $heightDp dp box") {
                check(bounds.top >= 0 && bounds.bottom <= heightDp)
            }
        }
    }

    private fun exactly(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)

    private fun texts(view: View): List<TextView> =
        when (view) {
            is TextView -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
            else -> emptyList()
        }
}
