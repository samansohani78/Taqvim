/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import androidx.datastore.core.DataStore
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.model.AsrJuristic
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.data.location.City
import ir.taqvim.data.location.CityCatalog
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.data.preferences.proto.UserPrefs
import ir.taqvim.data.preferences.toProto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1100 wiring: the Times tab's settings from the stored preferences and the chosen city. */
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
    private val noZone = tehran.copy(id = 2, timeZoneId = null)
    private val persian = UserPreferences.defaultsFor("fa")

    @Test
    fun `a city with a time zone gives localized times settings`() {
        val settings = timesSettings(persian, tehran).shouldNotBeNull()

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
        val settings = timesSettings(custom, tehran).shouldNotBeNull()

        settings.language.code shouldBe UserPreferences.FALLBACK_LANGUAGE
        settings.placeName shouldBe "Tehran"
        settings.prayer shouldBe PrayerSettings(method = PrayerMethod.MWL, asr = AsrJuristic.HANAFI)
        settings.calendar shouldBeSameInstanceAs GregorianCalendarSystem

        timesSettings(custom.copy(calendars = listOf(CalendarSystem.NEPALI)), tehran)
            .shouldNotBeNull()
            .calendar shouldBeSameInstanceAs PersianCalendarSystem
    }

    @Test
    fun `cities without a usable time zone give no settings`() {
        timesSettings(persian, noZone).shouldBeNull()
        timesSettings(persian, tehran.copy(timeZoneId = "Not/AZone")).shouldBeNull()
    }

    @Test
    fun `the source emits null until a known city is chosen`(): Unit =
        runTest {
            val chosen = MutableStateFlow<Long?>(null)
            var catalogLoads = 0
            val source =
                PreferencesTimesSettingsSource(
                    preferences = UserPreferencesRepository(InMemoryPrefs(persian.toProto())),
                    chosenCity = { chosen },
                    loadCatalog = {
                        catalogLoads++
                        CityCatalog(listOf(tehran, noZone))
                    },
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                )

            source.settings().first().shouldBeNull()
            catalogLoads shouldBe 0

            chosen.value = tehran.id
            source.settings().first()?.placeName shouldBe "تهران"
            chosen.value = noZone.id
            source.settings().first().shouldBeNull()
            chosen.value = UNKNOWN_CITY
            source.settings().first().shouldBeNull()
            catalogLoads shouldBe 1
        }

    private class InMemoryPrefs(
        initial: UserPrefs,
    ) : DataStore<UserPrefs> {
        private val state = MutableStateFlow(initial)

        override val data: Flow<UserPrefs> = state

        override suspend fun updateData(transform: suspend (t: UserPrefs) -> UserPrefs): UserPrefs =
            transform(state.value).also { state.value = it }
    }

    private companion object {
        const val UNKNOWN_CITY = 999L
    }
}
