/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import ir.taqvim.core.model.Jdn

/** One instance of a series: the day it [original]ly occurs on and the [override] that replaces it, if any. */
public data class SeriesInstance<out T>(
    public val original: Jdn,
    public val override: T?,
)

/**
 * The instances of a series (T-1003) whose [occurrences] are ascending days, after removing the [excluded] days
 * (RFC 5545 EXDATE, §3.8.5.1) and replacing the instances that [overrides] name by their original day (RECURRENCE-ID,
 * §3.8.4.4). Plain instances are kept when their day lies in [from]..[until]; an overridden instance is kept wherever
 * its original day lies, because the override may move it, so the caller checks where it lands. An override of a day
 * that is not an occurrence, or that is also excluded, is ignored.
 */
public fun <T> seriesInstances(
    occurrences: Sequence<Jdn>,
    excluded: Set<Jdn>,
    overrides: Map<Jdn, T>,
    from: Jdn,
    until: Jdn,
): List<SeriesInstance<T>> {
    val last = overrides.keys.fold(until) { latest, day -> maxOf(latest, day) }
    return occurrences
        .takeWhile { it <= last }
        .filter { it !in excluded }
        .mapNotNull { day ->
            val override = overrides[day]
            when {
                override != null -> SeriesInstance(day, override)
                day in from..until -> SeriesInstance(day, null)
                else -> null
            }
        }.toList()
}
