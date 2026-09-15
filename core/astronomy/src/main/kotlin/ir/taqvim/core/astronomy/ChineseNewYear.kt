/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.searchMoonPhase as librarySearchMoonPhase
import io.github.cosinekitty.astronomy.sunPosition as librarySunPosition
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn
import kotlin.math.floor
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** The start of a month of the Chinese calendar: its civil day in China, month [number] 1–12 and whether it is [leap]. */
internal data class ChineseMonthStart(
    val day: Jdn,
    val number: Int,
    val leap: Boolean,
)

/**
 * Chinese New Year (the first day of month 1) computed from the astronomical rules of the modern Chinese calendar
 * (GB/T 33661-2017; H. Aslaksen, "The Mathematics of the Chinese Calendar"), for any year the astronomy library covers:
 *
 * - days are civil days in China: UTC+8 (120°E) from 1929, Beijing local mean time (116°25′E, UTC+7:45:40) before;
 * - a month starts on the day of a new moon, and the month containing the December solstice is month 11;
 * - when 13 new moons separate two month-11 starts, the first month without a principal term (the Sun's apparent
 *   longitude reaching a multiple of 30°) is a leap month and repeats the previous month's number.
 */
public object ChineseNewYear {
    private const val EPOCH_JDN = 2_440_588L
    private const val SECONDS_PER_DAY = 86_400L
    private const val CHINA_OFFSET_SECONDS = 28_800L
    private const val BEIJING_MEAN_OFFSET_SECONDS = 27_940L
    private const val SEARCH_DAYS = 40.0
    private const val LOOKBACK_DAYS = 40
    private const val TERM_DEGREES = 30.0
    private const val MONTHS = 12
    private const val MONTH_ELEVEN = 11
    private const val LEAP_SPAN = 13
    private const val NO_LEAP_MONTH = -1

    /** First civil day of UTC+8 (1929-01-01); earlier days use Beijing mean time. */
    private val CHINA_STANDARD_TIME_FROM = Instant.parse("1928-12-31T16:00:00Z")
    private val CHINA_STANDARD_TIME_FIRST_DAY = Jdn(2_425_613L)

    /** Chinese New Year falling in Gregorian [gregorianYear]. */
    public fun date(gregorianYear: Int): CalendarDate = GregorianCalendarSystem.fromJdn(day(gregorianYear))

    /** Day number of Chinese New Year falling in Gregorian [gregorianYear]. */
    public fun day(gregorianYear: Int): Jdn = monthsOfSui(gregorianYear).first { it.number == 1 && !it.leap }.day

    /**
     * The month starts from month 11 before the December solstice of [gregorianYear] − 1 up to (not including) month 11
     * before the December solstice of [gregorianYear], numbered with the leap-month rule.
     */
    internal fun monthsOfSui(gregorianYear: Int): List<ChineseMonthStart> {
        val first = monthElevenStart(gregorianYear - 1)
        val next = monthElevenStart(gregorianYear)
        val starts = newMoonDays(first, next)
        val bounds = starts + next
        val leapIndex =
            if (starts.size == LEAP_SPAN) {
                starts.indices.first { !hasPrincipalTerm(bounds[it], bounds[it + 1]) }
            } else {
                NO_LEAP_MONTH
            }
        return starts.indices.fold(emptyList()) { months, index ->
            val leap = index == leapIndex
            val previous = months.lastOrNull()?.number
            val number =
                when {
                    previous == null -> MONTH_ELEVEN
                    leap -> previous
                    else -> previous % MONTHS + 1
                }
            months + ChineseMonthStart(starts[index], number, leap)
        }
    }

    /** Civil day in China of [instant]. */
    internal fun civilDay(instant: Instant): Jdn {
        val offset = if (instant < CHINA_STANDARD_TIME_FROM) BEIJING_MEAN_OFFSET_SECONDS else CHINA_OFFSET_SECONDS
        return Jdn(Math.floorDiv(instant.epochSeconds + offset, SECONDS_PER_DAY) + EPOCH_JDN)
    }

    /** First instant of civil [day] in China. */
    internal fun startOfDay(day: Jdn): Instant {
        val offset = if (day < CHINA_STANDARD_TIME_FIRST_DAY) BEIJING_MEAN_OFFSET_SECONDS else CHINA_OFFSET_SECONDS
        return Instant.fromEpochSeconds((day.value - EPOCH_JDN) * SECONDS_PER_DAY - offset)
    }

    /** Whether the Sun reaches a multiple of 30° apparent longitude during the days [from] until [until] (exclusive). */
    internal fun hasPrincipalTerm(
        from: Jdn,
        until: Jdn,
    ): Boolean = termIndex(startOfDay(from)) != termIndex(startOfDay(until))

    private fun termIndex(instant: Instant): Int =
        floor(librarySunPosition(instant.toAstronomyTime()).elon / TERM_DEGREES).toInt() % MONTHS

    /** Day of the last new moon on or before the day of the December solstice of [gregorianYear]. */
    private fun monthElevenStart(gregorianYear: Int): Jdn {
        val solstice = Sky.seasons(gregorianYear).decemberSolstice
        val solsticeDay = civilDay(solstice)
        return newMoons(solstice - LOOKBACK_DAYS.days)
            .map(::civilDay)
            .takeWhile { it <= solsticeDay }
            .last()
    }

    /** Days of the new moons from day [from] (inclusive) until day [until] (exclusive). */
    private fun newMoonDays(
        from: Jdn,
        until: Jdn,
    ): List<Jdn> =
        newMoons(startOfDay(from))
            .map(::civilDay)
            .dropWhile { it < from }
            .takeWhile { it < until }
            .toList()

    /** New moons from [from] onward. */
    private fun newMoons(from: Instant): Sequence<Instant> =
        generateSequence(searchNewMoon(from.toAstronomyTime())) { searchNewMoon(it.addDays(1.0)) }
            .map(Time::toInstant)

    private fun searchNewMoon(from: Time): Time? = librarySearchMoonPhase(0.0, from, SEARCH_DAYS)
}
