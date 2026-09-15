/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

/**
 * Mean lunar months continued beyond the astronomically computed months of a lunar calendar (Iranian crescent months,
 * ADR-0027; Umm al-Qura, ADR-0028). The interval from the edge month starting on [firstStart] to the edge month starting
 * on [lastStart], [months] months later, is spread evenly in whole days by integer division, so every continued month has
 * 29 or 30 days and the continuation meets both edges exactly. Arithmetic stays in [Long] for every month of the years
 * of [Int].
 */
internal class MeanLunarMonths(
    val firstStart: Long,
    val lastStart: Long,
    private val months: Long,
) {
    private val days = lastStart - firstStart

    /** JDN of the first day of the month [offset] months after the first edge month (negative values go back). */
    fun fromFirst(offset: Long): Long = firstStart + Math.floorDiv(Math.multiplyExact(offset, days), months)

    /** JDN of the first day of the month [offset] months after the last edge month. */
    fun fromLast(offset: Long): Long = lastStart + Math.floorDiv(Math.multiplyExact(offset, days), months)

    /** A month index near the one containing [jdn], counted in mean months from the last edge month at [lastIndex]. */
    fun estimateIndex(
        jdn: Long,
        lastIndex: Long,
    ): Long = lastIndex + Math.floorDiv(Math.multiplyExact(jdn - lastStart, months), days)
}
