/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.searchSunLongitude
import kotlin.math.roundToLong
import kotlin.time.Instant

/**
 * Astronomical events that calendar rules are defined by (ADR-0026), from cosinekitty/astronomy 2.1.19 (MIT). Library
 * types stay inside this object.
 *
 * Extension point: calendars whose rules need other events (the conjunction, or sunset and moonset at a site for
 * lunar month starts) add their searches here, next to [marchEquinox], so every calendar shares one ephemeris and one
 * conversion of library time to [Instant].
 */
internal object CalendarAstronomy {
    private const val MARCH_EQUINOX_LONGITUDE = 0.0
    private const val MARCH = 3
    private const val SEARCH_START_DAY = 10
    private const val SEARCH_DAYS = 20.0
    private const val SECONDS_PER_DAY = 86_400.0
    private const val J2000_UNIX_SECONDS = 946_728_000.0
    private const val MILLIS_PER_SECOND = 1_000.0

    /**
     * The March equinox of Gregorian [year]: the moment the Sun's apparent ecliptic longitude reaches 0°, searched
     * from 10 March over 20 days exactly as the library's own season search does. Null when the ephemeris finds none,
     * which happens only far outside its range (tens of thousands of years).
     */
    fun marchEquinox(year: Int): Instant? =
        searchSunLongitude(MARCH_EQUINOX_LONGITUDE, Time(year, MARCH, SEARCH_START_DAY, 0, 0, 0.0), SEARCH_DAYS)
            ?.let { instantOf(it.ut) }

    /** The instant [ut] days after 12:00 UTC on 1 January 2000 (the library's UT scale), to the millisecond. */
    fun instantOf(ut: Double): Instant =
        Instant.fromEpochMilliseconds(((ut * SECONDS_PER_DAY + J2000_UNIX_SECONDS) * MILLIS_PER_SECOND).roundToLong())
}
