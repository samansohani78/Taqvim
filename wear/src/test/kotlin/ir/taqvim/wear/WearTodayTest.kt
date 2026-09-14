/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarSystem
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test

/** T-1600: the watch's today model on the shared core (calendars, formatting, official events, prayer times). */
class WearTodayTest {
    private val calculator = WearFixtures.calculator

    @Test
    fun `Nowruz 1405 in Persian with its holiday, the other calendars and the next prayer`() {
        val today = calculator.today(WearFixtures.setup(), WearFixtures.NOWRUZ_MORNING)

        today.jdn shouldBe WearFixtures.NOWRUZ_MORNING.toJdn(WearFixtures.TEHRAN_ZONE)
        today.primaryDate shouldBe "شنبه ۱ فروردین ۱۴۰۵"
        today.dayLabel shouldBe "۱"
        today.monthName shouldBe "فروردین"
        today.dayOfMonth shouldBe 1
        today.monthLength shouldBe 31
        today.monthProgress shouldBe 1f / 31
        today.secondaryDates shouldHaveSize WearFixtures.setup().calendars.size - 1
        today.holidays.shouldNotBeEmpty()
        today.nextEvent.shouldNotBeNull().daysAway shouldBe 0
        val next = today.nextPrayer.shouldNotBeNull()
        next.prayer shouldBe WearPrayer.DHUHR
        next.clock.all { it == ':' || it in '۰'..'۹' } shouldBe true
    }

    @Test
    fun `after the last prayer the next one is tomorrow's Fajr`() {
        val lateEvening = Instant.parse("2026-03-21T20:00:00Z")
        val next = calculator.today(WearFixtures.setup(), lateEvening).nextPrayer.shouldNotBeNull()

        next.prayer shouldBe WearPrayer.FAJR
        next.at
            .toLocalDateTime(WearFixtures.TEHRAN_ZONE)
            .date.day shouldBe 22
    }

    @Test
    fun `the next prayer is always after now and within a day and a bit`(): Unit =
        runBlocking {
            val start = Instant.parse("2026-01-01T00:00:00Z").toEpochMilliseconds()
            val end = Instant.parse("2027-01-01T00:00:00Z").toEpochMilliseconds()
            val setup = WearFixtures.setup()
            checkAll(200, Arb.long(start..end)) { millis ->
                val now = Instant.fromEpochMilliseconds(millis)
                val next = calculator.today(setup, now).nextPrayer.shouldNotBeNull()
                (next.at > now) shouldBe true
                (next.at - now <= 26.hours) shouldBe true
            }
        }

    @Test
    fun `no chosen place means no prayer, and a polar day in Tromsø has none either`() {
        calculator.today(WearFixtures.setup(place = null), WearFixtures.NOWRUZ_MORNING).nextPrayer.shouldBeNull()

        val polarDay = Instant.parse("2026-06-21T10:00:00Z")
        val setup = WearFixtures.setup(place = WearFixtures.TROMSO, deviceZone = TimeZone.of("Europe/Oslo"))
        calculator.today(setup, polarDay).nextPrayer.shouldBeNull()
    }

    @Test
    fun `English uses its own main calendar, digits and the time zone of the place`() {
        val setup = WearFixtures.setup(language = "en", deviceZone = TimeZone.UTC)
        val today = calculator.today(setup, WearFixtures.NOWRUZ_MORNING)

        setup.zone shouldBe WearFixtures.TEHRAN_ZONE
        setup.primary.system shouldBe CalendarSystem.GREGORIAN
        today.primaryDate shouldContain "2026"
        today.dayLabel shouldBe "21"
        today.monthLength shouldBeGreaterThanOrEqual 28
    }
}
