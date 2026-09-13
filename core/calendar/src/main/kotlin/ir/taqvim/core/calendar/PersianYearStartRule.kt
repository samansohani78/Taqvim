/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.Jdn
import kotlin.time.Instant
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toLocalDateTime

/**
 * The A-02 year-start rule: 1 Farvardin is the day, reckoned at the Tehran meridian (52.5°E), whose noon follows the
 * March equinox. An equinox before noon starts the year that day; at or after noon, the next day.
 */
public object PersianYearStartRule {
    /** Mean solar time at 52.5°E (3 h 30 min east of Greenwich), which is also Iran Standard Time. */
    public val TEHRAN_MERIDIAN_OFFSET: UtcOffset = UtcOffset(hours = 3, minutes = 30)

    private val TEHRAN_MERIDIAN = FixedOffsetTimeZone(TEHRAN_MERIDIAN_OFFSET)
    private val NOON = LocalTime(hour = 12, minute = 0)

    /** JDN of 1 Farvardin of the year whose March equinox occurs at [equinox]. */
    public fun firstDayOfYear(equinox: Instant): Jdn {
        val local = equinox.toLocalDateTime(TEHRAN_MERIDIAN)
        val day = local.date.toJdn()
        return if (local.time < NOON) day else Jdn(day.value + 1)
    }
}
