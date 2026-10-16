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
 * `GlanceAppWidget.compose`.
 *
 * **What these numbers are not.** They are not a widget's render time, and they must not be read against plan §9's
 * 30 ms. That budget belongs to `WidgetRenderBenchmark`, which times the bitmap painters with no Glance harness
 * around them, and it is enforced in `benchmark/budgets.json`. Nothing here has a budget, and none of these
 * figures has ever been compared against one.
 *
 * **What is measured.** `GlanceAppWidget.compose` carries a large fixed cost per call: on the OnePlus 15
 * (2026-10-07) four widgets came out at 407.0, 410.1, 416.8 and 417.0 ms, a 2.5 % spread across widgets whose
 * content differs roughly tenfold, and the same four on a hosted x86 emulator read 412.2, 418.6, 421.1 and
 * 416.8 ms, on a machine half the phone's speed on compute-bound work (the bitmap benchmarks beside them moved
 * −27 % to −56 % between the two). A cost that moves neither with content nor with the CPU is not composition.
 *
 * **What is not established.** That the fixed cost is constant and additive, so that subtracting an empty widget's
 * time leaves the widget's own. Two separately measured benchmarks ran at different moments in different thermal
 * states, and the differences in question — roughly 1 to 11 ms on a ~406 ms base, under 3 % — are smaller than this
 * harness's own run-to-run spread. Call such a difference an **incremental wall-clock estimate**, not a composition
 * cost, until [pairedAgainstEmpty] has run on a device and shown the per-pair difference to be stable.
 *
 * **Still unseparated.** One number here covers data loading (excluded: the data is fixed), Glance session and
 * composition overhead, and `RemoteViews` translation. The launcher's inflation of those `RemoteViews`, and
 * end-to-end time to a first useful render, are not measured anywhere and cannot be from inside this process.
 */
@RunWith(AndroidJUnit4::class)
class GlanceWidgetBenchmark {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun date1x1(): Unit = compose("date1x1", DateWidget1x1())

    @Test
    fun dateClock4x1(): Unit = compose("dateClock4x1", DateClockWidget4x1())

    @Test
    fun daySummary2x2(): Unit = compose("daySummary2x2", DaySummaryWidget2x2())

    @Test
    fun prayerStrip4x2(): Unit = compose("prayerStrip4x2", PrayerStripWidget4x2())

    @Test
    fun threeDates(): Unit = compose("threeDates", ThreeDatesWidget())

    /**
     * Empty and populated widgets measured alternately, to find out whether subtracting one from the other means
     * anything (L4).
     *
     * The reported `timeNs` is the per-pair signed difference; the two sides it was computed from ride along as
     * the `emptyNs` and `populatedNs` metrics. The difference's own spread is the answer. If it is tight,
     * the fixed cost is additive and the subtraction is a real quantity. If it is as wide as the figures it is
     * drawn from, the subtraction is noise and no composition cost has been measured.
     *
     * The order within each pair alternates, so whichever side runs first does not keep paying to wake something
     * up.
     *
     * Not yet run on hardware: the session budget below was fitted to six single-composition cases, and this one
     * composes twice per iteration.
     */
    @Test
    fun pairedAgainstEmpty() {
        val populatedWidget = DateWidget1x1()
        val size = populatedWidget.kind.defaultSize
        val dpSize = DpSize(size.widthDp.dp, size.heightDp.dp)
        val empty = EmptyWidget()
        val populated = FixedContentWidget(populatedWidget, DATA, size)

        val paired =
            Timing.measurePaired(
                warmup = PAIRED_WARMUP,
                runs = PAIRED_RUNS,
                a = { runBlocking { empty.compose(context, size = dpSize) } },
                b = { runBlocking { populated.compose(context, size = dpSize) } },
            )

        // The difference is the headline; the two sides ride along as their own metrics so the raw samples are
        // there to check it against, without inventing benchmark names no `@Test` declares.
        TimingReport.record(
            CLASS_NAME,
            "pairedAgainstEmpty",
            paired.difference,
            extra = mapOf("emptyNs" to paired.a, "populatedNs" to paired.b),
        )
    }

    /**
     * Measures [widget] at the bucket its kind lays out for at its default span, so the number describes the
     * widget the launcher actually draws.
     *
     * The size used to be passed in per case, and T-1216 was measured at [WidgetSize.SMALL] — a size it does not
     * support — for as long as that was possible. Reading it from the kind makes that drift unrepresentable.
     */
    private fun compose(
        name: String,
        widget: TaqvimGlanceWidget,
    ) {
        val size = widget.kind.defaultSize
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

        /** Half the iterations of a single case, because each one composes twice. */
        const val PAIRED_WARMUP: Int = 3
        const val PAIRED_RUNS: Int = 8

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
