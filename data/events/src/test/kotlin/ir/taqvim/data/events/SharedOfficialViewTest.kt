/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.events.AstroKind
import ir.taqvim.core.events.AstronomicalEventSource
import ir.taqvim.core.events.EventPreferences
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.model.Weekday
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/**
 * The dataset lookups are built once per settings value and shared by every collector.
 *
 * The calendar screen collects four of these flows at once — the pager's three pages and the day-details pane — and
 * `monthEvents` rebuilds them on every swipe. While each collector built its own `OfficialView`, every swipe threw
 * away four freshly built [ir.taqvim.core.events.EventLookup] caches and recomputed each year index from the dataset's
 * ~300 definitions, including the ephemeris searches of the `Astronomical` rules (ADR-0044). The ephemeris is counted
 * here because it is the most expensive of that work and the easiest to observe.
 */
class SharedOfficialViewTest {
    private val tehran = TimeZone.of("Asia/Tehran")
    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.parse("2026-09-23T00:00:00Z")
        }
    private val queries = AtomicInteger()
    private val countingAstronomy =
        AstronomicalEventSource { kind, from, until ->
            queries.incrementAndGet()
            SkyAstronomicalEventSource.instants(kind, from, until)
        }
    private val settings = MutableStateFlow(settings(IslamicVariant.IRAN_OFFICIAL))

    private val repository =
        EventsRepository(
            settings = settings,
            inputs =
                EventInputs(
                    personal = PersonalEventsSource { MutableStateFlow(emptyList()) },
                    device = DeviceEventsSource { MutableStateFlow(emptyList()) },
                    ics = IcsEventsSource { MutableStateFlow(emptyList()) },
                ),
            clock = clock,
            zones = flowOf(tehran),
            catalog = OfficialCatalog(astronomy = countingAstronomy),
            computeDispatcher = Dispatchers.Unconfined,
        )

    @Test
    fun `a swipe reuses the lookups the open built`() =
        runTest {
            openScreen(offset = 0)
            val afterOpen = queries.get()
            afterOpen shouldBeGreaterThan 0

            openScreen(offset = 0)
            queries.get() shouldBe afterOpen

            // Swiping on may reach a year the open did not cover, which is legitimate new work...
            openScreen(offset = 1)
            val afterSwipe = queries.get()

            // ...but coming back must cost nothing: the pages are rebuilt, the lookups behind them are not.
            openScreen(offset = 0)
            queries.get() shouldBe afterSwipe
        }

    @Test
    fun `changed settings build the lookups again`() =
        runTest {
            openScreen(offset = 0)
            val afterOpen = queries.get()

            settings.value = settings(IslamicVariant.UMM_AL_QURA)
            openScreen(offset = 0)

            queries.get() shouldBeGreaterThan afterOpen
        }

    /** The three pages the pager prefetches plus the day-details pane, as `CalendarMonthSource.monthEvents` does. */
    private suspend fun openScreen(offset: Int) {
        (offset - 1..offset + 1).forEach { page -> repository.days(monthGrid(page)).first() }
        repository.day(monthGrid(offset).start).first()
    }

    private fun monthGrid(monthsFromStart: Int): JdnRange {
        val start = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 7, 1))
        val first = Jdn(start.value + monthsFromStart * GRID_STEP)
        return first..Jdn(first.value + GRID_DAYS)
    }

    private fun settings(variant: IslamicVariant) =
        EventsSettings(
            preferences =
                EventPreferences(
                    enabledSources = EventSource.entries.toSet(),
                    homeTimeZone = tehran,
                    islamicVariant = variant,
                    islamicOverrides = null,
                ),
            weekend = setOf(Weekday.FRIDAY),
            hijriOffset = null,
        )

    private companion object {
        const val GRID_STEP = 30L
        const val GRID_DAYS = 41L
    }
}
