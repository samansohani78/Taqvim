/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.events.DayEvents
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.widgets.WidgetConfig
import ir.taqvim.feature.widgets.WidgetContent
import ir.taqvim.feature.widgets.WidgetKind
import ir.taqvim.feature.widgets.WidgetPrayer
import ir.taqvim.feature.widgets.WidgetView
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/**
 * Every widget loads what it draws and nothing else.
 *
 * Counts rather than outcomes: a widget that quietly assembles a day it never shows still looks right on screen, so
 * the only way to keep it honest is to count what it touched. The counters stand for the real dependencies —
 * `rangeEvents` is the one call that reaches Room and the subscription cache, and the prayer names are consulted
 * exactly once per prayer line built, so an untouched name means no prayer line and, with the loader asking for
 * times only when a line needs them, no prayer-time calculation.
 *
 * The calendar provider is not among the dependencies any more (ADR-0051): no widget can reach it because no code
 * can. `SecurityKonsistTest` is what holds that, and it holds it for the whole app rather than per widget.
 *
 * A place is chosen in every case on purpose: with no place there is nothing to compute and the test would pass
 * whatever the loader did.
 */
class WidgetDataNeedsTest {
    private val tehran = ChosenPlace(PlaceSource.CITY, 1, "Tehran", Coordinates(35.69, 51.42), "Asia/Tehran")
    private val zone = TimeZone.of("Asia/Tehran")
    private val noon = Instant.parse("2026-09-13T08:30:00Z")
    private val today = LocalDate(2026, 9, 13).toJdn()

    private val assembled = mutableListOf<JdnRange>()
    private val holidayAsked = mutableListOf<Jdn>()
    private val prayersNamed = mutableListOf<WidgetPrayer>()

    private fun events(range: JdnRange): Flow<List<DayEvents>> {
        assembled += range
        return flowOf(range.map(::dayEvents))
    }

    private fun dayEvents(jdn: Jdn): DayEvents =
        DayEvents(
            jdn = jdn,
            islamicDate = CalendarDate(CalendarSystem.ISLAMIC, 1448, 3, 1),
            hijri = null,
            official = emptyList(),
            isHoliday = true,
            isWeekend = false,
            personal = emptyList(),
            ics = emptyList(),
        )

    private fun source() =
        PreferencesWidgetDataSource(
            preferences = repositoryOf(UserPreferences.defaultsFor("fa").copy(place = tehran)),
            rangeEvents = ::events,
            names = { prayer ->
                prayersNamed += prayer
                prayer.name
            },
            holidayOnly = { jdn ->
                holidayAsked += jdn
                true
            },
            deviceZone = { zone },
        )

    private suspend fun load(kind: WidgetKind) =
        source().load(kind, WidgetConfig().normalizedFor(kind), noon, WidgetView())

    @Test
    fun `the three-date widget touches nothing but the calendar engines`(): Unit =
        runTest {
            val data = load(WidgetKind.THREE_DATES)

            // T-1216's whole point: local date, three conversions, three strings. Everything a date needs is
            // arithmetic, so the widget draws filled on its first update instead of leaving the host's placeholder
            // up while a flow it does not read settles.
            data.calendarDates.isNotEmpty() shouldBe true

            withClue("no range load: no Room query, no subscription cache read, no day assembled") {
                assembled.shouldBeEmpty()
            }
            withClue("no dataset holiday read: the widget draws no holiday colour") {
                holidayAsked.shouldBeEmpty()
            }
            withClue("no prayer name looked up, so no PrayerTimesCalculator run") {
                prayersNamed.shouldBeEmpty()
            }
            data.nextPrayer.shouldBeNull()
            data.prayers.shouldBeEmpty()
            // Nothing personal reaches it either, which is what lets it offer itself for lock-screen placement.
            data.events.shouldBeEmpty()
        }

    @Test
    fun `a date widget reads its holiday from the dataset and computes no prayer times`(): Unit =
        runTest {
            val data = load(WidgetKind.DATE_1X1)

            holidayAsked shouldBe listOf(today)
            assembled.shouldBeEmpty()
            prayersNamed.shouldBeEmpty()
            data.nextPrayer.shouldBeNull()
            data.calendarDates.shouldBeEmpty()
        }

    @Test
    fun `a day summary with its event list switched off does not assemble the day`(): Unit =
        runTest {
            val full = WidgetConfig().normalizedFor(WidgetKind.DAY_SUMMARY_2X2)
            val withoutEvents = full.copy(contents = (full.contents - WidgetContent.EVENTS).toImmutableSet())

            val data = source().load(WidgetKind.DAY_SUMMARY_2X2, withoutEvents, noon, WidgetView())

            // It declares EVENTS as a kind, but this configuration draws none, so it pays the cheap holiday path.
            assembled.shouldBeEmpty()
            holidayAsked shouldBe listOf(today)
            data.events.shouldBeEmpty()
        }

    @Test
    fun `a day summary that does list events assembles exactly one day`(): Unit =
        runTest {
            load(WidgetKind.DAY_SUMMARY_2X2)

            assembled shouldBe listOf(today..today)
            holidayAsked.shouldBeEmpty()
        }

    @Test
    fun `the grid widgets load their range once, not today and then the range`(): Unit =
        runTest {
            val grids =
                listOf(
                    WidgetKind.MONTH_INTERACTIVE,
                    WidgetKind.MONTH_BITMAP,
                    WidgetKind.WEEK_STRIP,
                    WidgetKind.SCHEDULE,
                )

            grids.forEach { kind ->
                assembled.clear()
                holidayAsked.clear()

                load(kind)

                withClue(kind.name) {
                    // One range. Today used to be loaded on its own first, for a holiday flag and an event list the
                    // grid never reads, and then the whole range was loaded over the top of it.
                    assembled shouldHaveSize 1
                    assembled.single().contains(today) shouldBe true
                    holidayAsked.shouldBeEmpty()
                }
            }
        }

    @Test
    fun `only the prayer widgets compute prayer times`(): Unit =
        runTest {
            val silent =
                listOf(
                    WidgetKind.DATE_1X1,
                    WidgetKind.DATE_CLOCK_4X1,
                    WidgetKind.THREE_DATES,
                    WidgetKind.MONTH_INTERACTIVE,
                    WidgetKind.MONTH_BITMAP,
                    WidgetKind.WEEK_STRIP,
                    WidgetKind.SCHEDULE,
                    WidgetKind.MOON,
                    WidgetKind.MAP,
                    WidgetKind.COUNTDOWN,
                )

            silent.forEach { kind ->
                prayersNamed.clear()

                val data = load(kind)

                withClue(kind.name) {
                    prayersNamed.shouldBeEmpty()
                    data.prayers.shouldBeEmpty()
                    data.nextPrayer.shouldBeNull()
                }
            }
        }

    @Test
    fun `the prayer strip still gets the whole day and the summary still gets the next time`(): Unit =
        runTest {
            val strip = load(WidgetKind.PRAYER_STRIP_4X2)
            strip.prayers.isNotEmpty() shouldBe true

            prayersNamed.clear()
            val summary = load(WidgetKind.DAY_SUMMARY_2X2)
            summary.nextPrayer.shouldNotBeNull()

            prayersNamed.clear()
            val sun = load(WidgetKind.SUN_ARC)
            sun.nextPrayer.shouldNotBeNull()
        }

    @Test
    fun `only the three-date widget formats the day in every calendar the user keeps`(): Unit =
        runTest {
            WidgetKind.entries.forEach { kind ->
                val data = load(kind)

                withClue(kind.name) {
                    if (kind == WidgetKind.THREE_DATES) {
                        data.calendarDates.isNotEmpty() shouldBe true
                    } else {
                        data.calendarDates.shouldBeEmpty()
                    }
                }
            }
        }
}
