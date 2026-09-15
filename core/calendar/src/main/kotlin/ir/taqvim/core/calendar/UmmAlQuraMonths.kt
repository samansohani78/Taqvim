/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import kotlin.math.floor

/**
 * First days of every Umm al-Qura month (ADR-0028), addressed by a month index: 0 is Muharram of
 * [PUBLISHED_FIRST_YEAR], and each month adds one.
 *
 * - [PUBLISHED_FIRST_YEAR]‥[PUBLISHED_LAST_YEAR]: the published calendar ([UMM_AL_QURA_MONTH_MASKS]).
 * - Other years of [FIRST_ASTRONOMICAL_YEAR]‥[LAST_ASTRONOMICAL_YEAR]: the criterion since AH 1423
 *   ([UmmAlQuraCriterion]), applied forward from each month's own start, computed on first use [BLOCK_MONTHS] at a
 *   time and kept. Before the published years the rule is applied proleptically; after them it is the calendar's
 *   own rule.
 * - Every other year: mean lunar months continued from the nearer astronomical edge ([MeanLunation]); months keep 29
 *   or 30 days and join the astronomical months without a seam.
 */
internal object UmmAlQuraMonths {
    const val PUBLISHED_FIRST_YEAR: Int = 1300
    const val PUBLISHED_LAST_YEAR: Int = 1450
    const val FIRST_ASTRONOMICAL_YEAR: Int = -3000
    const val LAST_ASTRONOMICAL_YEAR: Int = 3000

    /** JDN of 1 Muharram [PUBLISHED_FIRST_YEAR]. */
    const val PUBLISHED_START_JDN: Long = 2_408_762L

    private const val MONTHS = 12
    private const val LONG_MONTH = 30
    private const val SHORT_MONTH = 29
    private const val BLOCK_MONTHS = 120
    private const val MEAN_SYNODIC_MONTH = 29.530588853

    /** Month starts of the published years, followed by 1 Muharram [PUBLISHED_LAST_YEAR] + 1. */
    private val published: LongArray =
        UMM_AL_QURA_MONTH_MASKS
            .flatMap { mask -> (1..MONTHS).map { month -> maskedLength(mask, month) } }
            .runningFold(PUBLISHED_START_JDN) { start, length -> start + length }
            .toLongArray()

    private val publishedMonths: Long = (published.size - 1).toLong()
    val firstAstronomicalIndex: Long = index(FIRST_ASTRONOMICAL_YEAR.toLong(), 1)
    val lastAstronomicalIndex: Long = index(LAST_ASTRONOMICAL_YEAR.toLong(), MONTHS)

    private val blocks: List<Lazy<LongArray>> =
        List(((lastAstronomicalIndex - firstAstronomicalIndex) / BLOCK_MONTHS + 1).toInt()) { lazy { block(it) } }

    private val mean: Lazy<MeanLunation> =
        lazy {
            MeanLunation(
                astronomicalStart(firstAstronomicalIndex),
                astronomicalStart(lastAstronomicalIndex),
                lastAstronomicalIndex - firstAstronomicalIndex,
            )
        }

    /** Month index of [month] (1‥12) of [year]. */
    fun index(
        year: Long,
        month: Int,
    ): Long = (year - PUBLISHED_FIRST_YEAR) * MONTHS + month - 1

    /** Year of month [index]. */
    fun yearOf(index: Long): Long = PUBLISHED_FIRST_YEAR + Math.floorDiv(index, MONTHS.toLong())

    /** Month (1‥12) of month [index]. */
    fun monthOf(index: Long): Int = Math.floorMod(index, MONTHS.toLong()).toInt() + 1

    /** Whether month [index] lies in the published years. */
    fun isPublished(index: Long): Boolean = index in 0 until publishedMonths

    /** JDN of the first day of month [index]. */
    fun start(index: Long): Long =
        when {
            index < firstAstronomicalIndex -> mean.value.startBeforeFirst(index - firstAstronomicalIndex)
            index > lastAstronomicalIndex -> mean.value.startAfterLast(index - lastAstronomicalIndex)
            else -> astronomicalStart(index)
        }

    /** Index of the month containing [jdn]; throws [IllegalArgumentException] outside the years of [Int]. */
    fun indexContaining(jdn: Long): Long {
        val first = index(Int.MIN_VALUE.toLong(), 1)
        val afterLast = index(Int.MAX_VALUE.toLong(), MONTHS) + 1
        require(jdn >= start(first) && jdn < start(afterLast)) {
            "JDN $jdn is outside the Umm al-Qura years ${Int.MIN_VALUE}..${Int.MAX_VALUE}"
        }
        var index = estimateIndex(jdn)
        while (start(index) > jdn) index--
        while (start(index + 1) <= jdn) index++
        return index
    }

    private fun estimateIndex(jdn: Long): Long =
        when {
            jdn < mean.value.firstStart -> firstAstronomicalIndex + mean.value.monthsFromFirst(jdn)
            jdn > mean.value.lastStart -> lastAstronomicalIndex + mean.value.monthsFromLast(jdn)
            else -> floor((jdn - PUBLISHED_START_JDN) / MEAN_SYNODIC_MONTH).toLong()
        }

    private fun astronomicalStart(index: Long): Long {
        val offset = index - firstAstronomicalIndex
        return blocks[(offset / BLOCK_MONTHS).toInt()].value[(offset % BLOCK_MONTHS).toInt()]
    }

    private fun block(number: Int): LongArray {
        val first = firstAstronomicalIndex + number.toLong() * BLOCK_MONTHS
        return LongArray(minOf(BLOCK_MONTHS.toLong(), lastAstronomicalIndex - first + 1).toInt()) {
            computeStart(first + it)
        }
    }

    /**
     * Published months use the published calendar. Every other month follows the criterion from the start of the month
     * before it; that start is the published one right after the published years, and otherwise found from its own
     * conjunction. The month just before the published years is kept at 29 or 30 days so the calendars join.
     */
    private fun computeStart(index: Long): Long =
        when {
            index in 0..publishedMonths -> {
                published[index.toInt()]
            }

            index == -1L -> {
                UmmAlQuraCriterion
                    .nextMonthStart(previousStart(index - 1))
                    .coerceIn(published[0] - LONG_MONTH, published[0] - SHORT_MONTH)
            }

            else -> {
                UmmAlQuraCriterion.nextMonthStart(previousStart(index - 1))
            }
        }

    private fun maskedLength(
        mask: Int,
        month: Int,
    ): Int = if (mask shr (MONTHS - month) and 1 == 1) LONG_MONTH else SHORT_MONTH

    private fun previousStart(index: Long): Long =
        if (index == publishedMonths) {
            published[index.toInt()]
        } else {
            UmmAlQuraCriterion.monthStartNear(PUBLISHED_START_JDN + index * MEAN_SYNODIC_MONTH)
        }
}

/**
 * Mean lunar months continued beyond the astronomical months: the interval from [firstStart] to [lastStart], [span]
 * months apart, is spread evenly in whole days, so every month has 29 or 30 days and the continuation meets both edges
 * exactly. Arithmetic stays in [Long] for every month of the years of [Int].
 */
internal class MeanLunation(
    val firstStart: Long,
    val lastStart: Long,
    private val span: Long,
) {
    private val interval = lastStart - firstStart
    private val meanMonth = interval.toDouble() / span

    /** JDN of the first day [months] months (negative) before the first astronomical month. */
    fun startBeforeFirst(months: Long): Long = firstStart + Math.floorDiv(months * interval, span)

    /** JDN of the first day [months] months after the last astronomical month. */
    fun startAfterLast(months: Long): Long = lastStart + Math.floorDiv(months * interval, span)

    /** Approximate months from the first astronomical month to [jdn]. */
    fun monthsFromFirst(jdn: Long): Long = floor((jdn - firstStart) / meanMonth).toLong()

    /** Approximate months from the last astronomical month to [jdn]. */
    fun monthsFromLast(jdn: Long): Long = floor((jdn - lastStart) / meanMonth).toLong()
}
