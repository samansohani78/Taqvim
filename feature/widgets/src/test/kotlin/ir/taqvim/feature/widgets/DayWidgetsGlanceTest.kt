/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.hasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasTextEqualTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.i18n.BidiText
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** T-1201…T-1204 Glance unit tests: each widget's content per size bucket and where its taps lead. */
@RunWith(AndroidJUnit4::class)
class DayWidgetsGlanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sample = WidgetSamples.data()
    private val events =
        persistentListOf(
            WidgetEventLine("Nowruz", isHoliday = true),
            WidgetEventLine("Mom's birthday", isHoliday = false, eventId = 42),
            WidgetEventLine("Dentist", isHoliday = false, eventId = 43),
            WidgetEventLine("Team call", isHoliday = false, eventId = 44),
            WidgetEventLine("Library", isHoliday = false, eventId = 45),
        )
    private val prayers =
        persistentListOf(
            WidgetPrayerLine("Fajr", "05:01"),
            WidgetPrayerLine("Sunrise", "06:30"),
            WidgetPrayerLine("Dhuhr", "13:05"),
            WidgetPrayerLine("Asr", "16:40", isNext = true),
            WidgetPrayerLine("Maghrib", "19:12"),
            WidgetPrayerLine("Isha", "20:10"),
        )
    private val full = sample.copy(events = events, prayers = prayers, nextPrayer = prayers[3])

    private fun render(
        widget: TaqvimGlanceWidget,
        data: WidgetData,
        size: WidgetSize,
        config: WidgetConfig = WidgetConfig.defaultFor(widget.kind),
        checks: GlanceAppWidgetUnitTest.() -> Unit,
    ) {
        runGlanceAppWidgetUnitTest(GLANCE_TEST_TIMEOUT) {
            setContext(context)
            setAppWidgetSize(DpSize(size.widthDp.dp, size.heightDp.dp))
            provideComposable {
                WidgetFrame(config, WidgetLinks.intent(context, WidgetClickTarget.Today)) { style ->
                    widget.Content(data, config, style, size)
                }
            }
            checks()
        }
    }

    private fun link(target: WidgetClickTarget) = hasStartActivityClickAction(WidgetLinks.intent(context, target))

    @Test
    @Config(qualifiers = "fa")
    fun threeDatesKeepMixedDirectionLinesIsolatedInTheCompactWidget() {
        val dates = persistentListOf("۱۸ مهر ۱۴۰۵", "10 October 2026", "٢٨ ربيع الآخر ١٤٤٨")
        render(ThreeDatesWidget(), sample.copy(calendarDates = dates), WidgetSize.MEDIUM) {
            dates.forEach { line ->
                onNode(hasTextEqualTo(BidiText.isolate(line))).assertExists()
                onNode(hasTextEqualTo(line)).assertDoesNotExist()
            }
        }
    }

    /**
     * The same contract as [threeDatesKeepMixedDirectionLinesIsolatedInTheCompactWidget], for the date header every
     * other widget draws.
     *
     * T-1216 isolated its three lines and `WidgetDateLines` did not, so the widget that exists to show mixed
     * calendars was correct and the twelve that merely *can* show them were not: a Gregorian secondary date on a
     * Persian widget was laid out in the widget's direction, which moves the year to the wrong end. Asserted here
     * on both lines, under a Persian qualifier, because that is the configuration where it is visible.
     */
    @Test
    @Config(qualifiers = "fa")
    fun theSharedDateHeaderIsolatesBothItsDates() {
        val persian = "۲۲ شهریور ۱۴۰۵"
        val gregorian = "13 September 2026"
        // The day summary at two cells, not the 4x1: one cell tall draws the day alone, so the secondary date
        // would not be on screen to have a direction.
        val data = full.copy(title = persian, secondaryDate = gregorian)
        render(DaySummaryWidget2x2(), data, WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo(BidiText.isolate(persian))).assertExists()
            onNode(hasTextEqualTo(persian)).assertDoesNotExist()
            onNode(hasTextEqualTo(BidiText.isolate(gregorian))).assertExists()
            onNode(hasTextEqualTo(gregorian)).assertDoesNotExist()
        }
    }

    /**
     * One cell is the day number and the link, and nothing else.
     *
     * It used to draw the weekday and the secondary date here too, which is what the widget's configuration asks
     * for — but 40 dp less the frame's padding cannot hold them, so they were laid out past the bottom edge and
     * the user saw a number and a sliver (`EveryWidgetFitsItsBoxTest`). What a widget *shows* is now what fits.
     */
    @Test
    fun theOneByOneWidgetShowsTheDayNumber() {
        render(DateWidget1x1(), full, WidgetSize.SMALL) {
            onNode(hasTextEqualTo("22")).assertExists()
            onNode(hasTextEqualTo("Sunday")).assertDoesNotExist()
            onNode(hasTextEqualTo(BidiText.isolate("22 Shahrivar 1405"))).assertDoesNotExist()
            onNode(link(WidgetClickTarget.Today)).assertExists()
        }
    }

    /** The 4x1 is one cell tall as well, so it draws the clock and the day — see above for why not more. */
    @Test
    fun theDateAndClockWidgetShowsTheFullDate() {
        render(DateClockWidget4x1(), full, WidgetSize.WIDE) {
            onNode(hasTextEqualTo(BidiText.isolate("22 Shahrivar 1405"))).assertExists()
            onNode(hasTextEqualTo(BidiText.isolate("13 September 2026"))).assertDoesNotExist()
            onNode(hasTextEqualTo("Nowruz")).assertDoesNotExist()
        }
    }

    @Test
    fun theDaySummaryShowsEventsAndTheNextPrayerWithTheirLinks() {
        render(DaySummaryWidget2x2(), full, WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo("Nowruz").and(link(WidgetClickTarget.Day(full.date)))).assertExists()
            onNode(hasTextEqualTo("Mom's birthday").and(link(WidgetClickTarget.Event(42)))).assertExists()
            onNode(hasTextEqualTo("Dentist")).assertDoesNotExist()
            onNode(hasTextEqualTo("Asr 16:40").and(link(WidgetClickTarget.PrayerTimes))).assertExists()
        }
        render(DaySummaryWidget2x2(), full, WidgetSize.LARGE) {
            onNode(hasTextEqualTo("Team call").and(link(WidgetClickTarget.Event(44)))).assertExists()
            onNode(hasTextEqualTo("Library")).assertDoesNotExist()
        }
    }

    @Test
    fun theDaySummaryExplainsMissingEventsAndPlaceAndHonoursSwitchedOffParts() {
        render(DaySummaryWidget2x2(), sample.copy(events = persistentListOf(), nextPrayer = null), WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo(context.getString(R.string.widget_no_events))).assertExists()
            onNode(hasTextEqualTo(context.getString(R.string.widget_no_place))).assertExists()
        }
        val dateOnly = WidgetConfig.defaultFor(WidgetKind.DAY_SUMMARY_2X2).copy(contents = persistentSetOf())
        render(DaySummaryWidget2x2(), full, WidgetSize.MEDIUM, dateOnly) {
            onNode(hasTextEqualTo(BidiText.isolate("22 Shahrivar 1405"))).assertExists()
            onNode(hasTextEqualTo("Nowruz")).assertDoesNotExist()
            onNode(hasTextEqualTo("Asr 16:40")).assertDoesNotExist()
        }
    }

    @Test
    fun thePrayerStripListsTheDayAndKeepsOnlyTheStripWhenShrunk() {
        render(PrayerStripWidget4x2(), full, WidgetSize.LARGE) {
            onNode(hasTextEqualTo(BidiText.isolate("22 Shahrivar 1405"))).assertExists()
            prayers.forEach { onNode(hasTextEqualTo(it.name)).assertExists() }
            onNode(hasTextEqualTo("16:40")).assertExists()
            onNode(link(WidgetClickTarget.PrayerTimes)).assertExists()
        }
        render(PrayerStripWidget4x2(), full, WidgetSize.WIDE) {
            onNode(hasTextEqualTo(BidiText.isolate("22 Shahrivar 1405"))).assertDoesNotExist()
            onNode(hasTextEqualTo("Isha")).assertExists()
        }
        render(PrayerStripWidget4x2(), sample.copy(prayers = persistentListOf()), WidgetSize.LARGE) {
            onNode(hasTextEqualTo(context.getString(R.string.widget_no_place)).and(link(WidgetClickTarget.PrayerTimes)))
                .assertExists()
        }
    }

    @Test
    @Config(qualifiers = "fa")
    fun persianWidgetsUsePersianTextAndDigits() {
        val persian =
            full.copy(
                dayNumber = "۲۲",
                title = "۲۲ شهریور ۱۴۰۵",
                weekday = "یکشنبه",
                events = persistentListOf(),
                nextPrayer = WidgetPrayerLine("عصر", "۱۶:۴۰", isNext = true),
            )
        render(DaySummaryWidget2x2(), persian, WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo(BidiText.isolate("۲۲ شهریور ۱۴۰۵"))).assertExists()
            onNode(hasTextEqualTo("امروز رویدادی نیست")).assertExists()
            onNode(hasTextEqualTo("عصر ۱۶:۴۰")).assertExists()
        }
    }
}
