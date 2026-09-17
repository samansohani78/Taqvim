/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.FakeClock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import org.junit.jupiter.api.Test

/** A-05 precedence (official override > user offset > computed, ADR-0027, ADR-0037) and offset expiry. */
class HijriDateResolverTest {
    private val clock = FakeClock()
    private val calendar = IranIslamicCalendar(OfficialIranMonths.overrides.table)
    private val resolver = HijriDateResolver(clock, calendar)

    private val officialDay = calendar.toJdn(CalendarDate(CalendarSystem.ISLAMIC, 1447, 10, 1))
    private val estimatedDay = calendar.toJdn(CalendarDate(CalendarSystem.ISLAMIC, 1450, 1, 10))

    @Test
    fun `official dates ignore an active offset`() {
        val resolved = resolver.resolve(officialDay, HijriOffset(1, clock.now()))

        resolved shouldBe ResolvedHijriDate(calendar.fromJdn(officialDay), HijriDateSource.OFFICIAL_TABLE, 0)
    }

    @Test
    fun `without an override every date is computed and the offset applies everywhere`() {
        val computed = HijriDateResolver(clock)

        computed.resolve(officialDay, null).source shouldBe HijriDateSource.CRESCENT_ESTIMATE
        computed.resolve(officialDay, HijriOffset(1, clock.now())).source shouldBe HijriDateSource.USER_OFFSET
    }

    @Test
    fun `an active offset corrects estimated dates`() {
        resolver.resolve(estimatedDay, HijriOffset(-1, clock.now())) shouldBe
            ResolvedHijriDate(CalendarDate(CalendarSystem.ISLAMIC, 1450, 1, 9), HijriDateSource.USER_OFFSET, -1)
        resolver.resolve(estimatedDay, null) shouldBe
            ResolvedHijriDate(CalendarDate(CalendarSystem.ISLAMIC, 1450, 1, 10), HijriDateSource.CRESCENT_ESTIMATE, 0)
    }

    @Test
    fun `an offset expires 30 days after it was set`() {
        val offset = HijriOffset(2, clock.now())

        clock.advanceBy(29.days + 23.hours)
        resolver.resolve(estimatedDay, offset).source shouldBe HijriDateSource.USER_OFFSET
        clock.advanceBy(1.hours)
        resolver.resolve(estimatedDay, offset).source shouldBe HijriDateSource.CRESCENT_ESTIMATE
    }

    @Test
    fun `an offset set in the future is not active yet`() {
        val offset = HijriOffset(1, clock.now() + 1.days)

        offset.isActiveAt(clock.now()) shouldBe false
        resolver.resolve(Jdn(estimatedDay.value), offset).source shouldBe HijriDateSource.CRESCENT_ESTIMATE
    }

    @Test
    fun `offsets are limited to two days`() {
        HijriOffset(-2, clock.now()).days shouldBe -2
        shouldThrow<IllegalArgumentException> { HijriOffset(3, clock.now()) }
        shouldThrow<IllegalArgumentException> { HijriOffset(-3, clock.now()) }
    }
}
