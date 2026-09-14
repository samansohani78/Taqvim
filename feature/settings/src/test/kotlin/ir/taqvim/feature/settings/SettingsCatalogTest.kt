/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.model.AsrJuristic
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.praytimes.HighLatitudeRule
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/** T-1500: every settings item has a control that reads back what it writes; labels cover every offered value. */
class SettingsCatalogTest {
    private val before = GeneralSettingsFixtures.settingsFor(LocationFixtures.persian)

    @TestFactory
    fun `every item reads back what it writes`(): List<DynamicTest> =
        SettingsItemId.entries.map { id ->
            DynamicTest.dynamicTest(id.name) {
                when (val control = SettingsCatalog.control(id)) {
                    is SettingsControl.Toggle -> {
                        control.read(control.write(before, !control.read(before))) shouldBe !control.read(before)
                    }

                    is SettingsControl.Choice -> {
                        control.options.forEach { option ->
                            val written = control.write(before, option.key)
                            if (id != SettingsItemId.SECONDARY_CALENDAR ||
                                option.key != before.calendars.first().name
                            ) {
                                control.read(written) shouldBe option.key
                            }
                        }
                    }

                    is SettingsControl.MultiChoice -> {
                        control.options.forEach { option ->
                            control.read(control.write(before, setOf(option.key))) shouldBe setOf(option.key)
                        }
                    }

                    is SettingsControl.Link -> {
                        control.destination.name shouldNotBe ""
                    }

                    SettingsControl.ClearRecentSearches -> {
                        id shouldBe SettingsItemId.CLEAR_SEARCHES
                    }
                }
            }
        }

    @Test
    fun `tabs hold the plan's groups and links open distinct pages`() {
        SettingsTab.entries.forEach { tab -> SettingsItemId.entries.any { it.tab == tab } shouldBe true }
        SettingsItemId.entries
            .map { SettingsCatalog.control(it) }
            .filterIsInstance<SettingsControl.Link>()
            .map { it.destination } shouldContainExactlyInAnyOrder SettingsDestination.entries
    }

    @Test
    fun `labels cover every value that can be chosen`() {
        SettingsLabels.themes.keys shouldBe ThemeChoice.entries.toSet()
        SettingsLabels.numerals.keys shouldBe NumeralSystem.entries.toSet()
        SettingsLabels.calendars.keys.toList() shouldBe SettingsCatalog.CALENDARS
        SettingsLabels.weekdays.keys shouldBe Weekday.entries.toSet()
        SettingsLabels.islamicVariants.keys shouldBe IslamicVariant.entries.toSet()
        SettingsLabels.eventSources.keys shouldBe (EventSource.entries - EventSource.USER).toSet()
        SettingsLabels.prayerMethods.keys shouldBe PrayerMethod.entries.toSet()
        SettingsLabels.asrJuristics.keys shouldBe AsrJuristic.entries.toSet()
        SettingsLabels.highLatitudeRules.keys shouldBe HighLatitudeRule.entries.toSet()
    }

    @Test
    fun `main and secondary calendars reorder the calendar list`() {
        val main = SettingsCatalog.control(SettingsItemId.MAIN_CALENDAR) as SettingsControl.Choice
        val secondary = SettingsCatalog.control(SettingsItemId.SECONDARY_CALENDAR) as SettingsControl.Choice
        val three = before.copy(calendars = SettingsCatalog.CALENDARS)

        main.write(three, CalendarSystem.GREGORIAN.name).calendars shouldBe
            listOf(CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC)
        secondary.write(three, CalendarSystem.GREGORIAN.name).calendars shouldBe
            listOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN, CalendarSystem.ISLAMIC)
        secondary.write(three, SettingsCatalog.NO_CALENDAR).calendars shouldBe listOf(CalendarSystem.PERSIAN)
        secondary.read(before.copy(calendars = listOf(CalendarSystem.PERSIAN))) shouldBe SettingsCatalog.NO_CALENDAR
        secondary.write(three, CalendarSystem.PERSIAN.name) shouldBe three
    }

    @Test
    fun `settings search matches word starts across Persian spelling variants`() {
        SettingsSearch.matches("dark", "Theme", "dark mode|colors") shouldBe true
        SettingsSearch.matches("mod da", "Theme", "dark mode|colors") shouldBe true
        SettingsSearch.matches("ark", "Theme", "dark mode|colors") shouldBe false
        SettingsSearch.matches("  ", "Theme", "dark mode") shouldBe false
        SettingsSearch.matches("تقويم", "تقویم اصلی", "") shouldBe true
        SettingsSearch.matches("اذان", "Athan", "اذان|صدا") shouldBe true
    }
}
