/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.Jdn
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** JDN of 1970-01-01, which is epoch day 0 in kotlinx.datetime (T-107). */
public const val JDN_OF_UNIX_EPOCH: Long = 2_440_588L

/** Day number of this ISO (proleptic Gregorian) local date. */
public fun LocalDate.toJdn(): Jdn = Jdn(Math.addExact(toEpochDays(), JDN_OF_UNIX_EPOCH))

/**
 * The ISO local date of this day. Throws [IllegalArgumentException] when the day lies outside the range
 * supported by kotlinx.datetime (about ±1 000 000 000 years).
 */
public fun Jdn.toLocalDate(): LocalDate = LocalDate.fromEpochDays(Math.subtractExact(value, JDN_OF_UNIX_EPOCH))

/** The civil day containing this instant in [zone], honouring the zone's offset and DST rules at that instant. */
public fun Instant.toJdn(zone: TimeZone): Jdn = toLocalDateTime(zone).date.toJdn()

/** Source of "today" for UI and schedulers. Implementations choose the clock and the time zone. */
public fun interface TodayProvider {
    /** The current civil day. */
    public fun today(): Jdn
}

/**
 * [TodayProvider] reading [clock] in the zone returned by [zone] on every call, so a device time-zone change
 * takes effect immediately without recreating the provider.
 */
public class ClockTodayProvider(
    private val clock: Clock,
    private val zone: () -> TimeZone,
) : TodayProvider {
    override fun today(): Jdn = clock.now().toJdn(zone())
}
