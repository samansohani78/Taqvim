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
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.settings.LocationSettings
import ir.taqvim.feature.settings.LocationSettingsStore
import ir.taqvim.feature.settings.PlaceChoice
import ir.taqvim.feature.settings.PlaceDescription
import ir.taqvim.feature.settings.PlaceKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1301 wiring: the map's settings from the preferences and saving a picked point as the chosen place. */
class MapAdaptersTest {
    private val tehran = ChosenPlace(PlaceSource.CITY, 1, "Tehran", Coordinates(35.69, 51.42), "Asia/Tehran")
    private val persian = UserPreferences.defaultsFor("fa")
    private val berlin = TimeZone.of("Europe/Berlin")

    @Test
    fun `map settings follow the language, the stored place, its zone and the primary calendar`() {
        val settings = mapSettings(persian.copy(place = tehran), berlin)

        settings.language.code shouldBe "fa"
        settings.place shouldBe tehran.coordinates
        settings.timeZone shouldBe TimeZone.of("Asia/Tehran")
        settings.calendar shouldBeSameInstanceAs PersianCalendarSystem
    }

    @Test
    fun `without a place the device zone is used and calendars follow the preferences`() {
        val settings = mapSettings(persian.copy(calendars = listOf(CalendarSystem.GREGORIAN)), berlin)

        settings.place.shouldBeNull()
        settings.timeZone shouldBe berlin
        settings.calendar shouldBeSameInstanceAs GregorianCalendarSystem
        mapSettings(persian.copy(calendars = listOf(CalendarSystem.NEPALI)), berlin)
            .calendar shouldBeSameInstanceAs PersianCalendarSystem
    }

    @Test
    fun `the source follows the stored preferences`(): Unit =
        runTest {
            val preferences = repositoryOf(persian)
            val source = PreferencesMapSettingsSource(preferences) { berlin }

            source
                .settings()
                .first()
                .place
                .shouldBeNull()
            preferences.update { it.copy(place = tehran) }
            source.settings().first().place shouldBe tehran.coordinates
        }

    @Test
    fun `a picked point is stored as a coordinates place with its described name and zone`(): Unit =
        runTest {
            val stored = mutableListOf<PlaceChoice>()
            val store =
                object : LocationSettingsStore {
                    override fun settings(): Flow<LocationSettings> = emptyFlow()

                    override suspend fun choose(place: PlaceChoice) {
                        stored += place
                    }
                }
            val paris = Coordinates(48.86, 2.35)

            MapPlacePicker({ PlaceDescription("Paris", "Europe/Paris") }, store).useAsPlace(paris) shouldBe true
            stored shouldBe listOf(PlaceChoice(PlaceKind.COORDINATES, null, "Paris", paris, "Europe/Paris"))

            MapPlacePicker({ error("no geocoder") }, store).useAsPlace(paris) shouldBe false
            stored.size shouldBe 1
        }
}
