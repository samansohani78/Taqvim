/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Weekday
import org.junit.jupiter.api.Test

/**
 * F-06: the Islamic variants side by side. The point of this screen is the disagreement — a user whose family keeps
 * a different method wants to see both readings, not be told one of them.
 */
class IslamicVariantComparisonTest {
    private fun calendars(variant: IslamicVariant) =
        CalendarCalendars(
            CalendarSettings(
                calendars = listOf(CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC),
                weekStart = Weekday.SATURDAY,
                islamicVariant = variant,
                languageCode = "fa",
            ),
        )

    private val day = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 6, 22))

    @Test
    fun `every variant is read, with the user's own first`() {
        val rows = calendars(IslamicVariant.UMM_AL_QURA).islamicVariantDates(day)

        rows.map { it.variant } shouldContainExactly
            listOf(IslamicVariant.UMM_AL_QURA) + (IslamicVariant.entries - IslamicVariant.UMM_AL_QURA)
        rows.first().isPreferred shouldBe true
        rows.drop(1).all { !it.isPreferred } shouldBe true
        rows.all { it.date.system == CalendarSystem.ISLAMIC } shouldBe true
    }

    @Test
    fun `the variants really do disagree, which is why the comparison exists`() {
        // If every method agreed on every day there would be nothing to show. Over a year the readings must differ
        // somewhere; asserting that keeps this screen honest if the engines are ever collapsed into one.
        val year = (0 until 365).map { day + it }
        val disagreeing =
            year.count { probe ->
                calendars(IslamicVariant.IRAN_OFFICIAL)
                    .islamicVariantDates(probe)
                    .map { it.date.day }
                    .distinct()
                    .size >
                    1
            }

        (disagreeing > 0) shouldBe true
    }

    @Test
    fun `the chosen variant's reading matches the date the rest of the screen shows`() {
        // The comparison must not be a second opinion computed a different way: the preferred row has to equal what
        // datesOf already produces for the Islamic calendar, or the screen would contradict itself.
        val calendars = calendars(IslamicVariant.IRAN_OFFICIAL)
        val shown = calendars.datesOf(day).first { it.system == CalendarSystem.ISLAMIC }

        calendars.islamicVariantDates(day).first { it.isPreferred }.date shouldBe shown
    }
}
