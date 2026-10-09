/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.EventCategory
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.data.events.OfficialEventView
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.events.toEventsSettings
import ir.taqvim.data.preferences.UserPreferences
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/**
 * Search answers under the same rules as the calendar (T-302, T-304).
 *
 * Until 2026-10-10 it did not: `OfficialEventSearchSource` searched the whole dataset with no
 * [ir.taqvim.core.events.EventVisibilityPolicy] at all, so switching religious occasions off removed them from the
 * calendar and left them in search, and a disabled source still answered. It also built a bare `EventLookup` with
 * the default calendars, so Afghan official events were dated in the computed Iranian calendar instead of the
 * tabular one they are announced in, and a user's official Iranian month overrides were ignored.
 */
class SearchVisibilityTest {
    private val tehran = TimeZone.of("Asia/Tehran")
    private val today = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1))

    /** Every category and source a user can actually choose; personal is neither selectable nor filterable. */
    private val allCategories = EventCategory.entries.toSet() - EventCategory.PERSONAL
    private val allSources = EventSource.entries.toSet() - EventSource.USER

    private fun preferences(
        categories: Set<EventCategory> = allCategories,
        sources: Set<EventSource> = allSources,
        holidaysOnly: Boolean = false,
    ): UserPreferences =
        UserPreferences.defaultsFor("fa").let { base ->
            base.copy(
                app =
                    base.app.copy(
                        enabledEventCategories = categories,
                        enabledEventSources = sources,
                        holidaysOnly = holidaysOnly,
                    ),
            )
        }

    private fun source(preferences: UserPreferences) =
        OfficialEventSearchSource(
            language = { "fa" },
            today = { today },
            settings = { preferences.toEventsSettings(homeTimeZone = tehran) },
            zone = { tehran },
        )

    /** Religious observances the dataset can answer for a broad query. */
    private suspend fun religiousHits(preferences: UserPreferences): Int {
        val ids =
            OfficialEvents.ALL
                .filter { it.category == EventCategory.RELIGIOUS }
                .map { it.id.value }
                .toSet()
        return source(preferences).search("ا", limit = 500, from = today).count { it.eventId in ids }
    }

    @Test
    fun `religious observances are searchable while the category is on`(): Unit =
        runTest {
            religiousHits(preferences()) shouldBeGreaterThan 0
        }

    @Test
    fun `switching the religious category off removes them from search too`(): Unit =
        runTest {
            religiousHits(preferences(categories = allCategories - EventCategory.RELIGIOUS)) shouldBe 0
        }

    @Test
    fun `a disabled source answers nothing`(): Unit =
        runTest {
            val iranIds =
                OfficialEvents.ALL
                    .filter { it.source == EventSource.IRAN_OFFICIAL }
                    .map { it.id.value }
                    .toSet()
            val withoutIran = allSources - EventSource.IRAN_OFFICIAL

            val hits = source(preferences(sources = withoutIran)).search("ا", limit = 500, from = today)

            hits.filter { it.eventId in iranIds }.shouldBeEmpty()
        }

    @Test
    fun `holidays-only search answers only days off`(): Unit =
        runTest {
            val hits = source(preferences(holidaysOnly = true)).search("ا", limit = 500, from = today)

            hits.all { it.isHoliday } shouldBe true
            hits.size shouldBeGreaterThan 0
        }

    @Test
    fun `switching every category off leaves no dataset result, rather than all of them`(): Unit =
        runTest {
            val hits = source(preferences(categories = emptySet())).search("ا", limit = 500, from = today)

            hits.shouldBeEmpty()
        }

    @Test
    fun `search filters before it limits, so a small limit is filled with visible results`(): Unit =
        runTest {
            val withoutReligious = preferences(categories = allCategories - EventCategory.RELIGIOUS)
            val religiousIds =
                OfficialEvents.ALL
                    .filter { it.category == EventCategory.RELIGIOUS }
                    .map { it.id.value }
                    .toSet()

            val hits = source(withoutReligious).search("ا", limit = 5, from = today)

            hits.size shouldBe 5
            hits.none { it.eventId in religiousIds } shouldBe true
        }

    @Test
    fun `search dates an event in the same calendar the calendar screen would`(): Unit =
        runTest {
            val preferences = preferences()
            val settings = preferences.toEventsSettings(homeTimeZone = tehran)
            val view = OfficialEventView(settings)
            val afghan =
                OfficialEvents.ALL.first {
                    it.source == EventSource.AFGHANISTAN_OFFICIAL && it.calendar == CalendarSystem.ISLAMIC
                }

            // The variant the calendar path uses for this source, not CalendarProvider.DEFAULT.
            val expected = view.calendars.providerFor(afghan.source).calendarFor(CalendarSystem.ISLAMIC)
            val fallback =
                ir.taqvim.core.events.CalendarProvider.DEFAULT
                    .calendarFor(CalendarSystem.ISLAMIC)

            expected.shouldNotBeNullAndDifferFrom(fallback)
        }

    private fun Any?.shouldNotBeNullAndDifferFrom(other: Any?) {
        checkNotNull(this)
        (this === other) shouldBe false
    }
}
