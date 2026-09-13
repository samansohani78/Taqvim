/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.devicecalendar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.data.database.DeviceEventCacheEntity
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import org.junit.jupiter.api.Test

/** T-602 (U): all-day and timed instances dated in UTC+3:30 and UTC−8, windows, filtering and color parsing. */
class DeviceEventMappingTest {
    private val tehran = TimeZone.of("UTC+03:30")
    private val pacific = TimeZone.of("UTC-08:00")
    private val zones = listOf(tehran, pacific, TimeZone.of("Asia/Tehran"), TimeZone.of("America/Los_Angeles"))

    private fun millis(iso: String) = Instant.parse(iso).toEpochMilliseconds()

    private fun day(
        month: Int,
        day: Int,
    ): Jdn = LocalDate(2026, month, day).toJdn()

    private fun row(
        visible: Boolean = true,
        deleted: Boolean = false,
        title: String? = "t",
        color: Int? = null,
    ) = InstanceRow(7L, 3L, title, 10L, 20L, allDay = false, displayColor = color, visible = visible, deleted = deleted)

    @Test
    fun `an all-day event keeps its date in UTC+3_30 and UTC-8`() {
        val begin = millis("2026-01-10T00:00:00Z")
        val end = millis("2026-01-11T00:00:00Z")
        zones.forEach { zone ->
            DeviceEventMapping.days(begin, end, allDay = true, zone) shouldBe day(1, 10)..day(1, 10)
            DeviceEventMapping.days(begin, millis("2026-01-13T00:00:00Z"), allDay = true, zone) shouldBe
                day(1, 10)..day(1, 12)
        }
    }

    @Test
    fun `timed events take their dates from the device zone`() {
        val begin = millis("2026-01-10T22:00:00Z")
        val end = millis("2026-01-10T23:00:00Z")
        DeviceEventMapping.days(begin, end, allDay = false, tehran) shouldBe day(1, 11)..day(1, 11)
        DeviceEventMapping.days(begin, end, allDay = false, pacific) shouldBe day(1, 10)..day(1, 10)

        val untilLocalMidnight = millis("2026-01-10T20:30:00Z")
        DeviceEventMapping.days(millis("2026-01-10T18:30:00Z"), untilLocalMidnight, allDay = false, tehran) shouldBe
            day(1, 10)..day(1, 10)
        DeviceEventMapping.days(begin, begin, allDay = false, pacific) shouldBe day(1, 10)..day(1, 10)
    }

    @Test
    fun `every all-day date is kept in every zone offset and lies inside its window`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.int(7_300..43_800), Arb.int(-48..56)) { epochDay, quarters ->
                val zone = UtcOffset(hours = quarters / 4, minutes = quarters % 4 * 15).asTimeZone()
                val date = LocalDate.fromEpochDays(epochDay)
                val begin = epochDay * 86_400_000L
                val end = begin + 86_400_000L
                val days = date.toJdn()..date.toJdn()

                DeviceEventMapping.days(begin, end, allDay = true, zone) shouldBe days
                val window = DeviceEventMapping.window(days, zone)
                (window.fromEpochMillis <= begin && end <= window.toEpochMillis).shouldBeTrue()
            }
        }

    @Test
    fun `the previous day's all-day event overlaps the window but not the requested day`() {
        val days = day(1, 10)..day(1, 10)
        val window = DeviceEventMapping.window(days, tehran)
        window shouldBe InstantWindow(millis("2026-01-08T20:30:00Z"), millis("2026-01-11T20:30:00Z"))

        val previousBegin = millis("2026-01-09T00:00:00Z")
        val previous = DeviceEventMapping.days(previousBegin, millis("2026-01-10T00:00:00Z"), allDay = true, tehran)
        DeviceEventMapping.overlaps(previous, days).shouldBeFalse()
        DeviceEventMapping.overlaps(day(1, 8)..day(1, 12), days).shouldBeTrue()
        DeviceEventMapping.overlaps(day(1, 11)..day(1, 12), days).shouldBeFalse()
        shouldThrow<IllegalArgumentException> { DeviceEventMapping.window(JdnRange(day(1, 2), day(1, 1)), tehran) }
    }

    @Test
    fun `invisible calendars and deleted events are dropped and colors made opaque`() {
        DeviceEventMapping.toCacheEntity(row(visible = false)).shouldBeNull()
        DeviceEventMapping.toCacheEntity(row(deleted = true)).shouldBeNull()
        DeviceEventMapping.toCacheEntity(row(title = null, color = 0x00336699)) shouldBe
            DeviceEventCacheEntity(7L, 3L, 10L, 20L, allDay = false, title = "", colorArgb = 0xFF336699.toInt())
        DeviceEventMapping.toCacheEntity(row())?.colorArgb.shouldBeNull()
        DeviceEventMapping.opaque(0x80FF0000.toInt()) shouldBe 0xFFFF0000.toInt()
    }

    @Test
    fun `cache rows become dated events`() {
        val entity =
            DeviceEventCacheEntity(
                eventId = 1L,
                calendarId = 2L,
                beginEpochMillis = millis("2026-01-10T00:00:00Z"),
                endEpochMillis = millis("2026-01-12T00:00:00Z"),
                allDay = true,
                title = "Trip",
                colorArgb = 5,
            )
        DeviceEventMapping.toDeviceEvent(entity, pacific) shouldBe
            DeviceEvent(
                eventId = 1L,
                calendarId = 2L,
                title = "Trip",
                begin = Instant.parse("2026-01-10T00:00:00Z"),
                end = Instant.parse("2026-01-12T00:00:00Z"),
                allDay = true,
                colorArgb = 5,
                days = day(1, 10)..day(1, 11),
            )
    }
}
