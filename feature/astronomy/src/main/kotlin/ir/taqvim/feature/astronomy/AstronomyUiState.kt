/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import androidx.compose.runtime.Immutable
import ir.taqvim.core.astronomy.ClassicalPlanet
import ir.taqvim.core.astronomy.EclipseKind
import ir.taqvim.core.astronomy.Season
import ir.taqvim.core.astronomy.ZodiacSign
import ir.taqvim.core.astronomy.ZodiacSystem
import ir.taqvim.core.ui.component.DateSelection
import kotlinx.collections.immutable.ImmutableList

/** The three views of the Astronomy screen. */
enum class AstronomyMode {
    EARTH,
    MOON,
    SUN,
}

/** The dialogs the screen can open. */
enum class AstronomyDialogKind {
    HOROSCOPE,
    YEAR_HOROSCOPE,
    PLANETARY_HOURS,
    MOON_IN_SCORPIO,
}

/** State of the Astronomy screen (T-1300). */
data class AstronomyUiState(
    val mode: AstronomyMode = AstronomyMode.EARTH,
    val content: AstronomyContent = AstronomyContent.Loading,
    /** The open dialog with its content, or `null`. */
    val dialog: AstronomyDialog? = null,
    /** Whether the date picker is shown. */
    val pickingDate: Boolean = false,
)

/** What the screen shows. */
sealed interface AstronomyContent {
    /** Preferences and place are loading. */
    data object Loading : AstronomyContent

    /** No place has been chosen yet. */
    data object NoLocation : AstronomyContent

    /** The sky at the selected instant. */
    data class Sky(
        val placeName: String,
        /** The selected instant: long date in the settings' calendar and local time. */
        val dateTitle: String,
        val timeText: String,
        /** Minute of the local day, 0‥1439, for the time slider. */
        val minuteOfDay: Int,
        /** Whether the selection follows the current time. */
        val isNow: Boolean,
        val header: HeaderText,
        val earth: EarthText,
        val moon: MoonText,
        val sun: SunText,
        val picker: PickerData,
    ) : AstronomyContent
}

/** Header values, localized. */
data class HeaderText(
    val sunSign: ZodiacSign,
    val moonSign: ZodiacSign,
    val moonConstellation: String,
    val phase: MoonPhaseName,
    val illumination: String,
    val moonDistance: String,
    val nextSeason: Season,
    val nextSeasonAt: String,
    val solarEclipse: EclipseText?,
    val lunarEclipse: EclipseText?,
)

/** An eclipse: its [kind] and the local date and time of its peak. */
data class EclipseText(
    val kind: EclipseKind,
    val peak: String,
)

/** The eight named phases of the Moon. */
enum class MoonPhaseName {
    NEW_MOON,
    WAXING_CRESCENT,
    FIRST_QUARTER,
    WAXING_GIBBOUS,
    FULL_MOON,
    WANING_GIBBOUS,
    THIRD_QUARTER,
    WANING_CRESCENT,
}

/** The Earth view: length of daylight, the Sun's declination (latitude of the subsolar point) and seasons. */
data class EarthText(
    /** Daylight from sunrise to sunset, `null` on polar days and nights. */
    val dayLength: String?,
    val subsolarLatitude: String,
    val nextSeason: Season,
    val nextSeasonAt: String,
)

/** The Moon view. */
data class MoonText(
    val illuminatedFraction: Float,
    val waxing: Boolean,
    val phase: MoonPhaseName,
    val illumination: String,
    val azimuth: String,
    val altitude: String,
    val rise: String?,
    val set: String?,
    val distance: String,
)

/** The Sun view. */
data class SunText(
    val azimuth: String,
    val altitude: String,
    val rise: String?,
    val transit: String?,
    val set: String?,
    /** Travelled part of the day's arc (0‥1), `null` while the Sun is down or never sets. */
    val progress: Float?,
)

/** What the date picker offers for the settings' calendar; [daysInMonth] is a bound calendar function. */
data class PickerData(
    val initial: DateSelection,
    val years: IntRange,
    val monthNames: ImmutableList<String>,
    val daysInMonth: (year: Int, month: Int) -> Int,
    val digits: (Int) -> String,
    val monthNamesIn: (year: Int) -> List<String> = { monthNames },
)

/** Content of an open dialog. */
sealed interface AstronomyDialog {
    /**
     * A chart at [at] (local date and time): for the selected instant, or for the March equinox that begins the year
     * when [yearly]. [chart] is `null` inside the polar circles, where Placidus houses are undefined.
     */
    data class Horoscope(
        val yearly: Boolean,
        val at: String,
        val chart: ChartText?,
    ) : AstronomyDialog

    /** The 24 planetary hours of the selected day, or empty when the Sun does not rise and set. */
    data class PlanetaryHours(
        val dayTitle: String,
        val hours: ImmutableList<PlanetaryHourText>,
    ) : AstronomyDialog

    /** Periods of the Moon in Scorpio in the selected calendar year. */
    data class MoonInScorpio(
        val yearTitle: String,
        val system: ZodiacSystem,
        val periods: ImmutableList<PeriodText>,
    ) : AstronomyDialog
}

/** A computed chart, localized; [cuspLongitudes] (degrees, cusp 1 first) drive the drawing. */
data class ChartText(
    val ascendant: SignPosition,
    val midheaven: SignPosition,
    val cusps: ImmutableList<CuspText>,
    val fortune: SignPosition,
    val spirit: SignPosition,
    val dayChart: Boolean,
    val cuspLongitudes: ImmutableList<Float>,
)

/** An ecliptic longitude as a [sign] and the localized [degrees] within it, e.g. "12°05′". */
data class SignPosition(
    val sign: ZodiacSign,
    val degrees: String,
)

/** One house cusp: its localized [house] number and position. */
data class CuspText(
    val house: String,
    val position: SignPosition,
)

/** One planetary hour. */
data class PlanetaryHourText(
    val number: String,
    val ruler: ClassicalPlanet,
    val span: String,
    val daytime: Boolean,
    val isCurrent: Boolean,
)

/** A period from [start] to [end], localized. */
data class PeriodText(
    val start: String,
    val end: String,
)

/** User actions of the Astronomy screen. */
@Immutable
data class AstronomyActions(
    val onMode: (AstronomyMode) -> Unit = {},
    val onStepDays: (Int) -> Unit = {},
    val onStepYears: (Int) -> Unit = {},
    val onMinuteOfDay: (Int) -> Unit = {},
    val onNow: () -> Unit = {},
    val onPickDate: () -> Unit = {},
    val onDatePicked: (DateSelection) -> Unit = {},
    val onDismissPicker: () -> Unit = {},
    val onOpenDialog: (AstronomyDialogKind) -> Unit = {},
    val onDismissDialog: () -> Unit = {},
)
