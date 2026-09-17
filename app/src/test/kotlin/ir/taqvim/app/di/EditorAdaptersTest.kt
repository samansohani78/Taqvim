/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.calendar.CalendarCalendars
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1000 wiring: the editor's settings from the preferences, and event anchors over the official dataset. */
class EditorAdaptersTest {
    private val persian = UserPreferences.defaultsFor("fa")
    private val anchors = OfficialAnchorLookup()

    @Test
    fun `editor settings follow the language, calendars and Islamic variant`() {
        val settings = editorSettings(persian, "Asia/Tehran", anchors)

        settings.language.code shouldBe "fa"
        settings.calendars shouldBe persian.calendars
        settings.timeZoneId shouldBe "Asia/Tehran"
        settings.anchors shouldBeSameInstanceAs anchors
        settings.arithmeticOf(CalendarSystem.PERSIAN) shouldBeSameInstanceAs PersianCalendarSystem
        settings.arithmeticOf(CalendarSystem.GREGORIAN) shouldBeSameInstanceAs GregorianCalendarSystem
        settings.arithmetic.keys shouldBe CalendarSystem.entries.toSet()

        val ummAlQura = persian.copy(islamicVariant = IslamicVariant.UMM_AL_QURA)
        editorSettings(ummAlQura, "Asia/Riyadh", null).arithmeticOf(CalendarSystem.ISLAMIC).javaClass shouldBe
            CalendarCalendars.arithmeticFor(CalendarSystem.ISLAMIC, IslamicVariant.UMM_AL_QURA).javaClass
    }

    @Test
    fun `Bikram Sambat can be edited and an empty calendar list falls back to Persian`() {
        val nepali = persian.copy(languageCode = "xx", calendars = listOf(CalendarSystem.NEPALI))
        val settings = editorSettings(nepali, "Asia/Kathmandu", null)

        settings.calendars shouldBe listOf(CalendarSystem.NEPALI)
        editorSettings(persian.copy(calendars = emptyList()), "Asia/Tehran", null).calendars shouldBe
            listOf(CalendarSystem.PERSIAN)
        settings.language.code shouldBe UserPreferences.FALLBACK_LANGUAGE
        settings.anchors.shouldBeNull()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `the settings source follows the device zone`(): Unit =
        runTest {
            val zones = MutableStateFlow<TimeZone>(TimeZone.UTC)
            val source = PreferencesEditorSettingsSource(repositoryOf(persian), anchors, zones)

            val zoneIds =
                async {
                    source
                        .settings()
                        .take(2)
                        .toList()
                        .map { it.timeZoneId }
                }
            runCurrent()
            zones.value = TimeZone.of("Asia/Tokyo")
            zoneIds.await() shouldBe listOf("UTC", "Asia/Tokyo")
        }

    @Test
    fun `anchors find the official event's occurrence closest to the reference day`() {
        val nowruz1405 = LocalDate(2026, 3, 21).toJdn()
        val nowruz1406 = LocalDate(2027, 3, 21).toJdn()

        anchors.find(NOWRUZ_TITLE, nowruz1405 - 3) shouldBe nowruz1405
        anchors.find(NOWRUZ_TITLE, LocalDate(2027, 2, 1).toJdn()) shouldBe nowruz1406
        anchors.find("xqzw", nowruz1405).shouldBeNull()
    }

    private companion object {
        /** The dataset's Persian title of 1 Farvardin (dataset/iran/iran-official-holidays.json). */
        const val NOWRUZ_TITLE = "آغاز نوروز"
    }
}
