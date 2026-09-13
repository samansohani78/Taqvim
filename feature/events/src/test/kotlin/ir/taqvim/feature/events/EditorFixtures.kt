/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.compose.runtime.Composable
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.ics.WeekdayNum
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.nlp.ParseContext
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import kotlin.random.Random
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

/** Sample settings, forms and events for the editor tests (synthetic data). */
object EditorFixtures {
    const val ZONE = "Asia/Tehran"

    /** 2026-09-14 12:00 in Tehran, which is 23 Shahrivar 1405 SH. */
    val NOW: Instant = Instant.parse("2026-09-14T08:30:00Z")
    val TODAY: Jdn = LocalDate(2026, 9, 14).toJdn()
    val CALENDARS = listOf(CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC, CalendarSystem.GREGORIAN)

    fun language(code: String): LanguageSpec = requireNotNull(LanguageTable.forCode(code)) { "no language $code" }

    fun settings(
        code: String = "en",
        calendars: List<CalendarSystem> = CALENDARS,
    ): EditorSettings = EditorSettings(language(code), calendars, ZONE)

    fun persian(
        year: Int,
        month: Int,
        day: Int,
    ): CalendarDate = PersianCalendarSystem.date(year, month, day)

    fun form(
        title: String = "Dentist",
        start: CalendarDate = persian(1405, 6, 23),
        end: CalendarDate = start,
    ): EditorForm =
        EditorForm(title = title, calendar = CalendarSystem.PERSIAN, start = start, end = end, timeZoneId = ZONE)

    fun event(
        id: Long? = 1,
        title: String = "Dentist",
        calendar: CalendarSystem = CalendarSystem.PERSIAN,
        start: CalendarDate = persian(1405, 6, 23),
    ): PersonalEvent =
        PersonalEvent(id = id, title = title, calendar = calendar, start = start, end = start, timeZoneId = ZONE)

    /** A random valid event (the start within ±3000 days of [TODAY]) from [random]. */
    fun randomEvent(random: Random): PersonalEvent {
        val system = CALENDARS.random(random)
        val calendar = ParseContext.DEFAULT_CALENDARS.getValue(system)
        val startJdn = TODAY + random.nextInt(-3_000, 3_000)
        val length = random.nextInt(0, 30)
        val startMinute = random.nextInt(0, 1_440)
        val endMinute = random.nextInt(if (length == 0) startMinute else 0, 1_440)
        val timed = random.nextBoolean()
        return PersonalEvent(
            id = random.nextLong(1, 1_000).takeIf { random.nextBoolean() },
            title = "Event ${random.nextInt(1_000)}",
            notes = if (random.nextBoolean()) "" else "Notes ${random.nextInt()}",
            calendar = system,
            start = calendar.fromJdn(startJdn),
            end = calendar.fromJdn(startJdn + length),
            startMinute = startMinute.takeIf { timed },
            endMinute = endMinute.takeIf { timed },
            timeZoneId = ZONE,
            colorArgb = EditorPresenter.COLORS.random(random).takeIf { random.nextBoolean() },
            recurrence = if (random.nextBoolean()) null else randomRule(random, startJdn),
            reminderMinutes = List(random.nextInt(0, 6)) { random.nextInt(0, 40_321) }.distinct().sorted(),
            sourceLink = "https://example.com/${random.nextInt(100)}".takeIf { random.nextBoolean() },
        )
    }

    private fun randomRule(
        random: Random,
        startJdn: Jdn,
    ): RecurrenceRule {
        val ending = RecurrenceEnd.entries.random(random)
        return RecurrenceRule(
            frequency = Frequency.entries.random(random),
            interval = random.nextInt(1, 1_000),
            count = random.nextInt(1, 10_000).takeIf { ending == RecurrenceEnd.COUNT },
            until = (startJdn + random.nextInt(0, 5_000)).takeIf { ending == RecurrenceEnd.UNTIL },
            byDay = Weekday.entries.filter { random.nextBoolean() }.map { WeekdayNum(it) },
            invalidDates = InvalidDatePolicy.entries.random(random),
        )
    }
}

/** An in-memory [PersonalEventStore] that can be made to fail. */
class FakeEventStore(
    vararg initial: PersonalEvent,
) : PersonalEventStore {
    private val stored = initial.associateBy { requireNotNull(it.id) }.toMutableMap()
    var failLoads = false
    var failSaves = false
    var failDeletes = false

    val events: Map<Long, PersonalEvent>
        get() = stored.toMap()

    override suspend fun load(id: Long): PersonalEvent? {
        check(!failLoads) { "load failed" }
        return stored[id]
    }

    override suspend fun save(event: PersonalEvent): Long {
        check(!failSaves) { "save failed" }
        val id = event.id ?: ((stored.keys.maxOrNull() ?: 0L) + 1)
        stored[id] = event.copy(id = id)
        return id
    }

    override suspend fun delete(id: Long) {
        check(!failDeletes) { "delete failed" }
        stored.remove(id)
    }
}

/** Settings that tests can change. */
class FakeSettingsSource(
    initial: EditorSettings = EditorFixtures.settings(),
) : EditorSettingsSource {
    val flow = MutableStateFlow(initial)

    override fun settings(): MutableStateFlow<EditorSettings> = flow
}

/** The app theme with fixed (non-dynamic) colors for tests and screenshots. */
@Composable
fun EditorTestTheme(
    rtl: Boolean = false,
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val settings = ThemeSettings(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false)
    TaqvimTheme(settings, if (rtl) TextDirection.RTL else TextDirection.LTR, content = content)
}
