/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.hasRunCallbackClickAction
import androidx.glance.appwidget.testing.unit.hasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasAnyDescendant
import androidx.glance.testing.unit.hasContentDescriptionEqualTo
import androidx.glance.testing.unit.hasTextEqualTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** T-1205…T-1209 Glance unit tests: controls, day cells, the schedule list and the drawn widgets per size bucket. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CalendarWidgetsGlanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val today = LocalDate(2026, 9, 13)
    private val holiday = LocalDate(2026, 9, 15)
    private val facts: (Jdn) -> WidgetDayFacts = { jdn ->
        if (jdn == holiday.toJdn()) {
            WidgetDayFacts(true, false, listOf(WidgetEventLine("Dentist", isHoliday = false, eventId = 43)))
        } else {
            WidgetDayFacts.NONE
        }
    }

    private fun data(
        language: LanguageSpec = requireNotNull(LanguageTable.forCode("en")),
        offset: Int = 0,
    ): WidgetData {
        val inputs =
            WidgetCalendarInputs(
                today.toJdn(),
                language,
                PersianCalendarSystem,
                GregorianCalendarSystem,
                Weekday.SATURDAY,
            )
        return WidgetSamples.data().copy(
            month = WidgetCalendarBuilder.month(inputs, offset, facts),
            week = WidgetCalendarBuilder.week(inputs, facts).toImmutableList(),
            schedule = WidgetCalendarBuilder.schedule(inputs, facts).toImmutableList(),
            sun = WidgetSun("06:38", "19:12", 0.5f),
        )
    }

    private fun render(
        widget: TaqvimGlanceWidget,
        data: WidgetData,
        size: WidgetSize,
        config: WidgetConfig = WidgetConfig.defaultFor(widget.kind),
        checks: GlanceAppWidgetUnitTest.() -> Unit,
    ) {
        runGlanceAppWidgetUnitTest {
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

    private fun step(delta: Int) = hasRunCallbackClickAction<MonthStepCallback>(WidgetMonthStep.parameters(delta))

    @Test
    fun theMonthControlsStepBetweenMonthsAndOpenTheEditor() {
        render(MonthInteractiveWidget(), data(), WidgetSize.LARGE) {
            onNode(hasContentDescriptionEqualTo("Previous month").and(hasTextEqualTo("‹")).and(step(-1))).assertExists()
            onNode(hasContentDescriptionEqualTo("Next month").and(hasTextEqualTo("›")).and(step(1))).assertExists()
            onNode(hasTextEqualTo("Today").and(step(0))).assertExists()
            onNode(
                hasContentDescriptionEqualTo("New event").and(link(WidgetClickTarget.NewEvent(today))),
            ).assertExists()
            onNode(hasTextEqualTo("Shahrivar 1405")).assertExists()
            onNode(hasTextEqualTo("August – September 2026")).assertExists()
            onAllNodes(link(WidgetClickTarget.Day(LocalDate(2026, 8, 23)))).assertCountEquals(2)
        }
        render(MonthInteractiveWidget(), data(offset = 1), WidgetSize.LARGE) {
            onNode(hasTextEqualTo("Mehr 1405")).assertExists()
            val mehr = LocalDate(2026, 9, 23)
            onNode(hasContentDescriptionEqualTo("New event").and(link(WidgetClickTarget.NewEvent(mehr)))).assertExists()
        }
    }

    @Test
    fun monthDaysOpenTheirDayWithHolidaysEventsAndSecondaryDaysWhenLarge() {
        render(MonthInteractiveWidget(), data(), WidgetSize.LARGE) {
            onNode(hasContentDescriptionEqualTo("22 Shahrivar 1405").and(link(WidgetClickTarget.Day(today))))
                .assertExists()
            onNode(
                hasContentDescriptionEqualTo("24 Shahrivar 1405, holiday").and(hasAnyDescendant(hasTextEqualTo("•"))),
            ).assertExists()
            onNode(hasContentDescriptionEqualTo("22 Shahrivar 1405").and(hasAnyDescendant(hasTextEqualTo("13"))))
                .assertDoesNotExist()
        }
        render(MonthInteractiveWidget(), data(), WidgetSize.EXTRA_LARGE) {
            onNode(hasContentDescriptionEqualTo("22 Shahrivar 1405").and(hasAnyDescendant(hasTextEqualTo("13"))))
                .assertExists()
        }
        render(MonthInteractiveWidget(), data().copy(month = null), WidgetSize.LARGE) {
            onNode(hasTextEqualTo(context.getString(R.string.widget_load_failed))).assertExists()
        }
    }

    @Test
    @Config(qualifiers = "fa-ldrtl")
    fun persianMonthsMirrorTheControlsAndUsePersianDigits() {
        render(MonthInteractiveWidget(), data(requireNotNull(LanguageTable.forCode("fa"))), WidgetSize.LARGE) {
            onNode(hasContentDescriptionEqualTo("ماه قبل").and(hasTextEqualTo("›")).and(step(-1))).assertExists()
            onNode(hasContentDescriptionEqualTo("ماه بعد").and(hasTextEqualTo("‹")).and(step(1))).assertExists()
            onNode(hasTextEqualTo("شهریور ۱۴۰۵")).assertExists()
            onNode(hasContentDescriptionEqualTo("۲۲ شهریور ۱۴۰۵").and(hasAnyDescendant(hasTextEqualTo("۲۲"))))
                .assertExists()
            onNode(hasContentDescriptionEqualTo("۲۴ شهریور ۱۴۰۵، تعطیل")).assertExists()
        }
    }

    @Test
    fun theWeekStripShowsTheSevenDaysFromTheWeekStart() {
        render(WeekStripWidget(), data(), WidgetSize.WIDE) {
            onNode(hasContentDescriptionEqualTo("21 Shahrivar 1405").and(link(WidgetClickTarget.Day(today.minusDay()))))
                .assertExists()
            onNode(hasContentDescriptionEqualTo("22 Shahrivar 1405").and(hasAnyDescendant(hasTextEqualTo("S"))))
                .assertExists()
            onNode(hasContentDescriptionEqualTo("27 Shahrivar 1405")).assertExists()
            onNode(hasContentDescriptionEqualTo("28 Shahrivar 1405")).assertDoesNotExist()
        }
    }

    @Test
    fun theScheduleListsDaysAndEventsWithTheirLinks() {
        render(ScheduleWidget(), data(), WidgetSize.LARGE) {
            onNode(hasTextEqualTo("Next 14 days")).assertExists()
            onNode(hasTextEqualTo("Sunday 22 Shahrivar 1405").and(link(WidgetClickTarget.Day(today)))).assertExists()
            onNode(hasTextEqualTo("Tuesday 24 Shahrivar 1405").and(link(WidgetClickTarget.Day(holiday)))).assertExists()
            onNode(hasTextEqualTo("Dentist").and(link(WidgetClickTarget.Event(43)))).assertExists()
        }
        val quiet = data().copy(schedule = persistentListOf(data().schedule.first()))
        render(ScheduleWidget(), quiet, WidgetSize.LARGE) {
            onNode(hasTextEqualTo(context.getString(R.string.widget_schedule_empty))).assertExists()
            onNode(hasTextEqualTo("Sunday 22 Shahrivar 1405")).assertDoesNotExist()
        }
    }

    @Test
    fun theMonthBitmapDrawsTheMonthAsOnePicture() {
        render(MonthBitmapWidget(), data(), WidgetSize.EXTRA_LARGE) {
            onNode(hasContentDescriptionEqualTo("Shahrivar 1405")).assertExists()
            onNode(hasContentDescriptionEqualTo("22 Shahrivar 1405")).assertDoesNotExist()
        }
        render(MonthBitmapWidget(), data().copy(month = null), WidgetSize.LARGE) {
            onNode(hasTextEqualTo(context.getString(R.string.widget_load_failed))).assertExists()
        }
    }

    @Test
    fun theSunArcDrawsTheDaylightAndTheNextPrayerOrAsksForAPlace() {
        render(SunArcWidget(), data(), WidgetSize.MEDIUM) {
            onNode(hasContentDescriptionEqualTo("Sunrise 06:38, sunset 19:12")).assertExists()
            onNode(hasTextEqualTo("Maghrib 19:12")).assertExists()
            onNode(
                link(WidgetClickTarget.PrayerTimes)
                    .and(hasAnyDescendant(hasContentDescriptionEqualTo("Sunrise 06:38, sunset 19:12"))),
            ).assertExists()
        }
        render(SunArcWidget(), data(), WidgetSize.LARGE, WidgetConfig(contents = persistentSetOf())) {
            onNode(hasTextEqualTo("Maghrib 19:12")).assertDoesNotExist()
        }
        render(SunArcWidget(), data().copy(sun = null), WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo(context.getString(R.string.widget_no_place)).and(link(WidgetClickTarget.PrayerTimes)))
                .assertExists()
        }
    }

    @Test
    fun aPolarDayOrNightAtTheChosenPlaceIsNotReportedAsAMissingPlace() {
        render(SunArcWidget(), data().copy(sun = null, daylightUnavailable = true), WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo("The Sun does not rise or set here today").and(link(WidgetClickTarget.PrayerTimes)))
                .assertExists()
            onNode(hasTextEqualTo(context.getString(R.string.widget_no_place))).assertDoesNotExist()
        }
    }

    private fun LocalDate.minusDay(): LocalDate = LocalDate.fromEpochDays(toEpochDays() - 1)
}
