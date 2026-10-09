/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.calendar.TodayProvider
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.events.EventSearchIndex
import ir.taqvim.core.events.Occurrence
import ir.taqvim.core.events.SearchQuery
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.events.DayEvents
import ir.taqvim.data.events.EventsRepository
import ir.taqvim.data.events.EventsSettings
import ir.taqvim.data.events.OfficialEventView
import ir.taqvim.data.events.SkyAstronomicalEventSource
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.calendar.CalendarDay
import ir.taqvim.feature.calendar.CalendarDaySource
import ir.taqvim.feature.calendar.CalendarDisplayStore
import ir.taqvim.feature.calendar.CalendarMonthSource
import ir.taqvim.feature.calendar.CalendarPlace
import ir.taqvim.feature.calendar.CalendarPlaceSource
import ir.taqvim.feature.calendar.CalendarSettings
import ir.taqvim.feature.calendar.CalendarSettingsSource
import ir.taqvim.feature.calendar.DayEventItem
import ir.taqvim.feature.calendar.DayEventKind
import ir.taqvim.feature.calendar.EventSearchResult
import ir.taqvim.feature.calendar.EventSearchSource
import ir.taqvim.feature.times.TimesSettingsSource
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone

/** The calendar screen's preferences (T-800) from the stored user preferences (T-600). */
internal class PreferencesCalendarSettingsSource(
    private val preferences: UserPreferencesRepository,
) : CalendarSettingsSource {
    override fun settings(): Flow<CalendarSettings> =
        preferences.preferences
            .map {
                CalendarSettings(
                    it.calendars,
                    it.weekStart,
                    it.islamicVariant,
                    it.languageCode,
                    it.app.showWeekNumbers,
                    it.islamicOverride.table,
                )
            }.distinctUntilChanged()
}

/** The calendar menu's display choices (T-803) stored in the user preferences (T-600). */
internal class PreferencesCalendarDisplayStore(
    private val preferences: UserPreferencesRepository,
) : CalendarDisplayStore {
    override suspend fun setShowWeekNumbers(show: Boolean) {
        preferences.update { current -> current.copy(app = current.app.copy(showWeekNumbers = show)) }
    }

    override suspend fun setSecondaryCalendar(system: CalendarSystem) {
        preferences.update { current -> current.copy(calendars = withSecondary(current.calendars, system)) }
    }
}

/** [calendars] with [system] second: the primary calendar stays first, the others keep their order. */
internal fun withSecondary(
    calendars: List<CalendarSystem>,
    system: CalendarSystem,
): List<CalendarSystem> {
    val others = calendars - system
    return others.take(1) + system + others.drop(1)
}

/** Day events for the calendar screen and its month pager from the events repository (T-305), titled in [language]. */
internal class RepositoryCalendarDaySource(
    private val events: EventsRepository,
    private val language: Flow<String>,
) : CalendarDaySource,
    CalendarMonthSource {
    override fun day(jdn: Jdn): Flow<CalendarDay> =
        combine(events.day(jdn), language.distinctUntilChanged()) { day, language -> day.toCalendarDay(language) }

    override fun days(range: JdnRange): Flow<List<CalendarDay>> =
        combine(events.days(range), language.distinctUntilChanged()) { days, language ->
            days.map { it.toCalendarDay(language) }
        }
}

/** These events for the calendar screen: dataset events (holidays first), then personal, device and feed events. */
internal fun DayEvents.toCalendarDay(language: String): CalendarDay =
    CalendarDay(
        jdn = jdn,
        isHoliday = isHoliday,
        isWeekend = isWeekend,
        events =
            official.map { occurrence ->
                val definition = occurrence.definition
                DayEventItem(
                    definition.id.value,
                    DayEventKind.OFFICIAL,
                    definition.title.forLanguage(language),
                    occurrence.isHoliday,
                    source = definition.source,
                    citations = definition.citations,
                    dateOrigin = officialOrigins[definition.id],
                    scope = definition.scope?.label?.forLanguage(language),
                )
            } +
                personal.map { item(it.itemId, DayEventKind.PERSONAL, it.title) } +
                device.map { item(it.eventId.toString(), DayEventKind.DEVICE, it.title) } +
                ics.map { item("${it.subscriptionId}:${it.uid}", DayEventKind.SUBSCRIPTION, it.summary) },
    )

/** The calendar screen's place (T-802) from the Times tab's settings (T-1100): the same chosen city and method. */
internal class TimesCalendarPlaceSource(
    private val times: TimesSettingsSource,
) : CalendarPlaceSource {
    override fun place(): Flow<CalendarPlace?> =
        times
            .settings()
            .map { settings -> settings?.let { CalendarPlace(it.placeName, it.place, it.timeZone, it.prayer) } }
            .distinctUntilChanged()
}

/** A non-holiday event: only dataset events can make a day a holiday. */
private fun item(
    id: String,
    kind: DayEventKind,
    title: String,
): DayEventItem = DayEventItem(id, kind, title, isHoliday = false)

/**
 * Search over the official dataset (T-304) for the calendar screen, titled in the current [language], with the next
 * occurrence from [today] in the event's own calendar (this or the next year).
 */
internal class OfficialEventSearchSource(
    private val language: suspend () -> String,
    private val today: TodayProvider,
    /** The same settings the calendar assembles its days from; search must answer under the same rules. */
    private val settings: suspend () -> EventsSettings,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val definitions: List<EventDefinition> = OfficialEvents.ALL,
) : EventSearchSource {
    private val index by lazy { EventSearchIndex(definitions) }
    private val shared = AtomicReference<OfficialEventView?>(null)

    override suspend fun search(
        text: String,
        limit: Int,
    ): List<EventSearchResult> = search(text, limit, today.today())

    /** Results for [text] with their next occurrence on or after [from], under the user's visibility rules. */
    suspend fun search(
        text: String,
        limit: Int,
        from: Jdn,
    ): List<EventSearchResult> {
        val settings = settings()
        val preferences = settings.preferences
        // An empty set is an explicit "none" for the visibility policy, while SearchQuery reads an empty filter as
        // "no filter"; without this, switching every source or category off would show everything.
        if (preferences.enabledSources.isEmpty() || preferences.enabledCategories.isEmpty()) return emptyList()
        val view = viewFor(settings)
        val language = language()
        val here = zone()
        val query =
            SearchQuery(
                text = text,
                sources = preferences.enabledSources,
                categories = preferences.enabledCategories,
                holidaysOnly = preferences.holidaysOnly,
                limit = limit,
            )
        return index.search(query).mapNotNull { hit ->
            val definition = hit.definition
            val occurrences = occurrences(view, definition, from)
            // A definition every one of whose occurrences is hidden is not a result; one with no occurrence at all
            // in reach stays, undated, as it always has.
            val next = occurrences.firstOrNull { view.isVisible(it, here) }
            if (occurrences.isNotEmpty() && next == null) return@mapNotNull null
            EventSearchResult(
                eventId = definition.id.value,
                title = definition.title.forLanguage(language),
                isHoliday = definition.isHoliday,
                nextDay = next?.jdn,
            )
        }
    }

    /** The view for [settings], shared between queries rather than rebuilt per keystroke. */
    private fun viewFor(settings: EventsSettings): OfficialEventView {
        shared.get()?.takeIf { it.settings == settings }?.let { return it }
        return OfficialEventView(settings, definitions).also { shared.set(it) }
    }

    /** Occurrences of [definition] on or after [from], this year and next, in the user's own calendars. */
    private fun occurrences(
        view: OfficialEventView,
        definition: EventDefinition,
        from: Jdn,
    ): List<Occurrence> {
        val calendar =
            view.calendars.providerFor(definition.source).calendarFor(definition.calendar)
                ?: return emptyList()
        val year = calendar.fromJdn(from).year
        return (year..year + 1)
            .asSequence()
            .flatMap { view.lookup.occurrencesIn(definition.calendar, it, setOf(definition.source)) }
            .filter { it.definition.id == definition.id && it.jdn >= from }
            .toList()
    }
}
