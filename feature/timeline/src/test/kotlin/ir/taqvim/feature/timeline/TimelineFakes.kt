/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import androidx.compose.runtime.Composable
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/** The civil day of a Gregorian date. */
internal fun gregorian(
    year: Int,
    month: Int,
    day: Int,
): Jdn = LocalDate(year, month, day).toJdn()

/** Friday, 21 Farvardin 1405. */
internal val TODAY: Jdn = gregorian(2026, 4, 10)

internal val PERSIAN_SETTINGS =
    TimelineSettings(
        calendars = listOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN, CalendarSystem.ISLAMIC),
        weekStart = Weekday.SATURDAY,
        islamicVariant = IslamicVariant.IRAN_OFFICIAL,
        languageCode = "fa",
    )

internal val ENGLISH_SETTINGS =
    TimelineSettings(
        calendars = listOf(CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN),
        weekStart = Weekday.MONDAY,
        islamicVariant = IslamicVariant.UMM_AL_QURA,
        languageCode = "en",
    )

/** Tehran; rounded coordinates used only as a sample input. */
internal val TEHRAN = TimelinePlace(Coordinates(35.69, 51.39), TimeZone.of("Asia/Tehran"), PrayerSettings())

internal fun timed(
    id: String,
    start: Int,
    end: Int,
    kind: TimelineEventKind = TimelineEventKind.PERSONAL,
    title: String = "Event $id",
): TimelineEvent =
    TimelineEvent(id, kind, title, isHoliday = false, isAllDay = false, startMinute = start, endMinute = end)

internal fun allDay(
    id: String,
    title: String,
    isHoliday: Boolean = false,
): TimelineEvent = TimelineEvent(id, TimelineEventKind.OFFICIAL, title, isHoliday, isAllDay = true)

internal class FakeTimelineSettingsSource(
    initial: TimelineSettings,
) : TimelineSettingsSource {
    val state = MutableStateFlow(initial)

    override fun settings(): Flow<TimelineSettings> = state
}

internal class FakeTimelineClockSource(
    initial: TimelineNow?,
) : TimelineClockSource {
    val state = MutableStateFlow(initial)

    override fun now(): Flow<TimelineNow> = state.filterNotNull()
}

internal class FakeTimelinePlaceSource(
    initial: TimelinePlace?,
) : TimelinePlaceSource {
    val state = MutableStateFlow(initial)

    override fun place(): Flow<TimelinePlace?> = state
}

/** Events per day from [events]; no day is a holiday or a weekend. Records every requested range. */
internal class FakeTimelineDaysSource : TimelineDaysSource {
    val events = MutableStateFlow(emptyMap<Jdn, List<TimelineEvent>>())

    /** Every range requested through [days], in order. */
    val requestedRanges = mutableListOf<JdnRange>()

    override fun days(range: JdnRange): Flow<List<TimelineDay>> {
        requestedRanges += range
        return events.map { byDay ->
            range.map { TimelineDay(it, isHoliday = false, isWeekend = false, events = byDay[it].orEmpty()) }
        }
    }
}

/** The app theme with fixed (non-dynamic) colors for tests and screenshots. */
@Composable
internal fun TimelineTestTheme(
    rtl: Boolean = false,
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val settings = ThemeSettings(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false)
    TaqvimTheme(settings, if (rtl) TextDirection.RTL else TextDirection.LTR, content = content)
}
