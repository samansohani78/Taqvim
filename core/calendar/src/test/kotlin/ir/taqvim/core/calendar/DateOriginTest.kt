/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import org.junit.jupiter.api.Test

/** ADR-0037: each calendar reports whether a date is computed, an official override or a printed historical date. */
class DateOriginTest {
    private val ramadan1447 = UmmAlQuraCalendar.toJdn(CalendarDate(CalendarSystem.ISLAMIC, 1447, 9, 1))

    @Test
    fun `the Iranian calendar is computed without an override and official inside one`() {
        IranIslamicCalendar().originOf(ramadan1447) shouldBe DateOrigin.COMPUTED
        val official = IranIslamicCalendar(OfficialIranMonths.overrides.table)
        official.originOf(ramadan1447) shouldBe DateOrigin.OFFICIAL_OVERRIDE
        val later = official.toJdn(CalendarDate(CalendarSystem.ISLAMIC, 1460, 1, 1))
        official.originOf(later) shouldBe DateOrigin.COMPUTED
    }

    @Test
    fun `Umm al-Qura is published before AH 1420 and computed from then on`() {
        val published = UmmAlQuraCalendar.toJdn(CalendarDate(CalendarSystem.ISLAMIC, 1400, 1, 1))
        UmmAlQuraCalendar.originOf(published) shouldBe DateOrigin.PUBLISHED_CALENDAR
        UmmAlQuraCalendar.originOf(ramadan1447) shouldBe DateOrigin.COMPUTED
    }

    @Test
    fun `rule-based calendars are always computed`() {
        listOf(PersianCalendarSystem, GregorianCalendarSystem, TabularIslamicCalendar.TYPE_II, IranCrescentCalendar)
            .forEach { it.originOf(ramadan1447) shouldBe DateOrigin.COMPUTED }
    }
}
