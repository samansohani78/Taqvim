/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.IranIslamicCalendar
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.nlp.AnchorLookup
import ir.taqvim.core.workdays.WorkdayCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone

/** What the tools need from the preferences (T-600, T-504); bound in `:app`. */
data class ToolsSettings(
    /** App language: dates, digits, durations and time-zone names. */
    val language: LanguageSpec,
    /** Civil time zone of the user: "today" of the converter and the reference row of the time-zone board. */
    val homeZone: TimeZone,
    /** The user's calendars, primary first; every date is shown in each of them. */
    val calendars: List<CalendarArithmetic> = DEFAULT_CALENDARS,
    /** IANA ids shown on the time-zone board besides [homeZone]. */
    val boardZones: List<String> = emptyList(),
    /** Workday arithmetic with the user's profile (F-07), or `null` when none is configured. */
    val workdays: WorkdayCalculator? = null,
    /** Finds named events for phrases such as "3 days before Nowruz" (T-500); `null` reads none of them. */
    val anchors: AnchorLookup? = null,
) {
    init {
        require(calendars.isNotEmpty()) { "at least one calendar is needed" }
    }

    companion object {
        /** Persian, Iran's official Islamic and Gregorian calendars. */
        val DEFAULT_CALENDARS: List<CalendarArithmetic> =
            listOf(PersianCalendarSystem, IranIslamicCalendar(), GregorianCalendarSystem)
    }
}

/** The current [ToolsSettings]; re-emits on every change. */
fun interface ToolsSettingsSource {
    fun settings(): Flow<ToolsSettings>
}

/** Keeps the time-zone board the user edits (IANA ids, in order); bound in `:app` over the preferences (T-1500). */
fun interface ToolsBoardStore {
    suspend fun setBoardZones(zones: List<String>)

    companion object {
        /** Keeps nothing: board edits last for the session. */
        val NONE: ToolsBoardStore = ToolsBoardStore { }
    }
}
