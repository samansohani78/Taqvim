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
 * T-1801 Glance composition timings of the text widgets T-1201 to T-1204 (plan §9: any widget render < 30 ms, budgets
 * in `benchmark/budgets.json`). Each run composes the widget's frame and content with fixed data and translates the
 * result to `RemoteViews` through Glance's own `GlanceAppWidget.compose`, as a widget update does once its state is
 * loaded. Loading the state and the launcher's inflation of the `RemoteViews` are not included.
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

    private fun compose(
        name: String,
        widget: TaqvimGlanceWidget,
        size: WidgetSize,
    ) {
        val host = FixedContentWidget(widget, DATA, size)
        val dpSize = DpSize(size.widthDp.dp, size.heightDp.dp)
        val timing = Timing.measure { runBlocking { host.compose(context, size = dpSize) } }
        TimingReport.record(CLASS_NAME, name, timing)
    }

    private companion object {
        val CLASS_NAME: String = GlanceWidgetBenchmark::class.java.name

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
