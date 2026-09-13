/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import androidx.annotation.StringRes
import ir.taqvim.core.astronomy.ClassicalPlanet
import ir.taqvim.core.astronomy.EclipseKind
import ir.taqvim.core.astronomy.Season
import ir.taqvim.core.astronomy.ZodiacSign

/** String resources naming the Astronomy screen's enumerations. */
internal object AstronomyLabels {
    @StringRes
    fun mode(mode: AstronomyMode): Int =
        when (mode) {
            AstronomyMode.EARTH -> R.string.astronomy_mode_earth
            AstronomyMode.MOON -> R.string.astronomy_mode_moon
            AstronomyMode.SUN -> R.string.astronomy_mode_sun
        }

    @StringRes
    fun dialog(kind: AstronomyDialogKind): Int =
        when (kind) {
            AstronomyDialogKind.HOROSCOPE -> R.string.astronomy_dialog_horoscope
            AstronomyDialogKind.YEAR_HOROSCOPE -> R.string.astronomy_dialog_year_horoscope
            AstronomyDialogKind.PLANETARY_HOURS -> R.string.astronomy_dialog_planetary_hours
            AstronomyDialogKind.MOON_IN_SCORPIO -> R.string.astronomy_dialog_moon_in_scorpio
        }

    @StringRes
    fun sign(sign: ZodiacSign): Int = SIGNS.getValue(sign)

    @StringRes
    fun phase(phase: MoonPhaseName): Int = PHASES.getValue(phase)

    @StringRes
    fun planet(planet: ClassicalPlanet): Int = PLANETS.getValue(planet)

    @StringRes
    fun season(season: Season): Int =
        when (season) {
            Season.MARCH_EQUINOX -> R.string.astronomy_season_march_equinox
            Season.JUNE_SOLSTICE -> R.string.astronomy_season_june_solstice
            Season.SEPTEMBER_EQUINOX -> R.string.astronomy_season_september_equinox
            Season.DECEMBER_SOLSTICE -> R.string.astronomy_season_december_solstice
        }

    @StringRes
    fun eclipse(kind: EclipseKind): Int =
        when (kind) {
            EclipseKind.PENUMBRAL -> R.string.astronomy_eclipse_penumbral
            EclipseKind.PARTIAL -> R.string.astronomy_eclipse_partial
            EclipseKind.ANNULAR -> R.string.astronomy_eclipse_annular
            EclipseKind.TOTAL -> R.string.astronomy_eclipse_total
        }

    private val SIGNS =
        mapOf(
            ZodiacSign.ARIES to R.string.astronomy_sign_aries,
            ZodiacSign.TAURUS to R.string.astronomy_sign_taurus,
            ZodiacSign.GEMINI to R.string.astronomy_sign_gemini,
            ZodiacSign.CANCER to R.string.astronomy_sign_cancer,
            ZodiacSign.LEO to R.string.astronomy_sign_leo,
            ZodiacSign.VIRGO to R.string.astronomy_sign_virgo,
            ZodiacSign.LIBRA to R.string.astronomy_sign_libra,
            ZodiacSign.SCORPIO to R.string.astronomy_sign_scorpio,
            ZodiacSign.SAGITTARIUS to R.string.astronomy_sign_sagittarius,
            ZodiacSign.CAPRICORN to R.string.astronomy_sign_capricorn,
            ZodiacSign.AQUARIUS to R.string.astronomy_sign_aquarius,
            ZodiacSign.PISCES to R.string.astronomy_sign_pisces,
        )

    private val PHASES =
        mapOf(
            MoonPhaseName.NEW_MOON to R.string.astronomy_phase_new_moon,
            MoonPhaseName.WAXING_CRESCENT to R.string.astronomy_phase_waxing_crescent,
            MoonPhaseName.FIRST_QUARTER to R.string.astronomy_phase_first_quarter,
            MoonPhaseName.WAXING_GIBBOUS to R.string.astronomy_phase_waxing_gibbous,
            MoonPhaseName.FULL_MOON to R.string.astronomy_phase_full_moon,
            MoonPhaseName.WANING_GIBBOUS to R.string.astronomy_phase_waning_gibbous,
            MoonPhaseName.THIRD_QUARTER to R.string.astronomy_phase_third_quarter,
            MoonPhaseName.WANING_CRESCENT to R.string.astronomy_phase_waning_crescent,
        )

    private val PLANETS =
        mapOf(
            ClassicalPlanet.SATURN to R.string.astronomy_planet_saturn,
            ClassicalPlanet.JUPITER to R.string.astronomy_planet_jupiter,
            ClassicalPlanet.MARS to R.string.astronomy_planet_mars,
            ClassicalPlanet.SUN to R.string.astronomy_planet_sun,
            ClassicalPlanet.VENUS to R.string.astronomy_planet_venus,
            ClassicalPlanet.MERCURY to R.string.astronomy_planet_mercury,
            ClassicalPlanet.MOON to R.string.astronomy_planet_moon,
        )
}
