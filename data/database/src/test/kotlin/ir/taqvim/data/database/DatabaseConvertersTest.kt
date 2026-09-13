/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.ics.WeekdayNum
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.workdays.LeaveRange
import org.junit.jupiter.api.Test

/** T-601: column encodings round-trip, including empty collections and empty shift labels. */
class DatabaseConvertersTest {
    private val collections = CollectionConverters()
    private val calendar = CalendarConverters()

    @Test
    fun `collections round-trip`() {
        listOf(emptySet(), setOf(Weekday.SUNDAY, Weekday.MONDAY), Weekday.entries.toSet()).forEach {
            collections.columnToWeekdays(collections.weekdaysToColumn(it)) shouldBe it
        }
        collections.weekdaysToColumn(setOf(Weekday.FRIDAY, Weekday.THURSDAY)) shouldBe "THURSDAY,FRIDAY"
        listOf(emptySet(), EventSource.entries.toSet()).forEach {
            collections.columnToSources(collections.sourcesToColumn(it)) shouldBe it
        }
        listOf(emptyList(), listOf(-1, 15, 31)).forEach {
            collections.columnToInts(collections.intsToColumn(it)) shouldBe it
        }
        listOf(emptyList(), listOf(""), listOf("D", "", "N, night", "")).forEach {
            collections.columnToLabels(collections.labelsToColumn(it)) shouldBe it
        }
        shouldThrow<IllegalArgumentException> { collections.labelsToColumn(listOf("a\u001Fb")) }
    }

    @Test
    fun `calendar values round-trip`() {
        val days = listOf(WeekdayNum(Weekday.FRIDAY), WeekdayNum(Weekday.MONDAY, -1), WeekdayNum(Weekday.SUNDAY, 53))
        calendar.weekdayNumsToColumn(days) shouldBe "FRIDAY,-1@MONDAY,53@SUNDAY"
        calendar.columnToWeekdayNums(calendar.weekdayNumsToColumn(days)) shouldBe days
        calendar.columnToWeekdayNums("") shouldBe emptyList()

        val leave = listOf(LeaveRange(Jdn(-10L), Jdn(-2L)), LeaveRange(Jdn(2_460_000L), Jdn(2_460_004L)))
        calendar.columnToLeave(calendar.leaveToColumn(leave)) shouldBe leave
        calendar.columnToLeave("") shouldBe emptyList()
    }
}
