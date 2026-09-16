/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.lifecycle.viewModelScope
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** Code review I04: failing or cancelled location actions show a retryable state and never stay busy. */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationFailureTest {
    /** Stores through [FakeLocationStore] unless [failing]. */
    private class FlakyStore(
        private val stored: FakeLocationStore = FakeLocationStore(LocationFixtures.english),
    ) : LocationSettingsStore {
        var failing = true

        override fun settings(): Flow<LocationSettings> = stored.settings()

        override suspend fun choose(place: PlaceChoice) {
            check(!failing) { "disk full" }
            stored.choose(place)
        }
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(
        store: LocationSettingsStore,
        search: CitySearch = FakeCitySearch(),
        device: DeviceLocation = DeviceLocation { DeviceFix.Found(LocationFixtures.tehran.coordinates) },
        describer: PlaceDescriber = LocationFixtures.describer,
    ): LocationSettingsViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return LocationSettingsViewModel(store, search, device, describer, PlatformTimeZoneIds)
    }

    @Test
    fun `a failing search offers a retry that searches again`(): Unit =
        runTest {
            var failing = true
            val cities = FakeCitySearch()
            val search =
                CitySearch { query ->
                    check(!failing) { "catalog unavailable" }
                    cities.search(query)
                }
            val model = viewModel(FakeLocationStore(LocationFixtures.english), search)
            model.onQueryChanged("teh")
            advanceUntilIdle()
            model.uiState.value.search.run {
                failed shouldBe true
                searching shouldBe false
                noResults shouldBe false
            }

            failing = false
            model.onRetrySearch()
            model.uiState.value.search.failed shouldBe false
            advanceUntilIdle()
            model.uiState.value.search.results
                .map { it.name } shouldBe listOf("Tehran")
        }

    @Test
    fun `a place that cannot be stored is reported and can be chosen again`(): Unit =
        runTest {
            val store = FlakyStore()
            val model = viewModel(store)
            model.onQueryChanged("teh")
            advanceUntilIdle()
            model.onCitySelected(LocationFixtures.tehran.id)
            advanceUntilIdle()
            model.uiState.value.saveFailed shouldBe true
            model.uiState.value.current
                .shouldBeNull()

            model.onLatitudeChanged("35.7")
            model.onLongitudeChanged("51.4")
            model.onTimeZoneChanged("Asia/Tehran")
            model.onSaveCoordinates()
            advanceUntilIdle()
            model.uiState.value.saveFailed shouldBe true
            model.uiState.value.manual.saved shouldBe false

            store.failing = false
            model.onCitySelected(LocationFixtures.tehran.id)
            advanceUntilIdle()
            model.uiState.value.saveFailed shouldBe false
            model.uiState.value.current
                ?.name shouldBe "Tehran"
        }

    @Test
    fun `a failing device lookup is unavailable, a cancelled one is not busy`(): Unit =
        runTest {
            val failing = viewModel(FlakyStore())
            failing.onLocate()
            advanceUntilIdle()
            failing.uiState.value.device shouldBe DeviceState.Unavailable

            val gate = CompletableDeferred<DeviceFix>()
            val waiting = viewModel(FakeLocationStore(LocationFixtures.english), device = { gate.await() })
            waiting.onLocate()
            advanceUntilIdle()
            waiting.uiState.value.device shouldBe DeviceState.Locating
            waiting.viewModelScope.cancel()
            advanceUntilIdle()
            waiting.uiState.value.device shouldBe DeviceState.Idle
        }

    @Test
    fun `a failing place description leaves the typed coordinates usable`(): Unit =
        runTest {
            val store = FakeLocationStore(LocationFixtures.english)
            val model = viewModel(store, describer = { error("geocoder offline") })
            model.onLatitudeChanged("35.7")
            model.onLongitudeChanged("51.4")
            advanceUntilIdle()
            model.uiState.value.manual.suggestedName
                .shouldBeNull()

            model.onTimeZoneChanged("Asia/Tehran")
            model.onSaveCoordinates()
            advanceUntilIdle()
            model.uiState.value.manual.saved shouldBe true
            store.chosen.single().timeZoneId shouldBe "Asia/Tehran"
        }
}
