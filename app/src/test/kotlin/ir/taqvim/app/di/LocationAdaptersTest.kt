/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import ir.taqvim.data.location.City
import ir.taqvim.data.location.CityCatalog
import ir.taqvim.data.location.GeocodeResult
import ir.taqvim.data.location.GeocodedPlace
import ir.taqvim.data.location.LocationFix
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.settings.CityOption
import ir.taqvim.feature.settings.DeviceFix
import ir.taqvim.feature.settings.PlaceChoice
import ir.taqvim.feature.settings.PlaceDescription
import ir.taqvim.feature.settings.PlaceKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-1502 wiring: the stored place, city search, device fixes and place descriptions. */
class LocationAdaptersTest {
    private val tehran =
        City(
            id = 1,
            englishName = "Tehran",
            localizedNames = mapOf("fa" to "تهران"),
            countryCode = "IR",
            region = "Tehran",
            coordinates = Coordinates(35.69, 51.42),
            timeZoneId = "Asia/Tehran",
            population = 9_000_000,
        )
    private val noZone =
        City(2, "Nowhere", emptyMap(), null, null, Coordinates(-60.0, -120.0), timeZoneId = null, population = null)

    private fun catalogOf(vararg cities: City) =
        CityCatalogProvider(load = { CityCatalog(cities.toList()) }, ioDispatcher = Dispatchers.Unconfined)

    @Test
    fun `the store shows the stored place and stores a chosen one`(): Unit =
        runTest {
            val preferences = repositoryOf(UserPreferences.defaultsFor("fa"))
            val store = PreferencesLocationSettingsStore(preferences)
            val city = PlaceChoice(PlaceKind.CITY, 1, "تهران", tehran.coordinates, "Asia/Tehran")
            val typed = PlaceChoice(PlaceKind.COORDINATES, null, null, Coordinates(-33.87, 151.21), "Australia/Sydney")

            store
                .settings()
                .first()
                .language.code shouldBe "fa"
            store
                .settings()
                .first()
                .place
                .shouldBeNull()

            store.choose(city)
            store.settings().first().place shouldBe city
            preferences.preferences.first().place shouldBe
                ChosenPlace(PlaceSource.CITY, 1, "تهران", tehran.coordinates, "Asia/Tehran")

            store.choose(typed)
            store.settings().first().place shouldBe typed
            store.choose(typed.copy(kind = PlaceKind.DEVICE))
            store
                .settings()
                .first()
                .place
                ?.kind shouldBe PlaceKind.DEVICE
            shouldThrow<IllegalArgumentException> { store.choose(typed.copy(timeZoneId = "Mars/Olympus_Mons")) }
        }

    @Test
    fun `city search names results in the current language with region and country`(): Unit =
        runTest {
            var language = "fa"
            val search = CatalogCitySearch(catalogOf(tehran, noZone)) { language }

            search.search("Teh") shouldBe
                listOf(CityOption(1, "تهران", "Tehran · IR", tehran.coordinates, "Asia/Tehran"))
            language = "en"
            search.search("تهران").single().name shouldBe "Tehran"
            search
                .search("Nowhere")
                .single()
                .detail
                .shouldBeNull()
        }

    @Test
    fun `device fixes map to the settings outcomes`(): Unit =
        runTest {
            val fixes =
                mapOf(
                    LocationFix.Found(tehran.coordinates, 12f, null, "gps") to DeviceFix.Found(tehran.coordinates),
                    LocationFix.PermissionDenied to DeviceFix.PermissionDenied,
                    LocationFix.LocationDisabled to DeviceFix.LocationDisabled,
                    LocationFix.TimedOut to DeviceFix.TimedOut,
                    LocationFix.Unavailable to DeviceFix.Unavailable,
                )

            fixes.forEach { (fix, expected) -> LocatorDeviceLocation { fix }.locate() shouldBe expected }
        }

    @Test
    fun `descriptions prefer the geocoder's name and take the zone of the nearest city`(): Unit =
        runTest {
            val here = Coordinates(35.7, 51.4)

            fun describer(
                result: GeocodeResult,
                vararg cities: City,
            ) = GeocoderPlaceDescriber({ result }, catalogOf(*cities), { "fa" }, { "UTC" })

            describer(found(locality = "ونک", subAdminArea = "تهران"), tehran).describe(here) shouldBe
                PlaceDescription("ونک", "Asia/Tehran")
            describer(found(locality = null, subAdminArea = "شمیرانات"), tehran).describe(here) shouldBe
                PlaceDescription("شمیرانات", "Asia/Tehran")
            describer(GeocodeResult.NotFound, tehran).describe(here) shouldBe PlaceDescription("تهران", "Asia/Tehran")
            describer(GeocodeResult.Failed, noZone).describe(here) shouldBe PlaceDescription("Nowhere", "UTC")
            describer(GeocodeResult.Unavailable).describe(here) shouldBe PlaceDescription(null, "UTC")
        }

    private fun found(
        locality: String?,
        subAdminArea: String?,
    ) = GeocodeResult.Found(listOf(GeocodedPlace(locality, subAdminArea, null, "IR", null, null)))
}
