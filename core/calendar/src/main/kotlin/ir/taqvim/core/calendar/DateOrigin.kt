/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.Jdn

/** Where a shown date comes from, so screens can label it (ADR-0037). */
public enum class DateOrigin {
    /** Computed by the calendar's rule or astronomy. */
    COMPUTED,

    /** Taken from an official override the user switched on or imported. */
    OFFICIAL_OVERRIDE,

    /** Taken from a printed historical calendar that no rule reproduces (Umm al-Qura AH 1300–1419, ADR-0028). */
    PUBLISHED_CALENDAR,
}

/** The [DateOrigin] of the date this calendar gives for [jdn]. */
public fun CalendarArithmetic.originOf(jdn: Jdn): DateOrigin =
    when (this) {
        is IranIslamicCalendar -> {
            if (isOfficial(jdn)) DateOrigin.OFFICIAL_OVERRIDE else DateOrigin.COMPUTED
        }

        UmmAlQuraCalendar -> {
            if (UmmAlQuraCalendar.isBundled(fromJdn(jdn).year)) DateOrigin.PUBLISHED_CALENDAR else DateOrigin.COMPUTED
        }

        else -> {
            DateOrigin.COMPUTED
        }
    }
