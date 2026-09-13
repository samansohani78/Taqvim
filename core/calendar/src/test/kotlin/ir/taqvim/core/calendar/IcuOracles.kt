/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import com.ibm.icu.util.Calendar
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem

/** The Islamic date an ICU4J calendar currently points at (proleptic extended year, 1-based month). */
internal fun Calendar.currentHijriDate(): CalendarDate =
    CalendarDate(
        CalendarSystem.ISLAMIC,
        get(Calendar.EXTENDED_YEAR),
        get(Calendar.MONTH) + 1,
        get(Calendar.DAY_OF_MONTH),
    )
