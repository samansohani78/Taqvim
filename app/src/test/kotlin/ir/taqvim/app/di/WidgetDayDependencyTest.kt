/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.events.DayEvents
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.widgets.WidgetConfig
import ir.taqvim.feature.widgets.WidgetContent
import ir.taqvim.feature.widgets.WidgetKind
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
 * A widget asks for the day it needs and no more (T-1200).
 *
 * Assembling a day subscribes to the personal, device-calendar and subscription sources and waits for all three, so
 * until this was checked every widget paid for Room and the calendar provider — a date widget waited on a database
 * to draw a date its own calendar engine already knew, and the host drew its placeholder in the meantime.
 */
class WidgetDayDependencyTest {
    private val berlin = TimeZone.of("Europe/Berlin")
    private val noon = Instant.parse("2026-09-13T10:00:00Z")
    private val today = LocalDate(2026, 9, 13).toJdn()
    private val assembled = mutableListOf<JdnRange>()
    private val holidayAsked = mutableListOf<Jdn>()

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
            device = emptyList(),
            ics = emptyList(),
        )

    private fun source() =
        PreferencesWidgetDataSource(
            preferences = repositoryOf(UserPreferences.defaultsFor("fa")),
            rangeEvents = ::events,
            names = { it.name },
            holidayOnly = { jdn ->
                holidayAsked += jdn
                true
            },
            deviceZone = { berlin },
        )

    @Test
    fun `a date widget colours its holiday without assembling the day`(): Unit =
        runTest {
            val config = WidgetConfig().normalizedFor(WidgetKind.DATE_1X1)

            val data = source().load(WidgetKind.DATE_1X1, config, noon, WidgetView())

            data.isHoliday shouldBe true
            holidayAsked shouldBe listOf(today)
            assembled.shouldBeEmpty()
        }

    @Test
    fun `a date widget with holidays switched off asks for nothing at all`(): Unit =
        runTest {
            val noHolidays =
                WidgetConfig(contents = setOf(WidgetContent.WEEKDAY).toImmutableSet())
                    .normalizedFor(WidgetKind.DATE_1X1)

            val data = source().load(WidgetKind.DATE_1X1, noHolidays, noon, WidgetView())

            data.isHoliday shouldBe false
            holidayAsked.shouldBeEmpty()
            assembled.shouldBeEmpty()
        }

    @Test
    fun `a widget that lists events still assembles the day`(): Unit =
        runTest {
            val config = WidgetConfig().normalizedFor(WidgetKind.DAY_SUMMARY_2X2)

            val data = source().load(WidgetKind.DAY_SUMMARY_2X2, config, noon, WidgetView())

            assembled shouldBe listOf(today..today)
            holidayAsked.shouldBeEmpty()
            data.isHoliday shouldBe true
        }

    @Test
    fun `the month widgets assemble, because they mark every day of the month`(): Unit =
        runTest {
            source().load(
                WidgetKind.MONTH_BITMAP,
                WidgetConfig().normalizedFor(WidgetKind.MONTH_BITMAP),
                noon,
                WidgetView(),
            )

            assembled.isNotEmpty() shouldBe true
            holidayAsked.shouldBeEmpty()
        }
}
