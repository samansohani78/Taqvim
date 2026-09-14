/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import kotlinx.coroutines.flow.Flow

/** The preferences the month list and agenda react to (T-901). */
data class AgendaSettings(
    /** The user's calendars in order; the first available one groups the list into months. */
    val calendars: List<CalendarSystem>,
    val islamicVariant: IslamicVariant,
    /** App language code (e.g. `fa`): month names, dates and digits. */
    val languageCode: String,
)

/** Where an event of the list comes from. */
enum class AgendaEventKind {
    OFFICIAL,
    PERSONAL,
    DEVICE,
    SUBSCRIPTION,
}

/** One event on a day, already titled in the app language. */
data class AgendaEvent(
    /** Identifier within [kind]: the dataset id, or the stored or provider id as text. */
    val id: String,
    val kind: AgendaEventKind,
    val title: String,
    val isHoliday: Boolean,
)

/** The events of the civil day [jdn]. */
data class AgendaDay(
    val jdn: Jdn,
    val isHoliday: Boolean,
    val isWeekend: Boolean,
    /** Events in display order (holidays and official events first). */
    val events: List<AgendaEvent>,
)

/** The user's list preferences; re-emits on every change. Implemented in `:app` over the data layer. */
fun interface AgendaSettingsSource {
    fun settings(): Flow<AgendaSettings>
}

/** The events of a range of days in day order; re-emits when they change. Implemented in `:app` (T-305). */
fun interface AgendaDaySource {
    fun days(range: JdnRange): Flow<List<AgendaDay>>
}

/** The current civil day; emits again when the day changes. */
fun interface AgendaTodaySource {
    fun today(): Flow<Jdn>
}
