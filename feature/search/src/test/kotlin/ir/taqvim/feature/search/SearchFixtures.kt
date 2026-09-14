/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import androidx.compose.runtime.Composable
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

/** 20 Esfand 1404 (11 March 2026). */
internal val TODAY: Jdn = LocalDate(2026, 3, 11).toJdn()

/** 1 Farvardin 1405 (21 March 2026), a Saturday. */
internal val NOWRUZ_1405: Jdn = LocalDate(2026, 3, 21).toJdn()

internal val PERSIAN_FA =
    SearchSettings("fa", listOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN, CalendarSystem.ISLAMIC))

internal val GREGORIAN_EN = SearchSettings("en", listOf(CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN))

/** Synthetic events titled in the language of [settings]: two official, a personal and a device event. */
internal fun sampleEvents(settings: SearchSettings): List<SearchEvent> {
    val fa = settings.languageCode == "fa"
    return listOf(
        SearchEvent(
            "ir.nowruz",
            SearchEventKind.OFFICIAL,
            if (fa) "عید نوروز" else "Nowruz",
            aliases = listOf(if (fa) "Nowruz" else "عید نوروز"),
            nextDay = NOWRUZ_1405,
            isHoliday = true,
        ),
        SearchEvent(
            "ir.nature-day",
            SearchEventKind.OFFICIAL,
            if (fa) "روز طبیعت" else "Nature Day",
            nextDay = NOWRUZ_1405 + 12,
        ),
        SearchEvent(
            "7",
            SearchEventKind.PERSONAL,
            if (fa) "تولد نورا" else "Nora's birthday",
            nextDay = TODAY + 3,
        ),
        SearchEvent(
            "dev-4",
            SearchEventKind.DEVICE,
            if (fa) "جلسه تیم" else "Team meeting",
            nextDay = TODAY + 1,
        ),
    )
}

/** A few settings and tools titled in the language of [settings]. */
internal fun sampleEntries(settings: SearchSettings): List<SearchEntry> {
    val fa = settings.languageCode == "fa"

    fun entry(
        target: SearchTarget,
        faTitle: String,
        enTitle: String,
        faKeywords: List<String>,
        enKeywords: List<String>,
    ) = SearchEntry(target, if (fa) faTitle else enTitle, if (fa) faKeywords else enKeywords)
    val athan = SearchTarget.Settings(SettingsEntry.ATHAN)
    val calendars = SearchTarget.Settings(SettingsEntry.CALENDARS)
    return listOf(
        entry(athan, "اذان", "Athan", listOf("نماز"), listOf("prayer", "azan")),
        entry(calendars, "تقویم‌ها", "Calendars", listOf("شمسی"), listOf("Hijri")),
        entry(
            SearchTarget.Tool(ToolEntry.PRAYER_TIMES),
            "اوقات شرعی",
            "Prayer times",
            listOf("طلوع"),
            listOf("sunrise"),
        ),
        entry(SearchTarget.Tool(ToolEntry.ASTRONOMY), "نجوم", "Astronomy", listOf("ماه"), listOf("moon")),
        entry(SearchTarget.Tool(ToolEntry.CONVERTER), "تبدیل تاریخ", "Date converter", emptyList(), listOf("convert")),
    )
}

/** Ranked results for [query] over the sample data, with a "go to date" suggestion. */
internal fun sampleResults(
    settings: SearchSettings,
    query: String,
): SearchContent.Results {
    val dates = SearchDates(settings)
    val sections = SearchRanker.rank(query, sampleEvents(settings), sampleEntries(settings), dates::label)
    return SearchContent.Results(sections.toImmutableList(), DateSuggestion(NOWRUZ_1405, dates.label(NOWRUZ_1405)))
}

/** Every source of the screen at once, with [events] served (or failing) on request and the calls recorded. */
internal class FakeSearchSources(
    settings: SearchSettings,
    var events: List<SearchEvent> = sampleEvents(settings),
    var failing: Boolean = false,
    private val entries: List<SearchEntry> = sampleEntries(settings),
) : SearchSettingsSource,
    SearchTodaySource,
    SearchEventSource,
    SearchCatalogSource {
    val settingsFlow = MutableStateFlow(settings)
    val calls = mutableListOf<String>()

    override fun settings(): Flow<SearchSettings> = settingsFlow

    override fun today(): Flow<Jdn> = MutableStateFlow(TODAY)

    override suspend fun events(
        query: String,
        languageCode: String,
        limit: Int,
    ): List<SearchEvent> {
        calls += query
        check(!failing) { "event source offline" }
        return events
    }

    override fun entries(): List<SearchEntry> = entries
}

internal val SAMPLE_RECENT = persistentListOf("نوروز", "قبله", "۱ فروردین")

@Composable
internal fun SearchTestTheme(
    rtl: Boolean = false,
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val settings = ThemeSettings(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false)
    TaqvimTheme(settings, if (rtl) TextDirection.RTL else TextDirection.LTR, content = content)
}
