/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import android.Manifest
import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.model.Coordinates
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/** T-1502 UI tests: each way of choosing a place stores its coordinates; states and errors are shown. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocationSettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val store = FakeLocationStore(LocationFixtures.english)

    private fun showRoute(device: DeviceLocation = DeviceLocation { DeviceFix.TimedOut }) {
        val viewModel =
            LocationSettingsViewModel(store, FakeCitySearch(), device, LocationFixtures.describer, PlatformTimeZoneIds)
        composeRule.setContent { LocationTestTheme { LocationSettingsRoute(viewModel = viewModel) } }
        settle()
    }

    private val screenState = mutableStateOf(LocationSettingsUiState())
    private var screenShown = false

    /** Shows [state]; content is set once per test and later calls only replace the state. */
    private fun show(state: LocationSettingsUiState) {
        screenState.value = state
        if (!screenShown) {
            screenShown = true
            composeRule.setContent {
                LocationTestTheme { LocationSettingsScreen(screenState.value, LocationSettingsActions()) }
            }
        }
        composeRule.waitForIdle()
    }

    /** Runs the main looper past the view model's debounce delays, then lets Compose settle. */
    private fun settle() {
        shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        composeRule.waitForIdle()
    }

    @Test
    fun choosingACityStoresItsCoordinates() {
        showRoute()
        composeRule.onNodeWithText("No place chosen yet").assertIsDisplayed()
        composeRule.onNodeWithText("Search cities").performTextInput("kar")
        settle()
        composeRule.onNodeWithText("Karaj").performClick()
        settle()

        assertEquals(listOf(LocationFixtures.choice(LocationFixtures.karaj)), store.chosen)
        composeRule.onNodeWithText("Chosen from the city list").assertIsDisplayed()
        composeRule.onNodeWithText("Chosen").assertIsSelected()
    }

    @Test
    fun usingTheDeviceLocationStoresItsCoordinates() {
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        val position = Coordinates(29.61, 52.53)
        showRoute(device = { DeviceFix.Found(position) })
        composeRule.onNodeWithText("My location").performClick()
        composeRule.onNodeWithText("Use my location").performClick()
        settle()

        assertEquals(listOf(PlaceChoice(PlaceKind.DEVICE, null, "Shiraz", position, "Asia/Tehran")), store.chosen)
        composeRule.onNodeWithText("Saved: Shiraz").assertIsDisplayed()
    }

    @Test
    fun typedCoordinatesAreStoredWithTheSuggestedZone() {
        showRoute()
        composeRule.onNodeWithText("Coordinates").performClick()
        composeRule.onNodeWithText("Latitude").performTextInput("29.61")
        composeRule.onNodeWithText("Longitude").performTextInput("52.53")
        settle()
        composeRule.onNodeWithText("Near Shiraz").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Save coordinates").performScrollTo().performClick()
        settle()

        assertEquals(
            listOf(PlaceChoice(PlaceKind.COORDINATES, null, "Shiraz", Coordinates(29.61, 52.53), "Asia/Tehran")),
            store.chosen,
        )
        composeRule.onNodeWithText("Coordinates saved").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun deniedPermissionExplainsWhatToDo() {
        val calls = mutableListOf<String>()
        composeRule.setContent {
            LocationTestTheme {
                LocationSettingsScreen(
                    LocationSettingsUiState(
                        loading = false,
                        mode = LocationMode.DEVICE,
                        device = DeviceState.PermissionDenied,
                    ),
                    LocationSettingsActions(onUseDeviceLocation = { calls += "locate" }),
                )
            }
        }
        composeRule.onNodeWithText("Location permission was denied", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Use my location").performClick()
        assertEquals(listOf("locate"), calls)
    }

    @Test
    fun otherDeviceProblemsAndLocating() {
        mapOf(
            DeviceState.LocationDisabled to "Location is switched off",
            DeviceState.TimedOut to "could not be found in time",
            DeviceState.Unavailable to "not available right now",
            DeviceState.Locating to "Finding your location",
        ).forEach { (device, text) ->
            show(LocationSettingsUiState(loading = false, mode = LocationMode.DEVICE, device = device))
            composeRule.onNodeWithText(text, substring = true).assertIsDisplayed()
        }
    }

    @Test
    fun cityWithoutZoneCannotBeChosenAndErrorsAreShown() {
        val rows = LocationStateMapper.rows(listOf(LocationFixtures.nowhere), NumeralSystem.LATIN, null)
        show(LocationSettingsUiState(loading = false, search = CitySearchState(query = "no", results = rows)))
        composeRule.onNodeWithText("Nowhere Island").assertIsNotEnabled()
        composeRule.onNodeWithText("cannot be chosen", substring = true).assertIsDisplayed()

        show(LocationSettingsUiState(loading = false, search = CitySearchState(query = "zz", noResults = true)))
        composeRule.onNodeWithText("No city matches", substring = true).assertIsDisplayed()

        show(
            LocationSettingsUiState(
                loading = false,
                mode = LocationMode.COORDINATES,
                manual = ManualState(latitudeError = true, longitudeError = true, timeZoneError = true),
            ),
        )
        composeRule.onNodeWithText("Enter a latitude between -90 and 90").assertIsDisplayed()
        composeRule.onNodeWithText("Enter a longitude between -180 and 180").assertIsDisplayed()
        composeRule.onNodeWithText("Unknown time zone").assertIsDisplayed()
    }

    @Test
    fun loadingIsAnnounced() {
        show(LocationSettingsUiState())
        composeRule.onNodeWithContentDescription("Loading location settings").assertExists()
    }

    @Test
    fun theMapPickIsOfferedWhereAMapCanBeOpened() {
        var opened = 0
        composeRule.setContent {
            LocationTestTheme {
                LocationSettingsScreen(
                    LocationSettingsUiState(loading = false, mode = LocationMode.COORDINATES),
                    LocationSettingsActions(onPickOnMap = { opened++ }),
                )
            }
        }
        composeRule.onNodeWithText("Pick on the map").performScrollTo().performClick()
        assertEquals(1, opened)
    }

    @Test
    fun withoutAMapNoMapPickIsOffered() {
        show(LocationSettingsUiState(loading = false, mode = LocationMode.COORDINATES))
        composeRule.onNodeWithText("Save coordinates").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Pick on the map").assertDoesNotExist()
    }
}
