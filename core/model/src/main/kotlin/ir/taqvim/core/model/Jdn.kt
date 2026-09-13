/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.model

/**
 * Julian Day Number: an integer count of days, the canonical calendar-independent day representation
 * (docs/PLAN.md §0.3). JDN 0 is the day that began at noon on 1 January 4713 BC in the proleptic Julian
 * calendar; JDN 2 451 545 is 1 January 2000 (Gregorian).
 *
 * Arithmetic is overflow-checked: exceeding the [Long] range throws [ArithmeticException] instead of wrapping.
 */
@JvmInline
public value class Jdn(
    public val value: Long,
) : Comparable<Jdn> {
    /** The day [days] after this one (negative values go back in time). */
    public operator fun plus(days: Long): Jdn = Jdn(Math.addExact(value, days))

    /** The day [days] after this one (negative values go back in time). */
    public operator fun plus(days: Int): Jdn = plus(days.toLong())

    /** The day [days] before this one. */
    public operator fun minus(days: Long): Jdn = Jdn(Math.subtractExact(value, days))

    /** The day [days] before this one. */
    public operator fun minus(days: Int): Jdn = minus(days.toLong())

    /** Signed number of days from [other] to this day (`this - other`). */
    public operator fun minus(other: Jdn): Long = Math.subtractExact(value, other.value)

    /** The next day. */
    public operator fun inc(): Jdn = plus(1L)

    /** The previous day. */
    public operator fun dec(): Jdn = minus(1L)

    /** Inclusive range of days from this day to [endInclusive]. */
    public operator fun rangeTo(endInclusive: Jdn): JdnRange = JdnRange(this, endInclusive)

    override fun compareTo(other: Jdn): Int = value.compareTo(other.value)

    /**
     * ISO weekday of this day. JDN 0 was a Monday, so the weekday is `JDN mod 7` counted from Monday
     * (equivalent to Meeus, *Astronomical Algorithms*, ch. 7: `(JD + 1.5) mod 7` with 0 = Sunday).
     */
    public fun weekday(): Weekday = Weekday.entries[Math.floorMod(value, DAYS_PER_WEEK).toInt()]

    override fun toString(): String = "Jdn($value)"

    private companion object {
        const val DAYS_PER_WEEK = 7L
    }
}

/** An inclusive, iterable range of days. Empty when [endInclusive] precedes [start]. */
public class JdnRange(
    override val start: Jdn,
    override val endInclusive: Jdn,
) : ClosedRange<Jdn>,
    Iterable<Jdn> {
    /** Number of days in the range (0 when empty). */
    public val dayCount: Long
        get() = if (isEmpty()) 0L else Math.addExact(endInclusive - start, 1L)

    override fun iterator(): Iterator<Jdn> =
        object : Iterator<Jdn> {
            private var next = start.value
            private val last = endInclusive.value

            override fun hasNext(): Boolean = next <= last

            override fun next(): Jdn {
                if (next > last) throw NoSuchElementException("JdnRange exhausted")
                return Jdn(next++)
            }
        }

    override fun equals(other: Any?): Boolean {
        if (other !is JdnRange) return false
        return (isEmpty() && other.isEmpty()) || (start == other.start && endInclusive == other.endInclusive)
    }

    override fun hashCode(): Int = if (isEmpty()) -1 else 31 * start.hashCode() + endInclusive.hashCode()

    override fun toString(): String = "$start..$endInclusive"
}
