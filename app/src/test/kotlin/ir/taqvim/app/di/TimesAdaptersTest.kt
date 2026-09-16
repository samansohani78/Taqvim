/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ir.taqvim.core.calendar.NepaliCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.model.AsrJuristic
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.data.location.City
import ir.taqvim.data.location.CityCatalog
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1100 wiring: the Times tab's settings from the stored preferences and the place chosen in T-1502. */
@OptIn(ExperimentalCoroutinesApi::class)
class TimesAdaptersTest {
    private val tehran =
        City(
            id = 1,
            englishName = "Tehran",
            localizedNames = mapOf("fa" to "تهران"),
            countryCode = "IR",
            region = "Tehran",
            coordinates = Coordinates(35.69, 51.42),
            timeZoneId = "Asia/Tehran",
            population = null,
        )
    private val cityPlace = ChosenPlace(PlaceSource.CITY, tehran.id, "Tehran", tehran.coordinates, "Asia/Tehran")
    private val devicePlace = ChosenPlace(PlaceSource.DEVICE, null, "خانه", Coordinates(36.3, 59.6), "Asia/Tehran")
    private val persian = UserPreferences.defaultsFor("fa")

    @Test
    fun `a chosen place gives settings with its coordinates, zone and the prayer conventions`() {
        val settings = timesSettings(persian, cityPlace, "تهران").shouldNotBeNull()

        settings.placeName shouldBe "تهران"
        settings.place shouldBe tehran.coordinates
        settings.timeZone shouldBe TimeZone.of("Asia/Tehran")
        settings.prayer shouldBe PrayerSettings(method = persian.prayerMethod, asr = persian.asrJuristic)
        settings.language.code shouldBe "fa"
        settings.calendar shouldBeSameInstanceAs PersianCalendarSystem
    }

    @Test
    fun `prayer conventions, primary calendar and unknown languages follow the preferences`() {
        val custom =
            persian.copy(
                languageCode = "xx",
                calendars = listOf(CalendarSystem.NEPALI, CalendarSystem.GREGORIAN),
                prayerMethod = PrayerMethod.MWL,
                asrJuristic = AsrJuristic.HANAFI,
            )
        val settings = timesSettings(custom, devicePlace, "Home").shouldNotBeNull()

        settings.language.code shouldBe UserPreferences.FALLBACK_LANGUAGE
        settings.prayer shouldBe PrayerSettings(method = PrayerMethod.MWL, asr = AsrJuristic.HANAFI)
        settings.calendar shouldBeSameInstanceAs NepaliCalendarSystem
        timesSettings(custom.copy(calendars = emptyList()), devicePlace, "Home")
            .shouldNotBeNull()
            .calendar shouldBeSameInstanceAs PersianCalendarSystem
    }

    @Test
    fun `places without a name are labelled with their coordinates`() {
        coordinatesLabel(Coordinates(35.6892, -51.389)) shouldBe "35.69, -51.39"
    }

    @Test
    fun `the source follows the stored place and names catalog cities in the app language`(): Unit =
        runTest {
            var catalogLoads = 0
            val preferences = repositoryOf(persian)
            val source =
                PreferencesTimesSettingsSource(
                    preferences = preferences,
                    catalog =
                        CityCatalogProvider(
                            load = {
                                catalogLoads++
                                CityCatalog(listOf(tehran))
                            },
                            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                        ),
                )

            source.settings().first().shouldBeNull()

            preferences.update { it.copy(place = devicePlace) }
            source.settings().first()?.placeName shouldBe "خانه"
            preferences.update { it.copy(place = devicePlace.copy(name = null)) }
            source.settings().first()?.place shouldBe devicePlace.coordinates
            catalogLoads shouldBe 0

            preferences.update { it.copy(place = cityPlace) }
            source.settings().first()?.placeName shouldBe "تهران"
            preferences.update { it.copy(place = cityPlace.copy(cityId = UNKNOWN_CITY)) }
            source.settings().first()?.placeName shouldBe "Tehran"
            catalogLoads shouldBe 1
        }

    private companion object {
        const val UNKNOWN_CITY = 999L
    }
}
