/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.DateOrigin
import ir.taqvim.core.calendar.IranCrescentCalendar
import ir.taqvim.core.calendar.IranIslamicCalendar
import ir.taqvim.core.calendar.IslamicMonthOverrides
import ir.taqvim.core.calendar.IslamicMonthTable
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.TabularIslamicCalendar
import ir.taqvim.core.events.EventId
import ir.taqvim.core.events.EventPreferences
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.model.Weekday
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** DT-002, DT-033: every official event in the Islamic calendar says whether its date is computed or official. */
class OfficialDateOriginsTest {
    private val tehran = TimeZone.of("Asia/Tehran")
    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.parse("2026-03-01T00:00:00Z")
        }
    private val officialMonths: IslamicMonthTable =
        IslamicMonthOverrides.parse(IslamicMonthOverrides.bundledIranOfficialText().orEmpty()).getOrThrow().table
    private val allSources = EventSource.entries.toSet() - EventSource.USER

    private fun repository(
        overrides: IslamicMonthTable?,
        variant: IslamicVariant = IslamicVariant.UMM_AL_QURA,
    ) = EventsRepository(
        settings =
            MutableStateFlow(
                EventsSettings(
                    preferences =
                        EventPreferences(
                            enabledSources = allSources,
                            homeTimeZone = tehran,
                            islamicVariant = variant,
                            islamicOverrides = overrides,
                        ),
                    weekend = setOf(Weekday.FRIDAY),
                    hijriOffset = null,
                ),
            ),
        inputs =
            EventInputs(
                personal = PersonalEventsSource { flowOf(emptyList()) },
                device = DeviceEventsSource { flowOf(emptyList()) },
                ics = IcsEventsSource { flowOf(emptyList()) },
            ),
        clock = clock,
        zones = flowOf(tehran),
        computeDispatcher = Dispatchers.Unconfined,
    )

    private fun hijri(
        year: Int,
        month: Int,
        day: Int,
    ) = CalendarDate(CalendarSystem.ISLAMIC, year, month, day)

    @Test
    fun `Iranian Islamic holidays are official with the override and computed without it`(): Unit =
        runTest {
            val eidAlFitr = EventId("ir.holiday.eid-al-fitr")
            val official = IranIslamicCalendar(officialMonths).toJdn(hijri(1447, 10, 1))
            repository(officialMonths).day(official).first().officialOrigins[eidAlFitr] shouldBe
                DateOrigin.OFFICIAL_OVERRIDE

            val computed = IranCrescentCalendar.toJdn(hijri(1447, 10, 1))
            val day = repository(overrides = null).day(computed).first()
            day.official.map { it.definition.id } shouldContain eidAlFitr
            day.officialOrigins[eidAlFitr] shouldBe DateOrigin.COMPUTED
        }

    @Test
    fun `Afghan Islamic holidays follow the tabular calendar whatever the user's variant`(): Unit =
        runTest {
            val eidAlAdha = EventId("af.holiday.eid-al-adha.1")
            val tabular = TabularIslamicCalendar.TYPE_II.toJdn(hijri(1447, 12, 10))
            listOf(IslamicVariant.UMM_AL_QURA, IslamicVariant.IRAN_OFFICIAL).forEach { variant ->
                val day = repository(overrides = null, variant = variant).day(tabular).first()
                withClue(variant) {
                    day.official.map { it.definition.id } shouldContain eidAlAdha
                    day.officialOrigins[eidAlAdha] shouldBe DateOrigin.COMPUTED
                }
            }
        }

    @Test
    fun `every Islamic-calendar occurrence of SH 1380 to 1480 has an origin, and no other occurrence does`(): Unit =
        runTest {
            val first = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1380, 1, 1))
            val last = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1481, 1, 1)).value - 1
            val repository = repository(officialMonths, IslamicVariant.IRAN_OFFICIAL)
            val range = JdnRange(first, Jdn(last))
            var islamic = 0
            repository.days(range).first().forEach { day ->
                day.official.forEach { occurrence ->
                    val id = occurrence.definition.id
                    if (occurrence.definition.calendar == CalendarSystem.ISLAMIC) {
                        islamic++
                        withClue("$id on ${day.jdn}") { (day.officialOrigins[id] != null) shouldBe true }
                    } else {
                        day.officialOrigins shouldNotContainKey id
                    }
                }
            }
            (islamic > 101 * 10) shouldBe true
        }
}
