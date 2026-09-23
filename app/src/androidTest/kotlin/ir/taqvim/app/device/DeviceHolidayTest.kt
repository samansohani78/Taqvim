/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.device

import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.ui.component.segmentTag
import ir.taqvim.data.events.EventsRepository
import ir.taqvim.feature.calendar.DayDetailsTab
import ir.taqvim.feature.calendar.R as CalendarR
import kotlin.time.Clock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import org.junit.Test
import org.koin.core.context.GlobalContext

/**
 * J09 (docs/device-journeys.md): a holiday-bearing day is marked in the month grid, and the title(s) shown on its
 * Events tab match [EventsRepository] — the exact source the UI reads from — instead of only asserting that the
 * day's screen opens (R11). Nowruz (Persian New Year, `PERSIAN` year/1/1) is used because every dataset year Taqvim
 * supports carries it; the expected titles are read from the dataset at run time, never hardcoded here, per the
 * project's "compute every value, tables only as goldens" rule.
 *
 * Three things had to be fixed before this passed on a device (CI run 35854629904, API 33 and 36; reproduced and
 * verified locally on `d1api33`):
 * - A first version picked `dayEvents.official.firstOrNull { it.isHoliday }` as "the" occurrence and asserted only
 *   its title. Nowruz 1405 day 1 actually carries **five** official occurrences that day (Iran's multi-day Nowruz
 *   holiday, the ancient-Iran and international Nowruz entries, and, that year, Eid al-Fitr 1447 coinciding with it —
 *   confirmed on device: the day cell's description read "۵ رویداد", five events), and `EventLookup.DAY_ORDER`
 *   (holiday, then source, then id string) does not put Nowruz's own entry first. Fixed by asserting every title the
 *   dataset actually returns for the day, computed here rather than assumed to be exactly one.
 * - Asserting the correct title(s) still failed: `AppDestination.Day` opens the same adaptive calendar screen as
 *   `AppDestination.Calendar` (`AppScreens.kt`'s `EntryScreen`), whose day-details pane defaults to the `CALENDARS`
 *   tab (`DayDetailsTab.CALENDARS`, ordinal 0) — event titles live on the `EVENTS` tab (`DayDetailsPanel.kt`'s
 *   `DayEventsTab`), which nothing had selected. Fixed by selecting `segmentTag(DayDetailsTab.EVENTS.ordinal)` first.
 *   The holiday-marking check does not need this: the month pane (`CalendarAdaptive.kt`'s `CalendarPanes`, `STACKED`
 *   on a phone width) is shown above the details pane regardless of which of its tabs is selected.
 * - Selecting the tab still did not make `By.textContains(title)` find anything, confirmed with a window-hierarchy
 *   dump on device: `EventChip` (`core/ui/component/EventChip.kt`) renders each event's row with
 *   `.clearAndSetSemantics { contentDescription = model.contentDescription }`, which — like `DayCellModel` in the
 *   month grid — replaces the row's accessibility text entirely with one `contentDescription` (title, then source,
 *   then "holiday"; see `DayEventsTab.kt`'s `chipDescription`). Nothing is missing from the app; `By.textContains`
 *   simply cannot see a chip built this way. Fixed by matching `By.descContains(title)` instead, exactly as the
 *   holiday-marking check already did for the month grid.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU) // useLanguage needs LocaleManager (ADR-0023)
class DeviceHolidayTest {
    @Test
    fun nowruzIsMarkedAndItsTitleMatchesTheDataset() {
        useLanguage("fa")
        val events = GlobalContext.get().get<EventsRepository>()
        val today = Clock.System.now().toJdn(TimeZone.currentSystemDefault())
        val year = PersianCalendarSystem.fromJdn(today).year
        val nowruz = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year, 1, 1))

        val dayEvents = runBlocking { events.day(nowruz).first() }
        check(dayEvents.isHoliday) { "Nowruz $year is not marked as a holiday by the dataset" }
        check(dayEvents.official.isNotEmpty()) { "Nowruz $year has no official occurrence in the dataset" }
        val titles = dayEvents.official.map { it.definition.title.forLanguage("fa") }
        val holidayWord = appContext.getString(CalendarR.string.calendar_holiday)

        openLink("taqvim://day/$year-1-1", "destination:Day")
        awaitTag(segmentTag(DayDetailsTab.EVENTS.ordinal)).click()
        device.waitForIdle()
        // The day carries more occurrences than fit on one screen since main@0855d52 added the UN observances of
        // 21 March, so each title is looked for while scrolling rather than on the first screenful.
        titles.forEach { title -> awaitDescription(title) }
        awaitDescription(holidayWord)
    }
}
