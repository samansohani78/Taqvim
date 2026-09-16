/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.data.database.DeviceEventCacheEntity
import ir.taqvim.data.database.IcsEventCacheEntity
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.search.SearchEvent
import ir.taqvim.feature.search.SearchEventKind
import ir.taqvim.feature.search.SearchSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.jupiter.api.Test

/** T-804 wiring: the search reads every event source, and its settings follow the preferences. */
class SearchAdaptersTest {
    private val nowruz = OfficialEvents.ALL.first { it.id.value == NOWRUZ }
    private val today = LocalDate(2026, 3, 18).toJdn()
    private val tehran = TimeZone.of("Asia/Tehran")

    @Test
    fun `search settings follow the app language and the calendar order`(): Unit =
        runTest {
            val preferences = UserPreferences.defaultsFor("fa")

            PreferencesSearchSettingsSource(repositoryOf(preferences)).settings().first() shouldBe
                SearchSettings("fa", preferences.calendars)
        }

    @Test
    fun `official events carry their aliases and next occurrence`(): Unit =
        runTest {
            val title = nowruz.title.forLanguage("fa")

            val event = source(stores()).events(title, "fa", limit = 5).first { it.id == NOWRUZ }

            event.kind shouldBe SearchEventKind.OFFICIAL
            event.title shouldBe title
            event.aliases shouldContainAll nowruz.aliases
            event.nextDay shouldBe LocalDate(2026, 3, 21).toJdn()
            event.isHoliday shouldBe true
        }

    @Test
    fun `personal, device and subscription events match by title from today on`(): Unit =
        runTest {
            val windows = mutableListOf<Pair<Long, Long>>()
            val tomorrowMorning = (today + 1).startMillis(tehran) + HOUR
            val stores =
                SearchEventStores(
                    personal = {
                        listOf(
                            personal(1, "Dentist visit", today + 2),
                            personal(2, "Lunch", today),
                            personal(3, "Dentist", today - 5),
                        )
                    },
                    device = { from, to ->
                        windows += from to to
                        listOf(DeviceEventCacheEntity(7, 1, tomorrowMorning, tomorrowMorning + HOUR, false, "Dentist"))
                    },
                    subscriptions = { _, _ ->
                        val end = tomorrowMorning + HOUR
                        listOf(IcsEventCacheEntity(4, "u1", tomorrowMorning, end, false, "Dentist talk"))
                    },
                )

            val found =
                source(stores)
                    .events("Dentist", "en", limit = 5)
                    .filter { it.kind != SearchEventKind.OFFICIAL }

            found shouldContainExactly
                listOf(
                    SearchEvent("1", SearchEventKind.PERSONAL, "Dentist visit", nextDay = today + 2),
                    SearchEvent("3", SearchEventKind.PERSONAL, "Dentist", nextDay = null),
                    SearchEvent("7", SearchEventKind.DEVICE, "Dentist", nextDay = today + 1),
                    SearchEvent("4:u1", SearchEventKind.SUBSCRIPTION, "Dentist talk", nextDay = today + 1),
                )
            windows.single() shouldBe (today.startMillis(tehran) to (today + WINDOW).startMillis(tehran))
        }

    @Test
    fun `all-day external events keep their date in every zone and overlapping ones start today`(): Unit =
        runTest {
            val losAngeles = TimeZone.of("America/Los_Angeles")
            // All-day rows are UTC-midnight bounded, like the calendar reads them.
            val dayStart = (today + 1).toLocalDate().atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
            val dayEnd = dayStart + DAY
            val started = today.toLocalDate().atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() - 2 * DAY
            val feedDay = IcsEventCacheEntity(4, "u1", dayStart, dayEnd, true, "Dentist talk")
            val stores =
                SearchEventStores(
                    personal = { emptyList() },
                    device = { _, _ ->
                        listOf(
                            DeviceEventCacheEntity(7, 1, dayStart, dayEnd, true, "Dentist"),
                            DeviceEventCacheEntity(8, 1, started, dayEnd, true, "Dentist week"),
                        )
                    },
                    subscriptions = { _, _ -> listOf(feedDay) },
                )

            val inTehran = source(stores).external("Dentist")
            val inLosAngeles = source(stores, losAngeles).external("Dentist")

            inTehran shouldContainExactly
                listOf(
                    SearchEvent("7", SearchEventKind.DEVICE, "Dentist", nextDay = today + 1),
                    SearchEvent("8", SearchEventKind.DEVICE, "Dentist week", nextDay = today),
                    SearchEvent("4:u1", SearchEventKind.SUBSCRIPTION, "Dentist talk", nextDay = today + 1),
                )
            inLosAngeles shouldBe inTehran
        }

    @Test
    fun `each source gives at most the limit and a blank query finds nothing`(): Unit =
        runTest {
            val many = (1L..10L).map { personal(it, "Meeting $it", today) }
            val source = source(SearchEventStores({ many }, { _, _ -> emptyList() }, { _, _ -> emptyList() }))

            source.events("Meeting", "en", limit = 3).filter { it.kind == SearchEventKind.PERSONAL } shouldHaveSize 3
            source.events("  ", "en", limit = 3) shouldBe emptyList()
            source.events("Meeting", "en", limit = 0) shouldBe emptyList()
        }

    /** Personal, device and subscription results for [query]. */
    private suspend fun CompositeSearchEventSource.external(query: String): List<SearchEvent> =
        events(query, "en", limit = 5).filter { it.kind != SearchEventKind.OFFICIAL }

    private fun stores(): SearchEventStores =
        SearchEventStores({ emptyList() }, { _, _ -> emptyList() }, { _, _ -> emptyList() })

    private fun source(
        stores: SearchEventStores,
        zone: TimeZone = tehran,
    ): CompositeSearchEventSource =
        CompositeSearchEventSource(
            official = OfficialEventSearchSource(language = { "fa" }, today = { today }),
            stores = stores,
            today = { today },
            zone = { zone },
            window = WINDOW,
        )

    private companion object {
        const val NOWRUZ = "ir.holiday.nowruz-1"
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR
        const val WINDOW = 30

        fun personal(
            id: Long,
            title: String,
            start: Jdn,
        ): PersonalEventEntity =
            PersonalEventEntity(
                id = id,
                title = title,
                calendarSystem = CalendarSystem.PERSIAN,
                startJdn = start.value,
                endJdn = start.value,
                timeZoneId = "Asia/Tehran",
                createdAtEpochMillis = 0,
                updatedAtEpochMillis = 0,
            )
    }
}
