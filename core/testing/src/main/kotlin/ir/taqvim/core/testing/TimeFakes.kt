/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.testing

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/** Deterministic [Clock] for tests: starts at [start] and only moves when told to. */
public class FakeClock(
    start: Instant = DEFAULT_START,
) : Clock {
    @Volatile
    private var current: Instant = start

    override fun now(): Instant = current

    /** Moves time forward by [duration]; time never goes backwards. */
    public fun advanceBy(duration: Duration) {
        require(!duration.isNegative()) { "FakeClock cannot move backwards (got $duration)" }
        current += duration
    }

    /** Jumps to [instant] (forwards or backwards), e.g. to simulate a manual time change. */
    public fun setTo(instant: Instant) {
        current = instant
    }

    public companion object {
        /** 1405-01-01 (Nowruz 2026, Tehran local midnight) expressed in UTC. */
        public val DEFAULT_START: Instant = Instant.parse("2026-03-20T20:30:00Z")
    }
}

/** Test double for "the device time zone": can be switched to simulate travel or a system time-zone change. */
public class FakeTimeZone(
    initial: TimeZone = TimeZones.TEHRAN,
) {
    @Volatile
    private var zone: TimeZone = initial

    /** The zone the device currently reports. */
    public val current: TimeZone
        get() = zone

    /** Changes the reported zone. */
    public fun set(timeZone: TimeZone) {
        zone = timeZone
    }
}

/** Time zones exercised across Taqvim tests (DST-abolished, no-DST, odd offsets, European DST, UTC−8). */
public object TimeZones {
    /** UTC+3:30; DST abolished in 2022. */
    public val TEHRAN: TimeZone = TimeZone.of("Asia/Tehran")

    /** UTC+4:30; no DST. */
    public val KABUL: TimeZone = TimeZone.of("Asia/Kabul")

    /** UTC+5:45; no DST. */
    public val KATHMANDU: TimeZone = TimeZone.of("Asia/Kathmandu")

    /** Central European time with DST. */
    public val BERLIN: TimeZone = TimeZone.of("Europe/Berlin")

    /** UTC−8 with DST. */
    public val LOS_ANGELES: TimeZone = TimeZone.of("America/Los_Angeles")

    /** Coordinated Universal Time. */
    public val UTC: TimeZone = TimeZone.UTC
}
