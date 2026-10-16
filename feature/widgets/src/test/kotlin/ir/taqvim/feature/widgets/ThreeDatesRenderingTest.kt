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
    fun english(): Unit = capture("en", listOf("18 Mehr 1405", "10 October 2026", "28 Rabiʻ II 1448"))

    @Test
    @Config(qualifiers = "fa-mdpi")
    fun persianWithLatinGregorian(): Unit =
        capture("fa", listOf("۱۸ مهر ۱۴۰۵", "10 October 2026", "۲۸ ربیع‌الثانی ۱۴۴۸"))

    @Test
    @Config(qualifiers = "ar-mdpi")
    fun arabic(): Unit = capture("ar", listOf("١٨ مهر ١٤٠٥", "١٠ أكتوبر ٢٠٢٦", "٢٨ ربيع الآخر ١٤٤٨"))

    @Test
    @Config(qualifiers = "en-mdpi")
    fun englishLargeText(): Unit =
        capture("en_fs200", listOf("18 Mehr 1405", "10 October 2026", "28 Rabiʻ II 1448"), 2f)

    @Test
    @Config(qualifiers = "fa-mdpi")
    fun persianLargeText(): Unit =
        capture("fa_fs200", listOf("۱۸ مهر ۱۴۰۵", "10 October 2026", "۲۸ ربیع‌الثانی ۱۴۴۸"), 2f)

    @Test
    @Config(qualifiers = "ar-mdpi")
    fun arabicLargeText(): Unit = capture("ar_fs200", listOf("١٨ مهر ١٤٠٥", "١٠ أكتوبر ٢٠٢٦", "٢٨ ربيع الآخر ١٤٤٨"), 2f)

    private fun capture(
        language: String,
        dates: List<String>,
        fontScale: Float = 1f,
    ) {
        RuntimeEnvironment.setFontScale(fontScale)
        val context = Robolectric.buildActivity(Activity::class.java).setup().get()
        val config = WidgetConfig.defaultFor(WidgetKind.THREE_DATES)
        val data = WidgetSamples.data().copy(calendarDates = persistentListOf(*dates.toTypedArray()))
        val remoteViews =
            runBlocking {
                GlanceRemoteViews()
                    .compose(context, DpSize(WIDTH.dp, HEIGHT.dp)) {
                        GlanceTheme {
                            WidgetFrame(config, onClick = null) { style ->
                                ThreeDatesWidget().Content(data, config, style, WidgetSize.MEDIUM_LARGE)
                            }
                        }
                    }.remoteViews
            }
        val parent = FrameLayout(context)
        context.setContentView(parent)
        parent.addView(remoteViews.apply(context, parent))
        parent.measure(exactly(WIDTH), exactly(HEIGHT))
        parent.layout(0, 0, WIDTH, HEIGHT)
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        parent.draw(Canvas(bitmap))
        bitmap.captureRoboImage("src/test/screenshots/three_dates/$language.png")
        assertDatesFit(parent)
    }

    private fun exactly(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)

    private fun assertDatesFit(parent: FrameLayout) {
        val labels = texts(parent).filter { it.text.isNotBlank() }
        labels.size shouldBe 3
        labels.forEach { label ->
            val bounds = Rect()
            label.getDrawingRect(bounds)
            parent.offsetDescendantRectToMyCoords(label, bounds)
            check(bounds.top >= 0 && bounds.bottom <= HEIGHT) { "Clipped date: ${label.text}, $bounds" }
            val layout = requireNotNull(label.layout)
            check(layout.height <= label.height) {
                "Vertically truncated date: ${label.text}; " +
                    "layout=${layout.height}, view=${label.height}, bounds=$bounds"
            }
            (0 until layout.lineCount).forEach { line -> layout.getEllipsisCount(line) shouldBe 0 }
        }
    }

    private fun texts(view: View): List<TextView> =
        when (view) {
            is TextView -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
            else -> emptyList()
        }

    private companion object {
        const val WIDTH = 180
        const val HEIGHT = 180
    }
}
