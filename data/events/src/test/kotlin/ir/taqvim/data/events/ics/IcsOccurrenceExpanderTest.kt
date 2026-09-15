/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.IcsDateTime
import ir.taqvim.core.ics.IcsEvent
import ir.taqvim.core.ics.Recurrence
import ir.taqvim.data.devicecalendar.InstantWindow
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1003 (U): subscribed events expanded into cache rows (RFC 5545 §3.8.5 recurrence set). */
class IcsOccurrenceExpanderTest {
    private val expander = IcsOccurrenceExpander(TimeZone.of("Asia/Tehran"))
    private val year2026 = InstantWindow(millis("2026-01-01T00:00:00Z"), millis("2027-01-01T00:00:00Z"))

    private fun millis(text: String): Long = Instant.parse(text).toEpochMilliseconds()

    private fun utc(text: String) = IcsDateTime.Utc(Instant.parse(text))

    private fun single(event: IcsEvent) = expander.expand(3, event, year2026).single()

    private fun starts(
        event: IcsEvent,
        window: InstantWindow = year2026,
    ): List<Long> = expander.expand(3, event, window).map { it.startEpochMillis }

    private fun date(
        month: Int,
        day: Int,
    ) = IcsDateTime.Date(LocalDate(2026, month, day))

    @Test
    fun `zoned weekly events keep their wall-clock time across a DST change`() {
        val rows =
            expander.expand(
                3,
                IcsEvent(
                    "berlin",
                    IcsDateTime.Zoned(LocalDateTime(2026, 3, 23, 9, 0), "Europe/Berlin"),
                    IcsDateTime.Zoned(LocalDateTime(2026, 3, 23, 10, 0), "Europe/Berlin"),
                    summary = "Standup",
                    recurrence = Recurrence(Frequency.WEEKLY, count = 3),
                ),
                year2026,
            )

        rows.map { it.startEpochMillis } shouldBe
            listOf(millis("2026-03-23T08:00:00Z"), millis("2026-03-30T07:00:00Z"), millis("2026-04-06T07:00:00Z"))
        rows.all { it.endEpochMillis - it.startEpochMillis == 1.hours.inWholeMilliseconds } shouldBe true
        rows.all { it.subscriptionId == 3L && it.uid == "berlin" && it.summary == "Standup" } shouldBe true
        rows.none { it.allDay } shouldBe true
    }

    @Test
    fun `all-day rows are UTC-midnight bounded, with EXDATE removed and RDATE added`() {
        val rows =
            expander.expand(
                3,
                IcsEvent(
                    "daily",
                    date(3, 21),
                    date(3, 22),
                    recurrence = Recurrence(Frequency.DAILY, count = 5),
                    exceptionDates = listOf(date(3, 23)),
                    recurrenceDates = listOf(date(4, 1), date(3, 21)),
                ),
                year2026,
            )

        rows.map { it.startEpochMillis } shouldBe
            listOf("03-21", "03-22", "03-24", "03-25", "04-01").map { millis("2026-${it}T00:00:00Z") }
        rows.all { it.allDay && it.endEpochMillis - it.startEpochMillis == 1.days.inWholeMilliseconds } shouldBe true
    }

    @Test
    fun `overrides replace or cancel the instance their RECURRENCE-ID names and orphans stand alone`() {
        val berlin = "Europe/Berlin"
        val series =
            IcsEvent(
                "s",
                IcsDateTime.Zoned(LocalDateTime(2026, 6, 1, 9, 0), berlin),
                IcsDateTime.Zoned(LocalDateTime(2026, 6, 1, 10, 0), berlin),
                summary = "Series",
                recurrence = Recurrence(Frequency.DAILY, count = 4),
            )
        val moved =
            IcsEvent(
                "s",
                utc("2026-06-02T15:00:00Z"),
                utc("2026-06-02T15:30:00Z"),
                summary = "Moved",
                recurrenceId = utc("2026-06-02T07:00:00Z"),
            )
        val cancelledAt = IcsDateTime.Zoned(LocalDateTime(2026, 6, 3, 9, 0), berlin)
        val cancelled = IcsEvent("s", cancelledAt, recurrenceId = cancelledAt, cancelled = true)
        val orphan =
            IcsEvent("o", utc("2026-06-10T10:00:00Z"), summary = "Alone", recurrenceId = utc("2026-06-09T10:00:00Z"))
        val cancelledOrphan = orphan.copy(uid = "gone", cancelled = true)

        val rows = expander.expandAll(3, listOf(moved, series, cancelled, orphan, cancelledOrphan), year2026)

        rows.map { Triple(it.uid, it.summary, it.startEpochMillis) } shouldBe
            listOf(
                Triple("s", "Series", millis("2026-06-01T07:00:00Z")),
                Triple("s", "Moved", millis("2026-06-02T15:00:00Z")),
                Triple("s", "Series", millis("2026-06-04T07:00:00Z")),
                Triple("o", "Alone", millis("2026-06-10T10:00:00Z")),
            )
        rows[1].endEpochMillis - rows[1].startEpochMillis shouldBe 30.minutes.inWholeMilliseconds
        val allDay = IcsEvent("d", date(6, 1), recurrence = Recurrence(Frequency.DAILY, count = 3))
        val movedDay = IcsEvent("d", date(6, 5), recurrenceId = date(6, 2))
        expander.expandAll(3, listOf(allDay, movedDay), year2026).map { it.startEpochMillis } shouldBe
            listOf("06-01", "06-03", "06-05").map { millis("2026-${it}T00:00:00Z") }
    }

    @Test
    fun `a date-time UNTIL includes the occurrence starting exactly then`() {
        val event =
            IcsEvent(
                "until",
                IcsDateTime.Zoned(LocalDateTime(2026, 7, 10, 1, 0), "Asia/Tehran"),
                recurrence = Recurrence(Frequency.DAILY, until = utc("2026-07-11T21:30:00Z")),
            )

        starts(event) shouldBe
            listOf(millis("2026-07-09T21:30:00Z"), millis("2026-07-10T21:30:00Z"), millis("2026-07-11T21:30:00Z"))
        starts(event.copy(recurrence = Recurrence(Frequency.DAILY, until = date(7, 11)))) shouldHaveSize 2
    }

    @Test
    fun `rows are clipped to the window and open-ended rules stop`() {
        val window = InstantWindow(millis("2026-06-01T00:00:00Z"), millis("2026-06-04T00:00:00Z"))
        val open =
            IcsEvent("open", utc("2026-05-31T23:30:00Z"), utc("2026-06-01T00:30:00Z"))

        starts(open.copy(recurrence = Recurrence(Frequency.DAILY)), window) shouldBe
            listOf("2026-05-31T23:30:00Z", "2026-06-01T23:30:00Z", "2026-06-02T23:30:00Z", "2026-06-03T23:30:00Z")
                .map(::millis)
        starts(IcsEvent("point", utc("2026-06-01T00:00:00Z")), window) shouldHaveSize 1
        starts(IcsEvent("after", utc("2026-06-04T00:00:00Z")), window) shouldHaveSize 0
        starts(IcsEvent("before", utc("2026-05-31T12:00:00Z")), window) shouldHaveSize 0
        IcsOccurrenceExpander(TimeZone.UTC, maxOccurrences = 10)
            .expand(3, IcsEvent("many", date(1, 1), recurrence = Recurrence(Frequency.DAILY)), year2026)
            .shouldHaveSize(10)
    }

    @Test
    fun `floating times use the device zone and missing ends give zero or one-day durations`() {
        val floating = single(IcsEvent("f", IcsDateTime.Floating(LocalDateTime(2026, 6, 1, 12, 0))))
        floating.startEpochMillis shouldBe millis("2026-06-01T08:30:00Z")
        floating.endEpochMillis shouldBe floating.startEpochMillis

        val mixed = single(IcsEvent("m", date(6, 1), IcsDateTime.Floating(LocalDateTime(2026, 6, 3, 0, 0))))
        mixed.endEpochMillis - mixed.startEpochMillis shouldBe 1.days.inWholeMilliseconds
        val backwards = single(IcsEvent("b", utc("2026-06-01T12:00:00Z"), utc("2026-06-01T11:00:00Z")))
        backwards.endEpochMillis shouldBe backwards.startEpochMillis
    }
}
