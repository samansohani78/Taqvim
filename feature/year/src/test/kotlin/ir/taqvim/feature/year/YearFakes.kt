/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import androidx.compose.runtime.Composable
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/** The civil day of a Gregorian date. */
internal fun gregorian(
    year: Int,
    month: Int,
    day: Int,
): Jdn = LocalDate(year, month, day).toJdn()

internal val PERSIAN_FIRST =
    YearSettings(
        calendars = listOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN, CalendarSystem.ISLAMIC),
        weekStart = Weekday.SATURDAY,
        islamicVariant = IslamicVariant.IRAN_OFFICIAL,
        languageCode = "fa",
    )

internal class FakeYearSettingsSource(
    initial: YearSettings,
) : YearSettingsSource {
    val state = MutableStateFlow(initial)

    override fun settings(): Flow<YearSettings> = state
}

internal class FakeYearTodaySource(
    initial: Jdn?,
) : YearTodaySource {
    val state = MutableStateFlow(initial)

    override fun today(): Flow<Jdn> = state.filterNotNull()
}

/** Days in [holidays] are holidays; no day is a weekend. Records every requested range. */
internal class FakeYearDaysSource : YearDaysSource {
    val holidays = MutableStateFlow(emptySet<Jdn>())

    /** Every range requested through [days], in order. */
    val requestedRanges = mutableListOf<JdnRange>()

    override fun days(range: JdnRange): Flow<List<YearDay>> {
        requestedRanges += range
        return holidays.map { holidays -> range.map { YearDay(it, it in holidays, isWeekend = false) } }
    }
}

/** The app theme with fixed (non-dynamic) colors for tests and screenshots. */
@Composable
internal fun YearTestTheme(
    rtl: Boolean = false,
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val settings = ThemeSettings(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false)
    TaqvimTheme(settings, if (rtl) TextDirection.RTL else TextDirection.LTR, content = content)
}
