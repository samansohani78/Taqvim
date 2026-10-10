/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.HolidayCalendar
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.data.events.OfficialEventView
import ir.taqvim.data.events.toEventsSettings
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.widgets.InstalledWidgetIds
import ir.taqvim.feature.widgets.WidgetConfig
import ir.taqvim.feature.widgets.WidgetDependency
import ir.taqvim.feature.widgets.WidgetKind
import ir.taqvim.feature.widgets.WidgetUpdatePolicy
import ir.taqvim.feature.widgets.WidgetUpdateTrigger
import ir.taqvim.feature.widgets.WidgetView
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Test

/**
 * Switching an official source off must redraw the widgets that colour a holiday, not only those that list events
 * (L1).
 *
 * A date widget lists nothing, so it declared [WidgetDependency.EVENTS] nowhere — but it does colour the day, and
 * whether the day is off is decided by exactly the settings that decide which events are shown. The refresher
 * therefore skipped it, and a 1×1 date widget went on showing a red day after the source that made it a holiday was
 * switched off, until something unrelated happened to redraw it.
 *
 * The fix is a dependency of its own rather than giving date widgets [WidgetDependency.EVENTS]: a holiday comes from
 * the dataset alone, so a personal event being saved must not wake every date widget on the home screen.
 */
class WidgetHolidayInvalidationTest {
    private val tehran = TimeZone.of("Asia/Tehran")
    private val base = UserPreferences.defaultsFor("fa")

    /** The holiday oracle the assembler itself uses, built the way production builds it. */
    private fun holidays(preferences: UserPreferences): HolidayCalendar {
        val view = OfficialEventView(preferences.toEventsSettings(homeTimeZone = tehran))
        return HolidayCalendar(view.lookup, preferences.app.enabledEventSources, preferences.weekend)
    }

    private fun withSources(sources: Set<EventSource>): UserPreferences =
        base.copy(app = base.app.copy(enabledEventSources = sources))

    /**
     * A day that is a holiday because of Iran's official calendar and of nothing else the user has on.
     *
     * Searched rather than hard-coded so the test keeps testing what it says when the dataset grows a record: a
     * fixed date could quietly become a holiday for a second source and stop proving anything.
     */
    private fun ironOnlyHoliday(
        on: HolidayCalendar,
        off: HolidayCalendar,
    ): Jdn {
        val firstOfYear = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, YEAR, 1, 1))
        val lastOfYear = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, YEAR + 1, 1, 1)) - 1
        val day =
            (firstOfYear.value..lastOfYear.value)
                .asSequence()
                .map(::Jdn)
                .firstOrNull { on.isHoliday(it) && !off.isHoliday(it) }
        return requireNotNull(day) { "no day in $YEAR is a holiday for Iran's calendar alone" }
    }

    @Test
    fun `switching an official source off redraws a date widget, which only colours the day`(): Unit =
        runTest {
            val on = withSources(setOf(EventSource.IRAN_OFFICIAL))
            val off = withSources(setOf(EventSource.INTERNATIONAL))
            val onHolidays = holidays(on)
            val offHolidays = holidays(off)
            val day = ironOnlyHoliday(onHolidays, offHolidays)

            // What the widget would draw really does change, so a redraw is owed.
            val before = loadHoliday(on, onHolidays, day)
            val after = loadHoliday(off, offHolidays, day)
            before shouldBe true
            after shouldBe false

            // And the refresher picks that widget, through the production selection path.
            val affected = widgetDependenciesChanged(on, off)
            affected shouldContain WidgetDependency.HOLIDAYS
            val placed: InstalledWidgetIds = mapOf(WidgetKind.DATE_1X1 to setOf(DATE_WIDGET_ID))
            val targets = WidgetUpdatePolicy().targets(WidgetUpdateTrigger.PreferencesChanged(affected), placed)

            targets[WidgetKind.DATE_1X1] shouldBe setOf(DATE_WIDGET_ID)
        }

    @Test
    fun `a date widget is still not woken by an event being saved, which cannot change a holiday`() {
        val placed: InstalledWidgetIds = mapOf(WidgetKind.DATE_1X1 to setOf(DATE_WIDGET_ID))

        val targets = WidgetUpdatePolicy().targets(WidgetUpdateTrigger.EventsChanged, placed)

        // This is why HOLIDAYS is its own dependency: giving date widgets EVENTS would have fixed the colour and
        // woken every one of them on each personal-event edit, for a flag no personal event can move.
        targets.shouldBeEmptyTargets()
        WidgetKind.DATE_1X1.dependencies shouldNotContain WidgetDependency.EVENTS
        WidgetKind.DATE_1X1.dependencies shouldContain WidgetDependency.HOLIDAYS
    }

    @Test
    fun `every setting that decides a holiday invalidates the widgets that colour one`() {
        val changes =
            mapOf(
                "a kind of day" to base.copy(app = base.app.copy(enabledEventCategories = emptySet())),
                "holidays only" to base.copy(app = base.app.copy(holidaysOnly = !base.app.holidaysOnly)),
                "the weekend" to base.copy(weekend = emptySet()),
                "a source" to withSources(setOf(EventSource.INTERNATIONAL)),
            )

        changes.forEach { (what, changed) ->
            withClue(what) {
                widgetDependenciesChanged(base, changed) shouldContain WidgetDependency.HOLIDAYS
            }
        }
    }

    @Test
    fun `a widget that shows no holiday is left alone`() {
        val off = withSources(setOf(EventSource.INTERNATIONAL))
        val affected = widgetDependenciesChanged(base, off)
        val placed: InstalledWidgetIds = mapOf(WidgetKind.THREE_DATES to setOf(DATE_WIDGET_ID))

        val targets = WidgetUpdatePolicy().targets(WidgetUpdateTrigger.PreferencesChanged(affected), placed)

        // T-1216 offers no holiday colour at all, so nothing it draws can have changed.
        targets.shouldBeEmptyTargets()
        WidgetKind.THREE_DATES.dependencies shouldNotContain WidgetDependency.HOLIDAYS
    }

    private suspend fun loadHoliday(
        preferences: UserPreferences,
        holidays: HolidayCalendar,
        day: Jdn,
    ): Boolean {
        val source =
            PreferencesWidgetDataSource(
                preferences = repositoryOf(preferences),
                rangeEvents = { error("a date widget must not assemble a day") },
                names = { it.name },
                holidayOnly = { holidays.isHoliday(it) },
                deviceZone = { tehran },
            )
        val config = WidgetConfig().normalizedFor(WidgetKind.DATE_1X1)
        return source.load(WidgetKind.DATE_1X1, config, day.middayIn(tehran), WidgetView()).isHoliday
    }

    private fun InstalledWidgetIds.shouldBeEmptyTargets() = values.flatten().isEmpty() shouldBe true

    /** Midday, so the civil date is the same one whatever the zone's offset does at its edges. */
    private fun Jdn.middayIn(zone: TimeZone): Instant = toLocalDate().atTime(NOON, 0).toInstant(zone)

    private companion object {
        const val DATE_WIDGET_ID = 11
        const val YEAR = 1405
        const val NOON = 12
    }
}
