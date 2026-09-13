/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import ir.taqvim.core.astronomy.HouseCusps
import ir.taqvim.core.astronomy.Houses
import ir.taqvim.core.astronomy.PlanetaryHours
import ir.taqvim.core.astronomy.PlanetaryHoursResult
import ir.taqvim.core.astronomy.Sky
import ir.taqvim.core.astronomy.Zodiac
import ir.taqvim.core.calendar.toJdn
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Builds the content of the Astronomy dialogs (T-1300) at the selected [instant]. */
internal object AstronomyDialogs {
    fun build(
        kind: AstronomyDialogKind,
        settings: AstronomySettings,
        instant: Instant,
    ): AstronomyDialog =
        when (kind) {
            AstronomyDialogKind.HOROSCOPE -> horoscope(settings, instant, yearly = false)
            AstronomyDialogKind.YEAR_HOROSCOPE -> horoscope(settings, yearStart(instant), yearly = true)
            AstronomyDialogKind.PLANETARY_HOURS -> planetaryHours(settings, instant)
            AstronomyDialogKind.MOON_IN_SCORPIO -> moonInScorpio(settings, instant)
        }

    /** The March equinox that begins the solar year containing [instant]. */
    fun yearStart(instant: Instant): Instant {
        val year = instant.toLocalDateTime(TimeZone.UTC).year
        val equinox = Sky.seasons(year).marchEquinox
        return if (instant >= equinox) equinox else Sky.seasons(year - 1).marchEquinox
    }

    private fun horoscope(
        settings: AstronomySettings,
        at: Instant,
        yearly: Boolean,
    ): AstronomyDialog.Horoscope {
        val text = AstronomyText(settings)
        val chart = Houses.placidus(at, settings.place)?.let { chart(it, settings, at, text) }
        return AstronomyDialog.Horoscope(yearly, text.dateTime(at), chart)
    }

    private fun chart(
        cusps: HouseCusps,
        settings: AstronomySettings,
        at: Instant,
        text: AstronomyText,
    ): ChartText {
        val lots = Houses.lots(at, settings.place, cusps.ascendant)
        return ChartText(
            ascendant = text.signPosition(cusps.ascendant),
            midheaven = text.signPosition(cusps.midheaven),
            cusps =
                cusps.cusps
                    .mapIndexed { index, longitude -> CuspText(text.plain(index + 1), text.signPosition(longitude)) }
                    .toImmutableList(),
            fortune = text.signPosition(lots.fortune),
            spirit = text.signPosition(lots.spirit),
            dayChart = lots.dayChart,
            cuspLongitudes = cusps.cusps.map { it.toFloat() }.toImmutableList(),
        )
    }

    private fun planetaryHours(
        settings: AstronomySettings,
        instant: Instant,
    ): AstronomyDialog.PlanetaryHours {
        val text = AstronomyText(settings)
        val day = instant.toJdn(settings.timeZone)
        val hours =
            when (val result = PlanetaryHours.forDay(settings.place, settings.startOf(day), day.weekday())) {
                PlanetaryHoursResult.Unavailable -> {
                    persistentListOf()
                }

                is PlanetaryHoursResult.Available -> {
                    result.hours
                        .map { hour ->
                            PlanetaryHourText(
                                number = text.plain(hour.number),
                                ruler = hour.ruler,
                                span = "${text.time(hour.start)}–${text.time(hour.end)}",
                                daytime = hour.daytime,
                                isCurrent = instant >= hour.start && instant < hour.end,
                            )
                        }.toImmutableList()
                }
            }
        return AstronomyDialog.PlanetaryHours(text.day(day), hours)
    }

    private fun moonInScorpio(
        settings: AstronomySettings,
        instant: Instant,
    ): AstronomyDialog.MoonInScorpio {
        val text = AstronomyText(settings)
        val calendar = settings.calendar
        val year = calendar.fromJdn(instant.toJdn(settings.timeZone)).year
        val from = settings.startOf(calendar.toJdn(calendar.date(year, 1, 1)))
        val until = settings.startOf(calendar.toJdn(calendar.date(year + 1, 1, 1)))
        val periods =
            Zodiac
                .moonInScorpio(from, until, settings.scorpioSystem)
                .map { PeriodText(text.dateTime(it.start), text.dateTime(it.end)) }
                .toImmutableList()
        return AstronomyDialog.MoonInScorpio(text.plain(year), settings.scorpioSystem, periods)
    }
}
