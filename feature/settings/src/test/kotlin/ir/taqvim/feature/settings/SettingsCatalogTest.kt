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
import kotlinx.collections.immutable.persistentListOf
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
                        choiceReadsBack(id, control)
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

                    is SettingsControl.TimeOfDay -> {
                        listOf(0, control.stepMinutes, 1_439).forEach { minute ->
                            control.read(control.write(before, minute)) shouldBe minute
                        }
                    }
                }
            }
        }

    /** Each option of [control] reads back once written (choosing the main calendar as secondary changes nothing). */
    private fun choiceReadsBack(
        id: SettingsItemId,
        control: SettingsControl.Choice,
    ) {
        control.options.forEach { option ->
            val written = control.write(before, option.key)
            if (id != SettingsItemId.SECONDARY_CALENDAR || option.key != before.calendars.first().name) {
                control.read(written) shouldBe option.key
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
    fun `the all-day reminder time offers every half hour in the settings' digits and keeps an odd stored time`() {
        val id = SettingsItemId.ALL_DAY_REMINDER_TIME
        val persianDigits = before.copy(allDayReminderMinute = 545)
        val dialog = requireNotNull(SettingsStateMapper.dialog(id, persianDigits))

        dialog.options.size shouldBe 49
        dialog.options.first().key shouldBe "0"
        dialog.selected shouldBe setOf("545")
        dialog.options.first { it.key == "545" }.text shouldBe SettingsStateMapper.timeOption(545, persianDigits).text
        SettingsStateMapper.timeOption(540, before.copy(numerals = NumeralSystem.LATIN)).text shouldBe "09:00"
        SettingsStateMapper.timeOption(1_439, before.copy(numerals = NumeralSystem.PERSIAN)).text shouldBe "۲۳:۵۹"
        SettingsStateMapper.rows(before).first { it.id == id }.value shouldBe
            RowValue.Chosen(persistentListOf(SettingsStateMapper.timeOption(540, before)))
    }

    @Test
    fun `main and secondary calendars reorder the calendar list`() {
        val main = SettingsCatalog.control(SettingsItemId.MAIN_CALENDAR) as SettingsControl.Choice
        val secondary = SettingsCatalog.control(SettingsItemId.SECONDARY_CALENDAR) as SettingsControl.Choice
        val all = before.copy(calendars = SettingsCatalog.CALENDARS)
        val nepali = CalendarSystem.NEPALI
        val hebrew = CalendarSystem.HEBREW

        main.write(all, CalendarSystem.GREGORIAN.name).calendars shouldBe
            listOf(CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC, nepali, hebrew)
        secondary.write(all, CalendarSystem.GREGORIAN.name).calendars shouldBe
            listOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN, CalendarSystem.ISLAMIC, nepali, hebrew)
        main.write(all, nepali.name).calendars.first() shouldBe nepali
        secondary.write(all, SettingsCatalog.NO_CALENDAR).calendars shouldBe listOf(CalendarSystem.PERSIAN)
        secondary.read(before.copy(calendars = listOf(CalendarSystem.PERSIAN))) shouldBe SettingsCatalog.NO_CALENDAR
        secondary.write(all, CalendarSystem.PERSIAN.name) shouldBe all
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
