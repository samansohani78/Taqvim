/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.benchmark.micro

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ir.taqvim.feature.widgets.DateClockWidget4x1
import ir.taqvim.feature.widgets.DateWidget1x1
import ir.taqvim.feature.widgets.DaySummaryWidget2x2
import ir.taqvim.feature.widgets.PrayerStripWidget4x2
import ir.taqvim.feature.widgets.TaqvimGlanceWidget
import ir.taqvim.feature.widgets.ThreeDatesWidget
import ir.taqvim.feature.widgets.WidgetConfig
import ir.taqvim.feature.widgets.WidgetData
import ir.taqvim.feature.widgets.WidgetEventLine
import ir.taqvim.feature.widgets.WidgetFrame
import ir.taqvim.feature.widgets.WidgetPrayerLine
import ir.taqvim.feature.widgets.WidgetSize
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-1801 Glance composition timings of the text widgets T-1201 to T-1204 and T-1216. Each run composes the widget's
 * frame and content with fixed data and translates the result to `RemoteViews` through Glance's own
 * `GlanceAppWidget.compose`. Loading the state and the launcher's inflation of the `RemoteViews` are not included.
 *
 * **Read these against [emptyBaseline], not against plan §9's 30 ms.** `GlanceAppWidget.compose` carries a large
 * fixed cost per call that has nothing to do with the widget: on the OnePlus 15 (2026-10-07) the four widgets came
 * out at 407.0, 410.1, 416.8 and 417.0 ms — a 2.5 % spread across widgets whose content differs roughly tenfold —
 * and the same four on a hosted x86 emulator read 412.2, 418.6, 421.1 and 416.8 ms, though that machine is half the
 * speed of the phone on compute-bound work (the bitmap benchmarks beside them moved −27 % to −56 % between the two).
 * A cost that moves neither with content nor with the CPU is not composition. The bitmap benchmarks, which have no
 * Glance harness, range 0.6 to 13.1 ms on the same runs and do scale with content.
 *
 * So [emptyBaseline] composes a widget that draws nothing, and a widget's own composition cost is its time minus
 * that baseline. That difference is what plan §9's budget is about, and it is roughly 10 ms between the simplest and
 * the most complex of these widgets. No budget is committed for the raw figures: one set to pass them would be
 * measuring the harness, and one set to the §9 rule would fail on a cost the app does not own.
 */
@RunWith(AndroidJUnit4::class)
class GlanceWidgetBenchmark {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun date1x1(): Unit = compose("date1x1", DateWidget1x1(), WidgetSize.SMALL)

    @Test
    fun dateClock4x1(): Unit = compose("dateClock4x1", DateClockWidget4x1(), WidgetSize.WIDE)

    @Test
    fun daySummary2x2(): Unit = compose("daySummary2x2", DaySummaryWidget2x2(), WidgetSize.MEDIUM)

    @Test
    fun prayerStrip4x2(): Unit = compose("prayerStrip4x2", PrayerStripWidget4x2(), WidgetSize.LARGE)

    @Test
    fun threeDates(): Unit = compose("threeDates", ThreeDatesWidget(), WidgetSize.SMALL)

    /**
     * A widget that draws nothing, so its time is the fixed cost of `GlanceAppWidget.compose` itself.
     *
     * Subtract it from the others to get what each widget's own content costs to compose.
     */
    @Test
    fun emptyBaseline() {
        val host = EmptyWidget()
        val size = WidgetSize.SMALL
        val dpSize = DpSize(size.widthDp.dp, size.heightDp.dp)
        val timing = Timing.measure(WARMUP, RUNS) { runBlocking { host.compose(context, size = dpSize) } }
        TimingReport.record(CLASS_NAME, "emptyBaseline", timing)
    }

    private fun compose(
        name: String,
        widget: TaqvimGlanceWidget,
        size: WidgetSize,
    ) {
        val host = FixedContentWidget(widget, DATA, size)
        val dpSize = DpSize(size.widthDp.dp, size.heightDp.dp)
        val timing = Timing.measure(WARMUP, RUNS) { runBlocking { host.compose(context, size = dpSize) } }
        TimingReport.record(CLASS_NAME, name, timing)
    }

    private companion object {
        val CLASS_NAME: String = GlanceWidgetBenchmark::class.java.name

        /**
         * Fewer compositions than the default, because `GlanceAppWidget.compose` does not come free of its sessions.
         * Six cases at the default 10 + 30 made the fifth fail with an empty failure and the sixth never run, while
         * each passes alone; 5 + 15 keeps all six in one process. That the harness cannot be asked for 240 sessions
         * is itself part of why its fixed cost is 405 ms.
         */
        const val WARMUP: Int = 5
        const val RUNS: Int = 15

        /** A full day: three events (one a holiday) and six prayer times with the next one marked. */
        val DATA: WidgetData =
            WidgetData(
                date = LocalDate(2026, 9, 13),
                dayNumber = "۲۲",
                title = "۲۲ شهریور ۱۴۰۵",
                weekday = "یکشنبه",
                secondaryDate = "13 September 2026",
                isHoliday = false,
                events =
                    persistentListOf(
                        WidgetEventLine("روز جهانی", isHoliday = false),
                        WidgetEventLine("تعطیل رسمی", isHoliday = true),
                        WidgetEventLine("جلسه", isHoliday = false, eventId = 1L),
                    ),
                nextPrayer = WidgetPrayerLine("مغرب", "۱۹:۱۲", isNext = true),
                prayers =
                    persistentListOf(
                        WidgetPrayerLine("صبح", "۰۵:۰۴"),
                        WidgetPrayerLine("طلوع", "۰۶:۳۱"),
                        WidgetPrayerLine("ظهر", "۱۲:۵۸"),
                        WidgetPrayerLine("غروب", "۱۸:۵۲"),
                        WidgetPrayerLine("مغرب", "۱۹:۱۲", isNext = true),
                        WidgetPrayerLine("نیمه‌شب", "۰۰:۱۷"),
                    ),
            )
    }
}

/** Composes nothing at all: its time is Glance's own per-call cost, not a widget's. */
private class EmptyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent { }
    }
}

/** Composes [widget]'s frame and content for [size] with [data] and the default configuration of its kind. */
private class FixedContentWidget(
    private val widget: TaqvimGlanceWidget,
    private val data: WidgetData,
    private val size: WidgetSize,
) : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val config = WidgetConfig().normalizedFor(widget.kind)
        provideContent {
            GlanceTheme {
                WidgetFrame(config, onClick = null) { style -> widget.Content(data, config, style, size) }
            }
        }
    }
}
