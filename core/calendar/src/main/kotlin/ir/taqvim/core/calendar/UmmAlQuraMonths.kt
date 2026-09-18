/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import kotlin.math.floor

/**
 * First days of every Umm al-Qura month (ADR-0028), addressed by a month index: 0 is Muharram of
 * [BUNDLED_FIRST_YEAR], and each month adds one.
 *
 * - [BUNDLED_FIRST_YEAR]‥[BUNDLED_LAST_YEAR]: the printed calendar ([UMM_AL_QURA_MONTH_MASKS]), the only years no rule
 *   reproduces.
 * - [MOONSET_RULE_FIRST_YEAR]‥[CONJUNCTION_RULE_FIRST_YEAR] − 1: the moonset-only rule of those years, chained month by
 *   month from the end of the bundled years.
 * - Other years of [FIRST_ASTRONOMICAL_YEAR]‥[LAST_ASTRONOMICAL_YEAR]: the criterion since AH 1423
 *   ([UmmAlQuraCriterion]), applied forward from each month's own start, computed on first use [BLOCK_MONTHS] at a
 *   time and kept. Before the bundled years the rule is applied proleptically.
 * - Every other year: mean lunar months continued from the nearer astronomical edge ([MeanLunarMonths]); months keep
 *   29 or 30 days and join the astronomical months without a seam.
 */
internal object UmmAlQuraMonths {
    const val BUNDLED_FIRST_YEAR: Int = 1300
    const val BUNDLED_LAST_YEAR: Int = 1419
    const val MOONSET_RULE_FIRST_YEAR: Int = 1420
    const val CONJUNCTION_RULE_FIRST_YEAR: Int = 1423
    const val FIRST_ASTRONOMICAL_YEAR: Int = -3000
    const val LAST_ASTRONOMICAL_YEAR: Int = 3000

    /** JDN of 1 Muharram [BUNDLED_FIRST_YEAR]. */
    const val BUNDLED_START_JDN: Long = 2_408_762L

    private const val MONTHS = 12
    private const val LONG_MONTH = 30
    private const val SHORT_MONTH = 29
    private const val BLOCK_MONTHS = 120
    private const val MEAN_SYNODIC_MONTH = 29.530588853

    /** Month starts of the bundled years, followed by 1 Muharram [MOONSET_RULE_FIRST_YEAR]. */
    private val bundled: LongArray =
        UMM_AL_QURA_MONTH_MASKS
            .flatMap { mask -> (1..MONTHS).map { month -> maskedLength(mask, month) } }
            .runningFold(BUNDLED_START_JDN) { start, length -> start + length }
            .toLongArray()

    private val bundledMonths: Long = (bundled.size - 1).toLong()
    private val conjunctionRuleIndex: Long = index(CONJUNCTION_RULE_FIRST_YEAR.toLong(), 1)

    /**
     * Month starts from 1 Muharram [MOONSET_RULE_FIRST_YEAR] to 1 Muharram [CONJUNCTION_RULE_FIRST_YEAR], each from the
     * month before it by the rule of the month it starts.
     */
    private val moonsetRuleStarts: Lazy<LongArray> =
        lazy {
            (bundledMonths + 1..conjunctionRuleIndex)
                .runningFold(bundled.last()) { start, index ->
                    val conjunctionRule = yearOf(index) >= CONJUNCTION_RULE_FIRST_YEAR
                    UmmAlQuraCriterion.nextMonthStart(start, requireConjunction = conjunctionRule)
                }.toLongArray()
        }
    val firstAstronomicalIndex: Long = index(FIRST_ASTRONOMICAL_YEAR.toLong(), 1)
    val lastAstronomicalIndex: Long = index(LAST_ASTRONOMICAL_YEAR.toLong(), MONTHS)

    private val blocks: List<Lazy<LongArray>> =
        List(((lastAstronomicalIndex - firstAstronomicalIndex) / BLOCK_MONTHS + 1).toInt()) { lazy { block(it) } }

    private val mean: Lazy<MeanLunarMonths> =
        lazy {
            MeanLunarMonths(
                astronomicalStart(firstAstronomicalIndex),
                astronomicalStart(lastAstronomicalIndex),
                lastAstronomicalIndex - firstAstronomicalIndex,
            )
        }

    /** Month index of [month] (1‥12) of [year]. */
    fun index(
        year: Long,
        month: Int,
    ): Long = (year - BUNDLED_FIRST_YEAR) * MONTHS + month - 1

    /** Year of month [index]. */
    fun yearOf(index: Long): Long = BUNDLED_FIRST_YEAR + Math.floorDiv(index, MONTHS.toLong())

    /** Month (1‥12) of month [index]. */
    fun monthOf(index: Long): Int = Math.floorMod(index, MONTHS.toLong()).toInt() + 1

    /** Whether month [index] lies in the bundled years. */
    fun isBundled(index: Long): Boolean = index in 0 until bundledMonths

    /** JDN of the first day of month [index]. */
    fun start(index: Long): Long =
        when {
            index < firstAstronomicalIndex -> mean.value.fromFirst(index - firstAstronomicalIndex)
            index > lastAstronomicalIndex -> mean.value.fromLast(index - lastAstronomicalIndex)
            else -> astronomicalStart(index)
        }

    /** Index of the month containing [jdn]; throws [IllegalArgumentException] outside the years of [Int]. */
    fun indexContaining(jdn: Long): Long {
        val first = index(Int.MIN_VALUE.toLong(), 1)
        val afterLast = index(Int.MAX_VALUE.toLong(), MONTHS) + 1
        requireInCalendarRange(jdn >= start(first) && jdn < start(afterLast)) {
            "JDN $jdn is outside the Umm al-Qura years ${Int.MIN_VALUE}..${Int.MAX_VALUE}"
        }
        var index = estimateIndex(jdn)
        while (start(index) > jdn) index--
        while (start(index + 1) <= jdn) index++
        return index
    }

    private fun estimateIndex(jdn: Long): Long =
        if (jdn < mean.value.firstStart || jdn > mean.value.lastStart) {
            mean.value.estimateIndex(jdn, lastAstronomicalIndex)
        } else {
            floor((jdn - BUNDLED_START_JDN) / MEAN_SYNODIC_MONTH).toLong()
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
     * Bundled months use the printed calendar and the moonset-rule years their chained starts. Every other month follows
     * the criterion from the start of the month before it; that start is the chained one right after the moonset-rule
     * years, and otherwise found from its own conjunction. The month just before the bundled years is kept at 29 or
     * 30 days so the calendars join.
     */
    private fun computeStart(index: Long): Long =
        when {
            index in 0..bundledMonths -> {
                bundled[index.toInt()]
            }

            index in bundledMonths..conjunctionRuleIndex -> {
                moonsetRuleStarts.value[(index - bundledMonths).toInt()]
            }

            index == -1L -> {
                UmmAlQuraCriterion
                    .nextMonthStart(previousStart(index - 1))
                    .coerceIn(bundled[0] - LONG_MONTH, bundled[0] - SHORT_MONTH)
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
        if (index == conjunctionRuleIndex) {
            moonsetRuleStarts.value.last()
        } else {
            UmmAlQuraCriterion.monthStartNear(BUNDLED_START_JDN + index * MEAN_SYNODIC_MONTH)
        }
}
