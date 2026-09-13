/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.GoldenFile
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/** T-502 against 30 synthetic calendars that reproduce the documented structure of real exports (golden/ics). */
class IcsFixturesTest {
    private data class Expectation(
        val events: Int,
        val warnings: Int,
        val check: (IcsCalendar) -> Unit = {},
    )

    private fun read(name: String): IcsParseResult = IcsReader.read(GoldenFile.load("golden/ics/$name.ics").body)

    private fun IcsCalendar.first(): IcsEvent = events.first()

    private val expectations: Map<String, Expectation> =
        mapOf(
            "01-google-timed-event" to
                Expectation(1, 0) {
                    it.productId shouldContain "Google"
                    it.first().start shouldBe IcsDateTime.Zoned(LocalDateTime(2026, 3, 21, 9, 0), "Asia/Tehran")
                    it.first().summary shouldBe "Synthetic meeting"
                },
            "02-google-all-day" to
                Expectation(1, 0) {
                    it.first().start shouldBe IcsDateTime.Date(LocalDate(2026, 3, 21))
                    it.first().end shouldBe IcsDateTime.Date(LocalDate(2026, 3, 22))
                },
            "03-google-weekly-until" to
                Expectation(1, 0) {
                    it.first().recurrence shouldBe
                        Recurrence(
                            Frequency.WEEKLY,
                            until = IcsDateTime.Utc(Instant.parse("2026-06-30T05:59:59Z")),
                            byDay =
                                listOf(Weekday.MONDAY, Weekday.WEDNESDAY, Weekday.FRIDAY).map { day ->
                                    WeekdayNum(day)
                                },
                        )
                },
            "04-google-exdate" to Expectation(1, 0) { it.first().exceptionDates shouldHaveSize 2 },
            "05-google-display-alarm" to
                Expectation(1, 0) {
                    it.first().alarms shouldBe
                        listOf(DisplayAlarm(AlarmTrigger.Relative((-30).minutes), "This is an event reminder"))
                },
            "06-outlook-windows-tzid" to
                Expectation(1, 2) {
                    it.first().start shouldBe IcsDateTime.Floating(LocalDateTime(2026, 3, 21, 9, 0))
                },
            "07-outlook-all-day" to
                Expectation(1, 0) { it.first().start shouldBe IcsDateTime.Date(LocalDate(2026, 4, 1)) },
            "08-outlook-monthly-byday" to
                Expectation(1, 0) {
                    it.first().recurrence shouldBe
                        Recurrence(Frequency.MONTHLY, count = 6, byDay = listOf(WeekdayNum(Weekday.TUESDAY, 2)))
                },
            "09-folded-description" to
                Expectation(1, 0) {
                    it.first().description shouldBe
                        "This description is folded over several physical lines so that no line exceeds " +
                        "seventy-five octets as RFC 5545 section 3.1 requires " +
                        "and the continuation here starts with a tab."
                },
            "10-escaped-text" to
                Expectation(1, 0) {
                    it.first().summary shouldBe "Tea, coffee; and cake"
                    it.first().description shouldBe "Line one\nLine two\nLine three with a backslash \\ here"
                },
            "11-utc-times" to
                Expectation(1, 0) {
                    it.first().start shouldBe IcsDateTime.Utc(Instant.parse("2026-05-01T08:30:00Z"))
                },
            "12-floating-times" to
                Expectation(1, 0) {
                    it.first().end shouldBe IcsDateTime.Floating(LocalDateTime(2026, 5, 1, 9, 30))
                },
            "13-multiple-events" to
                Expectation(5, 0) { it.events.map { e -> e.summary } shouldBe (1..5).map { d -> "Event $d" } },
            "14-daily-interval-count" to
                Expectation(
                    1,
                    0,
                ) { it.first().recurrence shouldBe Recurrence(Frequency.DAILY, interval = 2, count = 5) },
            "15-yearly-bymonthday" to
                Expectation(
                    1,
                    0,
                ) { it.first().recurrence shouldBe Recurrence(Frequency.YEARLY, byMonthDay = listOf(21)) },
            "16-monthly-last-day" to
                Expectation(1, 0) {
                    it.first().recurrence shouldBe
                        Recurrence(
                            Frequency.MONTHLY,
                            until = IcsDateTime.Date(LocalDate(2026, 12, 31)),
                            byMonthDay = listOf(-1),
                        )
                },
            "17-weekly-interval" to
                Expectation(1, 0) {
                    it.first().recurrence shouldBe
                        Recurrence(Frequency.WEEKLY, interval = 3, byDay = listOf(WeekdayNum(Weekday.SATURDAY)))
                },
            "18-unsupported-rrule-parts" to
                Expectation(1, 1) {
                    it.first().recurrence shouldBe
                        Recurrence(Frequency.YEARLY, byDay = listOf(WeekdayNum(Weekday.SUNDAY, -1)))
                },
            "19-audio-alarm" to
                Expectation(1, 1) {
                    it.first().alarms shouldBe listOf(DisplayAlarm(AlarmTrigger.Relative((-5).minutes), "Soon"))
                },
            "20-absolute-trigger" to
                Expectation(1, 0) {
                    it
                        .first()
                        .alarms
                        .single()
                        .trigger shouldBe
                        AlarmTrigger.Absolute(Instant.parse("2026-05-01T06:00:00Z"))
                },
            "21-related-end-trigger" to
                Expectation(1, 0) {
                    it
                        .first()
                        .alarms
                        .single()
                        .trigger shouldBe AlarmTrigger.Relative(10.minutes, relatedToEnd = true)
                },
            "22-lowercase-names" to
                Expectation(1, 0) { it.first().start shouldBe IcsDateTime.Date(LocalDate(2026, 6, 1)) },
            "23-x-and-unknown-properties" to
                Expectation(1, 0) {
                    it.first().extensions shouldBe mapOf("X-GOOGLE-CONFERENCE" to "https://example.org/meet")
                },
            "24-quoted-parameters" to
                Expectation(1, 0) {
                    it.first().start shouldBe IcsDateTime.Zoned(LocalDateTime(2026, 7, 10, 18, 0), "Europe/Berlin")
                },
            "25-persian-text" to
                Expectation(1, 0) {
                    it.first().summary shouldBe "جشن نوروز"
                },
            "26-missing-uid" to Expectation(1, 1) { it.first().uid shouldBe "valid@taqvim.test" },
            "27-missing-dtstart" to Expectation(0, 1),
            "30-unsupported-and-invalid-values" to
                Expectation(1, 4) {
                    val event = it.first()
                    event.end shouldBe null
                    event.recurrence shouldBe null
                    event.exceptionDates shouldBe listOf(IcsDateTime.Date(LocalDate(2026, 7, 2)))
                    event.recurrenceDates shouldBe listOf(IcsDateTime.Date(LocalDate(2026, 7, 10)))
                },
        )

    @TestFactory
    fun `readable fixtures give their events and warnings`(): List<DynamicTest> =
        expectations.map { (name, expected) ->
            DynamicTest.dynamicTest(name) {
                val result = read(name)
                withClue(result.toString()) {
                    result.shouldBeInstanceOf<IcsParseResult.Success>()
                    result.calendar.events shouldHaveSize expected.events
                    result.warnings shouldHaveSize expected.warnings
                    result.warnings.forEach { warning -> warning.line shouldBeGreaterThan 0 }
                    expected.check(result.calendar)
                }
            }
        }

    @TestFactory
    fun `structurally broken fixtures fail with line numbers`(): List<DynamicTest> =
        listOf("28-unbalanced-end", "29-no-vcalendar").map { name ->
            DynamicTest.dynamicTest(name) {
                val result = read(name)
                result.shouldBeInstanceOf<IcsParseResult.Failure>()
                result.errors.forEach { error -> error.line shouldBeGreaterThan 0 }
            }
        }

    @Test
    fun `there are 30 fixtures`() {
        (expectations.size + 2) shouldBe 30
    }
}
