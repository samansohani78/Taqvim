/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import app.cash.turbine.test
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.Coordinates
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** T-1502: city search, device position and typed coordinates each store the place; failures store nothing. */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationSettingsViewModelTest {
    private val tehran = LocationFixtures.tehran

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(
        store: FakeLocationStore,
        search: CitySearch = FakeCitySearch(),
        device: DeviceLocation = DeviceLocation { DeviceFix.TimedOut },
        describer: PlaceDescriber = LocationFixtures.describer,
    ): LocationSettingsViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return LocationSettingsViewModel(store, search, device, describer, PlatformTimeZoneIds)
    }

    private fun persian(value: String): String = Numerals.format(BigDecimal(value), NumeralSystem.PERSIAN)

    @Test
    fun `the stored place is shown in the language's digits`(): Unit =
        runTest {
            val store = FakeLocationStore(LocationFixtures.persian, LocationFixtures.choice(tehran))
            val viewModel = viewModel(store)

            viewModel.uiState.test {
                awaitItem().loading shouldBe true
                val loaded = awaitItem()
                loaded.loading shouldBe false
                val coordinates =
                    persian("35.6892") + CoordinateInput.separator(NumeralSystem.PERSIAN) + persian("51.3890")
                loaded.current shouldBe CurrentPlace(PlaceKind.CITY, "Tehran", coordinates, "Asia/Tehran")
                viewModel.onModeSelected(LocationMode.COORDINATES)
                awaitItem().mode shouldBe LocationMode.COORDINATES
            }
        }

    @Test
    fun `search waits for typing to pause, keeps the ranking and marks the chosen city`(): Unit =
        runTest {
            val search = FakeCitySearch()
            val store = FakeLocationStore(LocationFixtures.english, LocationFixtures.choice(tehran))
            val viewModel = viewModel(store, search)
            advanceUntilIdle()

            listOf("k", "ka", "a").forEach(viewModel::onQueryChanged)
            viewModel.uiState.value.search.searching shouldBe true
            advanceTimeBy(250)
            search.queries.shouldBeEmpty()
            advanceUntilIdle()

            search.queries shouldBe listOf("a")
            val rows = viewModel.uiState.value.search.results
            rows.map { it.name } shouldBe listOf("Tehran", "Karaj", "Nowhere Island")
            rows.map { it.selected } shouldBe listOf(true, false, false)
            rows.map { it.selectable } shouldBe listOf(true, true, false)
            rows.first().coordinates shouldBe "35.6892, 51.3890"
            viewModel.uiState.value.search.searching shouldBe false

            viewModel.onQueryChanged("zz")
            advanceUntilIdle()
            viewModel.uiState.value.search.noResults shouldBe true

            viewModel.onQueryChanged(" ")
            advanceUntilIdle()
            viewModel.uiState.value.search shouldBe CitySearchState(query = " ")
            search.queries shouldBe listOf("a", "zz")
        }

    @Test
    fun `choosing a city stores it while cities without a zone and unknown ids are ignored`(): Unit =
        runTest {
            val store = FakeLocationStore(LocationFixtures.english)
            val viewModel = viewModel(store)
            viewModel.onQueryChanged("a")
            advanceUntilIdle()

            viewModel.onCitySelected(LocationFixtures.nowhere.id)
            viewModel.onCitySelected(999)
            viewModel.onCitySelected(LocationFixtures.karaj.id)
            advanceUntilIdle()

            store.chosen shouldBe listOf(LocationFixtures.choice(LocationFixtures.karaj))
            viewModel.uiState.value.current
                ?.name shouldBe "Karaj"
            viewModel.uiState.value.search.results
                .single { it.selected }
                .name shouldBe "Karaj"
        }

    @Test
    fun `the device position is stored with the described name and zone`(): Unit =
        runTest {
            val store = FakeLocationStore(LocationFixtures.english)
            val position = Coordinates(29.61, 52.53)
            val viewModel = viewModel(store, device = { DeviceFix.Found(position) })
            advanceUntilIdle()

            viewModel.onLocate()
            viewModel.uiState.value.device shouldBe DeviceState.Locating
            advanceUntilIdle()

            val expected = PlaceChoice(PlaceKind.DEVICE, null, "Shiraz", position, "Asia/Tehran")
            store.chosen shouldBe listOf(expected)
            viewModel.uiState.value.device shouldBe DeviceState.Saved("Shiraz", "29.6100, 52.5300", "Asia/Tehran")
            viewModel.uiState.value.current
                ?.kind shouldBe PlaceKind.DEVICE
        }

    @Test
    fun `denied, disabled, timed-out and unavailable positions store nothing`(): Unit =
        runTest {
            val outcomes =
                mapOf(
                    DeviceFix.PermissionDenied to DeviceState.PermissionDenied,
                    DeviceFix.LocationDisabled to DeviceState.LocationDisabled,
                    DeviceFix.TimedOut to DeviceState.TimedOut,
                    DeviceFix.Unavailable to DeviceState.Unavailable,
                )
            outcomes.forEach { (fix, state) ->
                val store = FakeLocationStore(LocationFixtures.english)
                val viewModel = viewModel(store, device = { fix })
                viewModel.onLocate()
                advanceUntilIdle()
                viewModel.uiState.value.device shouldBe state
                store.chosen.shouldBeEmpty()
            }
            val store = FakeLocationStore(LocationFixtures.english)
            val viewModel = viewModel(store)
            viewModel.onPermissionDenied()
            viewModel.uiState.value.device shouldBe DeviceState.PermissionDenied
            store.chosen.shouldBeEmpty()
        }

    @Test
    fun `typed coordinates get a live suggestion and are stored`(): Unit =
        runTest {
            val store = FakeLocationStore(LocationFixtures.persian)
            val viewModel = viewModel(store)
            advanceUntilIdle()

            viewModel.onLatitudeChanged(persian("29.61"))
            viewModel.onLongitudeChanged("52.53")
            viewModel.uiState.value.manual.suggestedName
                .shouldBeNull()
            advanceUntilIdle()
            viewModel.uiState.value.manual.suggestedName shouldBe "Shiraz"
            viewModel.uiState.value.manual.timeZoneId shouldBe "Asia/Tehran"

            viewModel.onSaveCoordinates()
            advanceUntilIdle()
            store.chosen shouldBe
                listOf(PlaceChoice(PlaceKind.COORDINATES, null, "Shiraz", Coordinates(29.61, 52.53), "Asia/Tehran"))
            viewModel.uiState.value.manual.saved shouldBe true

            viewModel.onLatitudeChanged("29.7")
            viewModel.uiState.value.manual.saved shouldBe false
            viewModel.uiState.value.manual.suggestedName
                .shouldBeNull()
        }

    @Test
    fun `invalid fields are flagged, nothing is stored, and a zone the user typed is kept`(): Unit =
        runTest {
            val store = FakeLocationStore(LocationFixtures.english)
            val viewModel = viewModel(store)
            advanceUntilIdle()

            viewModel.onSaveCoordinates()
            viewModel.uiState.value.manual shouldBe
                ManualState(latitudeError = true, longitudeError = true, timeZoneError = true)

            viewModel.onLatitudeChanged("91")
            viewModel.onLongitudeChanged("east")
            viewModel.onTimeZoneChanged("Mars/Base")
            viewModel.uiState.value.manual.let {
                it.latitudeError shouldBe true
                it.longitudeError shouldBe true
                it.timeZoneError shouldBe true
            }
            viewModel.onSaveCoordinates()
            advanceUntilIdle()
            store.chosen.shouldBeEmpty()

            viewModel.onTimeZoneChanged("Asia/Dubai")
            viewModel.onLatitudeChanged("25.2")
            viewModel.onLongitudeChanged("55.27")
            advanceUntilIdle()
            viewModel.uiState.value.manual.let {
                it.timeZoneId shouldBe "Asia/Dubai"
                it.suggestedName shouldBe "Shiraz"
                it.latitudeError shouldBe false
                it.longitudeError shouldBe false
                it.timeZoneError shouldBe false
            }
            viewModel.onSaveCoordinates()
            advanceUntilIdle()
            store.chosen.single().timeZoneId shouldBe "Asia/Dubai"
        }
}
