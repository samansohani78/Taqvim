/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.EventCategory
import ir.taqvim.core.events.EventPreferences
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.data.database.PersonalEventEntity
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
 * Switching the religious category off removes religious observances from the whole app, and nothing else (T-302).
 *
 * The category is one of the two independent axes [ir.taqvim.core.events.EventVisibilityPolicy] applies — a source
 * says who publishes an observance, a category what kind of day it is — and every surface reads its answer, so this
 * proves what the calendar, day details, year, agenda, search and the widgets all show.
 */
class ReligiousCategoryFilterTest {
    private val tehran = TimeZone.of("Asia/Tehran")
    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.parse("2026-03-01T00:00:00Z")
        }
    private val year = 1405
    private val personal = MutableStateFlow(listOf(myBirthday()))

    private fun settings(categories: Set<EventCategory>) =
        EventsSettings(
            preferences =
                EventPreferences(
                    enabledSources = EventSource.entries.toSet(),
                    homeTimeZone = tehran,
                    islamicVariant = IslamicVariant.IRAN_OFFICIAL,
                    enabledCategories = categories,
                ),
            weekend = setOf(Weekday.FRIDAY),
            hijriOffset = null,
        )

    private fun repository(categories: Set<EventCategory>) =
        EventsRepository(
            settings = MutableStateFlow(settings(categories)),
            inputs =
                EventInputs(
                    personal = PersonalEventsSource { personal },
                    device = DeviceEventsSource { MutableStateFlow(emptyList()) },
                    ics = IcsEventsSource { MutableStateFlow(emptyList()) },
                ),
            clock = clock,
            zones = flowOf(tehran),
            computeDispatcher = Dispatchers.Unconfined,
        )

    private fun nowruz(): Jdn = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year, 1, 1))

    private fun myBirthday() =
        PersonalEventRecord(
            event =
                PersonalEventEntity(
                    id = 1,
                    title = "My birthday",
                    calendarSystem = CalendarSystem.PERSIAN,
                    startJdn = nowruz().value,
                    startMinute = null,
                    endMinute = null,
                    endJdn = nowruz().value,
                    timeZoneId = tehran.id,
                    createdAtEpochMillis = 0,
                    updatedAtEpochMillis = 0,
                ),
            recurrence = null,
        )

    /** Every category of dataset observance in the Persian year, by category. */
    private suspend fun categoriesShown(enabled: Set<EventCategory>): List<EventCategory> {
        val from = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year, 1, 1))
        val to = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year + 1, 1, 1)) - 1
        return repository(enabled)
            .days(from..to)
            .first()
            .flatMap { day -> day.official.map { it.definition.category } }
    }

    @Test
    fun `religious observances are shown while the category is on`(): Unit =
        runTest {
            val shown = categoriesShown(EventCategory.entries.toSet())

            shown.count { it == EventCategory.RELIGIOUS } shouldBeGreaterThan 0
            shown.count { it == EventCategory.NATIONAL } shouldBeGreaterThan 0
        }

    @Test
    fun `switching the religious category off removes every religious observance`(): Unit =
        runTest {
            val withoutReligious = EventCategory.entries.toSet() - EventCategory.RELIGIOUS

            val shown = categoriesShown(withoutReligious)

            shown.filter { it == EventCategory.RELIGIOUS }.shouldBeEmpty()
        }

    @Test
    fun `Iranian national days stay when religious observances are switched off`(): Unit =
        runTest {
            val all = categoriesShown(EventCategory.entries.toSet())
            val withoutReligious = categoriesShown(EventCategory.entries.toSet() - EventCategory.RELIGIOUS)

            withoutReligious.count { it == EventCategory.NATIONAL } shouldBe all.count { it == EventCategory.NATIONAL }
            withoutReligious.count { it == EventCategory.NATIONAL } shouldBeGreaterThan 0
        }

    @Test
    fun `a personal event survives every dataset category being switched off`(): Unit =
        runTest {
            val nothing = repository(emptySet()).days(nowruz()..nowruz()).first().single()

            nothing.official.shouldBeEmpty()
            nothing.personal.map { it.title } shouldContain "My birthday"
        }
}
