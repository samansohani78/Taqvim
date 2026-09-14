/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

import androidx.compose.runtime.Composable
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate

/** The civil day of a Gregorian date. */
internal fun gregorian(
    year: Int,
    month: Int,
    day: Int,
): Jdn = LocalDate(year, month, day).toJdn()

/** 22 Shahrivar 1405 (13 September 2026), a Sunday. */
internal val TODAY: Jdn = gregorian(2026, 9, 13)

internal val PERSIAN_FA =
    AgendaSettings(
        calendars = listOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN, CalendarSystem.ISLAMIC),
        islamicVariant = IslamicVariant.IRAN_OFFICIAL,
        languageCode = "fa",
    )

internal val GREGORIAN_EN =
    AgendaSettings(
        calendars = listOf(CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN),
        islamicVariant = IslamicVariant.UMM_AL_QURA,
        languageCode = "en",
    )

internal fun event(
    id: String,
    title: String,
    kind: AgendaEventKind = AgendaEventKind.OFFICIAL,
    holiday: Boolean = false,
): AgendaEvent = AgendaEvent(id, kind, title, holiday)

/** Synthetic events around [TODAY]: an official holiday, a personal, a device and a subscription event. */
internal val SAMPLE_EVENTS: Map<Jdn, List<AgendaEvent>> =
    mapOf(
        TODAY - 10 to listOf(event("dev-1", "Dentist", AgendaEventKind.DEVICE)),
        TODAY + 2 to
            listOf(
                event("off-1", "Holiday event", holiday = true),
                event("per-1", "Birthday", AgendaEventKind.PERSONAL),
            ),
        TODAY + 5 to listOf(event("per-2", "Meeting", AgendaEventKind.PERSONAL)),
        TODAY + 40 to listOf(event("sub-1", "Match", AgendaEventKind.SUBSCRIPTION)),
    )

/** Settings, today and events of a test, all changeable. */
internal class FakeAgendaSources(
    settings: AgendaSettings = PERSIAN_FA,
    today: Jdn = TODAY,
    events: Map<Jdn, List<AgendaEvent>> = SAMPLE_EVENTS,
) : AgendaSettingsSource,
    AgendaTodaySource,
    AgendaDaySource {
    val settingsState = MutableStateFlow(settings)
    val todayState = MutableStateFlow(today)
    val eventsState = MutableStateFlow(events)
    val holidays = MutableStateFlow(events.filterValues { list -> list.any { it.isHoliday } }.keys)

    /** Every range requested through [days], in order. */
    val requested = mutableListOf<JdnRange>()

    override fun settings(): Flow<AgendaSettings> = settingsState

    override fun today(): Flow<Jdn> = todayState

    override fun days(range: JdnRange): Flow<List<AgendaDay>> {
        requested += range
        return combine(eventsState, holidays) { events, holidays ->
            range.map { AgendaDay(it, it in holidays, isWeekend = false, events[it].orEmpty()) }
        }
    }
}

/** A loaded list as the view model builds it, for UI tests and screenshots. */
internal fun sampleContent(
    settings: AgendaSettings = GREGORIAN_EN,
    mode: AgendaMode = AgendaMode.AGENDA,
    window: AgendaWindow = AgendaWindow.INITIAL,
    events: Map<Jdn, List<AgendaEvent>> = SAMPLE_EVENTS,
    isLoading: Boolean = false,
): AgendaContent {
    val calendars = AgendaCalendars(settings)
    val language = AgendaListBuilder.languageFor(settings.languageCode)
    val days =
        calendars.range(TODAY, window.first, window.last).map { day ->
            val list = events[day].orEmpty()
            AgendaDay(day, list.any { it.isHoliday }, isWeekend = false, list)
        }
    val list = AgendaListBuilder(calendars, language).build(TODAY, window, days, mode)
    return AgendaContent(
        today = TODAY,
        mode = mode,
        items = list.items,
        todayIndex = list.todayIndex,
        canLoadEarlier = window.canLoadEarlier,
        canLoadLater = window.canLoadLater,
        isLoading = isLoading,
        scrollToTodayRequest = 0,
        localeTag = language.localeTag,
        isRightToLeft = language.direction == TextDirection.RTL,
    )
}

/** The app theme with fixed (non-dynamic) colors for tests and screenshots. */
@Composable
internal fun AgendaTestTheme(
    rtl: Boolean = false,
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val settings = ThemeSettings(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false)
    TaqvimTheme(settings, if (rtl) TextDirection.RTL else TextDirection.LTR, content = content)
}
