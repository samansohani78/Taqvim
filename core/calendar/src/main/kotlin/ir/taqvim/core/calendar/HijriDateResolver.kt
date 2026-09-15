/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** A user correction of the lunar Hijri date by [days] (±2), set at [setAt] and expiring after [VALIDITY] (A-05). */
public data class HijriOffset(
    public val days: Int,
    public val setAt: Instant,
) {
    init {
        require(days in -MAX_DAYS..MAX_DAYS) { "Hijri offset must be within ±$MAX_DAYS days (was $days)" }
    }

    /** Whether the offset applies at [now]: from [setAt] until [VALIDITY] later. */
    public fun isActiveAt(now: Instant): Boolean = now >= setAt && now < setAt + VALIDITY

    public companion object {
        /** Largest correction in either direction. */
        public const val MAX_DAYS: Int = 2

        /** How long a correction stays in force. */
        public val VALIDITY: Duration = 30.days
    }
}

/** Where a displayed lunar Hijri date came from, for the "date source" explanation. */
public enum class HijriDateSource {
    /** The Calendar Center's published month starts. */
    OFFICIAL_TABLE,

    /** The estimate corrected by an active [HijriOffset]. */
    USER_OFFSET,

    /**
     * The calculated crescent estimate (A-06 Iran calibration) joined to the official table, for dates without
     * published data (ADR-0027).
     */
    CRESCENT_ESTIMATE,
}

/** A lunar Hijri [date] with its [source] and the [offsetDays] that were applied. */
public data class ResolvedHijriDate(
    public val date: CalendarDate,
    public val source: HijriDateSource,
    public val offsetDays: Int,
)

/** Resolves lunar Hijri dates with precedence official table > user offset > crescent estimate (A-05, ADR-0027). */
public class HijriDateResolver(
    private val clock: Clock,
    private val calendar: IranIslamicCalendar = IranIslamicCalendar(),
) {
    /** The lunar Hijri date of [jdn], honouring [offset] only where no official data exists. */
    public fun resolve(
        jdn: Jdn,
        offset: HijriOffset?,
    ): ResolvedHijriDate =
        when {
            calendar.isOfficial(jdn) -> {
                ResolvedHijriDate(calendar.fromJdn(jdn), HijriDateSource.OFFICIAL_TABLE, 0)
            }

            offset != null && offset.isActiveAt(clock.now()) -> {
                val corrected = calendar.fromJdn(Jdn(jdn.value + offset.days))
                ResolvedHijriDate(corrected, HijriDateSource.USER_OFFSET, offset.days)
            }

            else -> {
                ResolvedHijriDate(calendar.fromJdn(jdn), HijriDateSource.CRESCENT_ESTIMATE, 0)
            }
        }
}
