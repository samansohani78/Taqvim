/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.IranCrescentCalendar
import ir.taqvim.core.calendar.IslamicMonthTable
import ir.taqvim.core.calendar.TabularIslamicCalendar
import ir.taqvim.core.calendar.UmmAlQuraCalendar
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/** T-302: truth table over source × holidays-only × time zone × validity (plus holiday and always-displayed). */
class EventVisibilityPolicyTest {
    private val home = TimeZone.of("Asia/Tehran")
    private val abroad = TimeZone.of("Europe/Berlin")
    private val citation = Citation("https://example.org", "test")

    /** Validity cases for an occurrence on 1 Farvardin 1404. */
    private enum class ValidityCase(
        val validity: Validity?,
    ) {
        NONE(null),
        INSIDE(Validity(CalendarSystem.PERSIAN, 1400, 1410, Citation("https://example.org", "test"))),
        OUTSIDE(Validity(CalendarSystem.PERSIAN, 1405, null, Citation("https://example.org", "test"))),
    }

    private data class Row(
        val sourceEnabled: Boolean,
        val holidaysOnly: Boolean,
        val hideAway: Boolean,
        val away: Boolean,
        val validity: ValidityCase,
        val holiday: Boolean,
        val alwaysDisplayed: Boolean,
    ) {
        /** The documented rule order, restated independently of the implementation. */
        val expected: Boolean
            get() =
                when {
                    validity == ValidityCase.OUTSIDE -> false
                    alwaysDisplayed -> true
                    !sourceEnabled -> false
                    holidaysOnly && !holiday -> false
                    hideAway && away && !holiday -> false
                    else -> true
                }
    }

    private fun occurrence(row: Row): Occurrence {
        val definition =
            event("test.observance", CalendarSystem.PERSIAN, EventRule.Fixed(1, 1), isHoliday = row.holiday).copy(
                source = EventSource.IRAN_OFFICIAL,
                category = EventCategory.RELIGIOUS,
                validity = row.validity.validity,
                flags = if (row.alwaysDisplayed) setOf(EventFlag.ALWAYS_DISPLAYED) else emptySet(),
            )
        return OccurrenceCalculator(listOf(definition)).occurrences(definition, 1404).single()
    }

    private fun policy(row: Row) =
        EventVisibilityPolicy(
            EventPreferences(
                enabledSources =
                    if (row.sourceEnabled) {
                        setOf(
                            EventSource.IRAN_OFFICIAL,
                        )
                    } else {
                        setOf(EventSource.INTERNATIONAL)
                    },
                homeTimeZone = home,
                holidaysOnly = row.holidaysOnly,
                hideReligiousOutsideHomeZone = row.hideAway,
            ),
        )

    private val rows: List<Row> =
        listOf(true, false).flatMap { sourceEnabled ->
            listOf(true, false).flatMap { holidaysOnly ->
                listOf(true, false).flatMap { hideAway ->
                    listOf(true, false).flatMap { away ->
                        ValidityCase.entries.flatMap { validity ->
                            listOf(true, false).flatMap { holiday ->
                                listOf(true, false).map { always ->
                                    Row(sourceEnabled, holidaysOnly, hideAway, away, validity, holiday, always)
                                }
                            }
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `visibility truth table`(): List<DynamicTest> {
        rows shouldHaveSize 192
        return rows.map { row ->
            DynamicTest.dynamicTest(row.toString()) {
                policy(row).isVisible(occurrence(row), if (row.away) abroad else home) shouldBe row.expected
            }
        }
    }

    @Test
    fun `only religious non-holidays are hidden away from home`() {
        val national =
            event(
                "test.national",
                CalendarSystem.GREGORIAN,
                EventRule.Fixed(5, 1),
            ).copy(category = EventCategory.NATIONAL)
        val occurrence = OccurrenceCalculator(listOf(national)).occurrences(national, 2026).single()
        val preferences = EventPreferences(setOf(EventSource.INTERNATIONAL), home, hideReligiousOutsideHomeZone = true)

        EventVisibilityPolicy(preferences).visible(listOf(occurrence), abroad) shouldBe listOf(occurrence)
    }

    @Test
    fun `a validity in an unavailable calendar hides the occurrence`() {
        val nepaliValidity = Validity(CalendarSystem.NEPALI, 2080, null, citation)
        val definition =
            event(
                "test.nepali-valid",
                CalendarSystem.GREGORIAN,
                EventRule.Fixed(5, 1),
            ).copy(validity = nepaliValidity)
        val occurrence = OccurrenceCalculator(listOf(definition)).occurrences(definition, 2026).single()

        val preferences = EventPreferences(EventSource.entries.toSet(), home)
        // Bikram Sambat 2083 is within the validity; a provider without the calendar hides the occurrence.
        EventVisibilityPolicy(preferences).isVisible(occurrence, home) shouldBe true
        val withoutNepali =
            CalendarProvider { system ->
                CalendarProvider.DEFAULT.calendarFor(system).takeIf {
                    system !=
                        CalendarSystem.NEPALI
                }
            }
        val sources = IslamicCalendarSelection(preferences.islamicVariant, base = withoutNepali)
        EventVisibilityPolicy(preferences, sources).isVisible(occurrence, home) shouldBe false
    }

    @Test
    fun `Islamic variants are selected per source`() {
        val selection = IslamicCalendarSelection(IslamicVariant.UMM_AL_QURA)

        selection.variantFor(EventSource.IRAN_OFFICIAL) shouldBe IslamicVariant.IRAN_OFFICIAL
        selection.variantFor(EventSource.INTERNATIONAL) shouldBe IslamicVariant.UMM_AL_QURA
        selection.providerFor(EventSource.INTERNATIONAL).calendarFor(CalendarSystem.ISLAMIC) shouldBeSameInstanceAs
            UmmAlQuraCalendar
        selection.providerFor(EventSource.INTERNATIONAL).calendarFor(CalendarSystem.GREGORIAN) shouldBeSameInstanceAs
            GregorianCalendarSystem
        IslamicCalendarSelection.calendarFor(IslamicVariant.TABULAR_15) shouldBeSameInstanceAs
            TabularIslamicCalendar.TYPE_I
        IslamicCalendarSelection.calendarFor(IslamicVariant.TABULAR_16) shouldBeSameInstanceAs
            TabularIslamicCalendar.TYPE_II
        IslamicCalendarSelection.calendarFor(IslamicVariant.CALCULATED_OBSERVATIONAL) shouldBeSameInstanceAs
            IranCrescentCalendar
        IslamicCalendarSelection(
            IslamicVariant.TABULAR_16,
            bySource = emptyMap(),
        ).variantFor(EventSource.IRAN_OFFICIAL) shouldBe
            IslamicVariant.TABULAR_16
    }

    @Test
    fun `official Iranian months apply only when an override is given`() {
        val first = CalendarDate(CalendarSystem.ISLAMIC, 1450, 1, 1)
        val start = IranCrescentCalendar.toJdn(first).value + 1
        val table = IslamicMonthTable(1450, 1, start, listOf(29, 30))
        val computed = IslamicCalendarSelection(IslamicVariant.IRAN_OFFICIAL)
        val official = IslamicCalendarSelection(IslamicVariant.IRAN_OFFICIAL, overrides = table)

        computed.calendarOf(IslamicVariant.IRAN_OFFICIAL).toJdn(first) shouldBe IranCrescentCalendar.toJdn(first)
        official.calendarOf(IslamicVariant.IRAN_OFFICIAL).toJdn(first).value shouldBe start
        official.calendarOf(IslamicVariant.UMM_AL_QURA) shouldBeSameInstanceAs UmmAlQuraCalendar
        official
            .providerFor(
                EventSource.IRAN_OFFICIAL,
            ).calendarFor(CalendarSystem.ISLAMIC)
            ?.toJdn(first)
            ?.value shouldBe
            start
        IslamicCalendarSelection
            .arithmeticFor(CalendarSystem.ISLAMIC, IslamicVariant.IRAN_OFFICIAL, table)
            .toJdn(first)
            .value shouldBe start
        EventVisibilityPolicy(EventPreferences(EventSource.entries.toSet(), home, islamicOverrides = table))
            .calendars
            .providerFor(EventSource.IRAN_OFFICIAL)
            .calendarFor(CalendarSystem.ISLAMIC)
            ?.toJdn(first)
            ?.value shouldBe start
    }

    @Test
    fun `the lookup uses each source's Islamic variant`() {
        val official =
            event(
                "test.fitr-official",
                CalendarSystem.ISLAMIC,
                EventRule.Fixed(10, 1),
            ).copy(source = EventSource.IRAN_OFFICIAL)
        val international = event("test.fitr-uaq", CalendarSystem.ISLAMIC, EventRule.Fixed(10, 1))
        val selection = IslamicCalendarSelection(IslamicVariant.UMM_AL_QURA)
        val lookup = EventLookup(listOf(official, international), selection)
        val shawwal = CalendarDate(CalendarSystem.ISLAMIC, 1447, 10, 1)

        val days =
            lookup.occurrencesIn(CalendarSystem.ISLAMIC, 1447, EventSource.entries.toSet()).associate {
                it.definition.id.value to
                    it.jdn
            }

        days.getValue("test.fitr-official") shouldBe
            IslamicCalendarSelection.calendarFor(IslamicVariant.IRAN_OFFICIAL).toJdn(shawwal)
        days.getValue("test.fitr-uaq") shouldBe UmmAlQuraCalendar.toJdn(shawwal)
        EventVisibilityPolicy(
            EventPreferences(EventSource.entries.toSet(), home),
            selection,
        ).calendars shouldBeSameInstanceAs
            selection
    }
}
