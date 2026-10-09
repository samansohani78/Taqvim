/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.IslamicCalendarSelection
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.data.events.OfficialEventView
import ir.taqvim.data.events.toEventsSettings
import ir.taqvim.data.preferences.UserPreferences
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/**
 * Every surface that shows dataset events dates them with the same calendars (T-302, ADR-0010, ADR-0037).
 *
 * The calendar screen assembled its days through [IslamicCalendarSelection]; search and the workday calculator each
 * built a bare `EventLookup`, whose default gives every source the computed Iranian calendar. So an Afghan official
 * event was dated by one rule on the calendar and another in search, and a user's official Iranian month overrides
 * reached only the calendar. These assertions are about the selection all three now share.
 */
class CrossSurfaceCalendarsTest {
    private val tehran = TimeZone.of("Asia/Tehran")

    private fun view(variant: IslamicVariant = IslamicVariant.IRAN_OFFICIAL): OfficialEventView {
        val preferences = UserPreferences.defaultsFor("fa").let { it.copy(islamicVariant = variant) }
        return OfficialEventView(preferences.toEventsSettings(homeTimeZone = tehran))
    }

    @Test
    fun `Afghan official events keep their own variant, not the user's and not the default`() {
        val selection = view().calendars

        val afghan = selection.variantFor(EventSource.AFGHANISTAN_OFFICIAL)
        val iran = selection.variantFor(EventSource.IRAN_OFFICIAL)

        afghan shouldBe IslamicVariant.TABULAR_16
        iran shouldBe IslamicVariant.IRAN_OFFICIAL
    }

    @Test
    fun `the Afghan calendar differs from the one a bare lookup would have used`() {
        val selection = view().calendars
        val afghan = selection.providerFor(EventSource.AFGHANISTAN_OFFICIAL).calendarFor(CalendarSystem.ISLAMIC)
        val bare = CalendarProvider.DEFAULT.calendarFor(CalendarSystem.ISLAMIC)

        checkNotNull(afghan)
        checkNotNull(bare)
        // Same question, different arithmetic: this is exactly what search and workday were getting wrong.
        (afghan === bare) shouldBe false
    }

    @Test
    fun `the user's chosen variant reaches every source that has no variant of its own`() {
        val ummAlQura = view(IslamicVariant.UMM_AL_QURA).calendars

        ummAlQura.variantFor(EventSource.INTERNATIONAL) shouldBe IslamicVariant.UMM_AL_QURA
        // A source with its own announced calendar is not overridden by the user's preference.
        ummAlQura.variantFor(EventSource.AFGHANISTAN_OFFICIAL) shouldBe IslamicVariant.TABULAR_16
    }

    @Test
    fun `a settings change makes a different view, so no surface keeps a stale one`() {
        val iran = view(IslamicVariant.IRAN_OFFICIAL)
        val ummAlQura = view(IslamicVariant.UMM_AL_QURA)

        (iran.settings == ummAlQura.settings) shouldBe false
        iran.calendars.preferredVariant shouldBe IslamicVariant.IRAN_OFFICIAL
        ummAlQura.calendars.preferredVariant shouldBe IslamicVariant.UMM_AL_QURA
    }

    @Test
    fun `switching every source off hides every dataset occurrence from the shared policy`() {
        val preferences =
            UserPreferences.defaultsFor("fa").let { base ->
                base.copy(app = base.app.copy(enabledEventSources = emptySet()))
            }
        val view = OfficialEventView(preferences.toEventsSettings(homeTimeZone = tehran))
        val year = 1405

        val visible =
            view.lookup
                .occurrencesIn(CalendarSystem.PERSIAN, year, emptySet())
                .filter { view.isVisible(it, tehran) }

        visible.shouldBeEmpty()
    }
}
