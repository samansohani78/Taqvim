/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import ir.taqvim.core.astronomy.ZodiacSign
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.DurationFormatter
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.Jdn
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** Localized numbers, times and dates for the Astronomy screen (digits and calendar from [AstronomySettings]). */
internal class AstronomyText(
    private val settings: AstronomySettings,
) {
    private val numerals = settings.language.numerals

    /** [value] with one decimal and a degree sign. */
    fun degrees(value: Double): String = decimal(value, 1) + DEGREE

    /** [value] rounded to [scale] decimals, in the language's digits. */
    fun decimal(
        value: Double,
        scale: Int,
    ): String = Numerals.format(BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP), numerals)

    /** A whole number, grouped, in the language's digits. */
    fun integer(value: Long): String = Numerals.format(value, numerals, grouped = true)

    /** A number without grouping, e.g. a house number. */
    fun plain(value: Int): String = Numerals.format(value.toLong(), numerals)

    /** Local `HH:mm` of [instant]. */
    fun time(instant: Instant): String = clock(instant.toLocalDateTime(settings.timeZone).time)

    /** Long date of [instant]'s local day in the settings' calendar. */
    fun date(instant: Instant): String = day(instant.toJdn(settings.timeZone))

    /** Long date of [day] in the settings' calendar. */
    fun day(day: Jdn): String =
        DateFormatter.format(settings.calendar.fromJdn(day), day.weekday(), settings.language, DateStyle.LONG)

    /** Long date and local time of [instant]. */
    fun dateTime(instant: Instant): String = "${date(instant)} ${time(instant)}"

    /** [duration] in words, or `H:MM` where the language has no duration patterns (docs/DATA_TODO.md DT-009). */
    fun duration(duration: Duration): String =
        DurationFormatter.format(duration, settings.language)
            ?: duration.inWholeMinutes.let { minutes ->
                val text = "${minutes / MINUTES_PER_HOUR}:" + (minutes % MINUTES_PER_HOUR).toString().padStart(2, '0')
                Numerals.localizeDigits(text, numerals)
            }

    /** An ecliptic longitude as its sign and the degrees and minutes within it. */
    fun signPosition(longitude: Double): SignPosition {
        val wrapped = longitude - FULL_TURN * floor(longitude / FULL_TURN)
        val inSign = wrapped - SIGN_DEGREES * floor(wrapped / SIGN_DEGREES)
        val whole = floor(inSign).toInt()
        val minutes = floor((inSign - whole) * MINUTES_PER_DEGREE).toInt()
        val text = "$whole$DEGREE" + minutes.toString().padStart(2, '0') + MINUTE_SIGN
        return SignPosition(ZodiacSign.ofEclipticLongitude(wrapped), Numerals.localizeDigits(text, numerals))
    }

    private fun clock(time: LocalTime): String =
        Numerals.localizeDigits(
            time.hour.toString().padStart(2, '0') + ":" + time.minute.toString().padStart(2, '0'),
            numerals,
        )

    companion object {
        private const val DEGREE = "°"
        private const val MINUTE_SIGN = "′"
        private const val MINUTES_PER_HOUR = 60
        private const val MINUTES_PER_DEGREE = 60
        private const val FULL_TURN = 360.0
        private const val SIGN_DEGREES = 30.0
    }
}

/** Local midnight at the start of [day] in the settings' zone. */
internal fun AstronomySettings.startOf(day: Jdn): Instant = day.toLocalDate().atTime(0, 0).toInstant(timeZone)

/** [day] at the local [time] in the settings' zone. */
internal fun AstronomySettings.at(
    day: Jdn,
    time: LocalTime,
): Instant = LocalDateTime(day.toLocalDate(), time).toInstant(timeZone)
