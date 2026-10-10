/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.events.Citation
import ir.taqvim.core.events.EventCategory
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventId
import ir.taqvim.core.events.EventRule
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.LocalizedText
import ir.taqvim.core.events.Validity
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.data.events.toEventsSettings
import ir.taqvim.data.preferences.UserPreferences
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/**
 * The final limit applies after every visibility decision, not before it (L2).
 *
 * Definition-level filters — source, category, holidays-only — were already applied inside the index, before its
 * `take(limit)`. Occurrence-level ones were not and could not be: whether the user can see an event's next day
 * depends on the validity range it falls in and on the Islamic calendar its own source is announced in, which the
 * index knows nothing about. So the adapter cut to [limit] first and rejected afterwards, and a search for one
 * result whose best match was an observance that does not exist yet came back empty while a visible second-best
 * sat right behind it.
 */
class OfficialEventSearchLimitTest {
    private val preferences = UserPreferences.defaultsFor("fa")
    private val tehran = TimeZone.of("Asia/Tehran")

    /** Nowruz 1405: the search dates results from here, and both candidates fall in the Persian year 1405. */
    private val from = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, YEAR, 1, 1))

    private val citation = Citation("https://example.invalid/law", "A law")

    /**
     * The exact match, and therefore the first hit — but its day only becomes an observance in 1500, so under the
     * user's rules it has no visible occurrence at all.
     */
    private val hiddenExactMatch =
        definition(
            id = "hidden-exact",
            title = "جشن",
            month = 2,
            day = 10,
            validity = Validity(CalendarSystem.PERSIAN, fromYear = 1500, toYear = null, citation = citation),
        )

    /** A prefix match, so it ranks behind the exact one, and it is visible every year. */
    private val visiblePrefixMatch = definition(id = "visible-prefix", title = "جشن مهرگان", month = 7, day = 10)

    private fun definition(
        id: String,
        title: String,
        month: Int,
        day: Int,
        validity: Validity? = null,
    ): EventDefinition =
        EventDefinition(
            id = EventId(id),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = false,
            title = LocalizedText(mapOf("fa" to title)),
            rule = EventRule.Fixed(month, day),
            validity = validity,
        )

    private fun source(definitions: List<EventDefinition>) =
        OfficialEventSearchSource(
            language = { "fa" },
            today = { from },
            settings = { preferences.toEventsSettings(homeTimeZone = tehran) },
            zone = { tehran },
            definitions = definitions,
        )

    private companion object {
        const val YEAR = 1405
    }

    @Test
    fun `asking for one result returns the visible second-best, not nothing`(): Unit =
        runTest {
            val definitions = listOf(hiddenExactMatch, visiblePrefixMatch)

            val results = source(definitions).search("جشن", limit = 1, from = from)

            results shouldHaveSize 1
            results.single().eventId shouldBe "visible-prefix"
            results.single().nextDay.shouldNotBeNull()
        }

    @Test
    fun `the hidden candidate really is the better match, so the ordering is what is being tested`(): Unit =
        runTest {
            // Without the validity range the exact match wins, which is what makes the test above meaningful:
            // the fix is not that the hidden event ranks low, it is that ranking low is not how it gets dropped.
            val alwaysValid = hiddenExactMatch.copy(validity = null)

            val results = source(listOf(alwaysValid, visiblePrefixMatch)).search("جشن", limit = 1, from = from)

            results.single().eventId shouldBe "hidden-exact"
        }

    @Test
    fun `a limit still limits`(): Unit =
        runTest {
            val many = (1..5).map { definition(id = "e$it", title = "جشن $it", month = 3, day = it) }

            source(many).search("جشن", limit = 2, from = from) shouldHaveSize 2
            source(many).search("جشن", limit = 5, from = from) shouldHaveSize 5
        }

    @Test
    fun `an event with no occurrence in reach is still a result, undated`(): Unit =
        runTest {
            // 30 Esfand exists only in leap years; the point here is that "no occurrence" and "every occurrence
            // hidden" stay different answers after the reordering.
            val undated = definition(id = "undated", title = "جشن نادر", month = 12, day = 30)

            val results = source(listOf(undated)).search("جشن نادر", limit = 1, from = from)

            results shouldHaveSize 1
            results.single().eventId shouldBe "undated"
        }
}
