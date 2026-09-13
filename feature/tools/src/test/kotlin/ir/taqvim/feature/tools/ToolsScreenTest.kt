/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import android.app.Application
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.testing.FakeClock
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/** T-1400 UI tests: what each tool shows and which actions it reports. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ToolsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<Any>()
    private val actions =
        ToolsActions(
            onSelectTab = { calls += it },
            onInputsChange = { calls += it },
            onAddZone = { calls += "add $it" },
            onRemoveZone = { calls += "remove $it" },
            onShareQr = { calls += "share" },
        )

    private fun show(state: ToolsUiState) {
        composeRule.setContent { ToolsTestTheme { ToolsScreen(state, actions) } }
    }

    @Test
    fun converterShowsEveryCalendarAndReportsTypingAndTabs() {
        show(ToolsFixtures.state(ToolsTab.CONVERTER))
        composeRule.onNodeWithText("Tools").assertIsDisplayed()
        composeRule.onNodeWithText("Today").assertIsDisplayed()
        composeRule.onNodeWithText("Solar Hijri").assertIsDisplayed()
        composeRule.onNodeWithText("Gregorian").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Date").performTextInput("tomorrow")
        composeRule.onNodeWithText("Duration").performClick()
        assertEquals(listOf(ToolsInputs(converter = "tomorrow"), ToolsTab.DURATION), calls)
    }

    @Test
    fun distanceShowsWeeksPeriodAndWorkdays() {
        show(ToolsFixtures.state(ToolsTab.DISTANCE, ToolsInputs(distanceFrom = "1405/1/1", distanceTo = "1405/6/22")))
        composeRule.onNodeWithText("Weeks and days").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("25 + 1").assertExists()
        composeRule.onNodeWithText("In the Solar Hijri calendar").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Workdays, without the last day").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("151").assertExists()
    }

    @Test
    fun unrecognizedDistanceEndsAreFlagged() {
        show(ToolsFixtures.state(ToolsTab.DISTANCE, ToolsInputs(distanceFrom = "1405/1/1", distanceTo = "banana")))
        composeRule.onNodeWithText("No date found in this text").assertIsDisplayed()
        composeRule.onNodeWithText("Weeks and days").assertDoesNotExist()
    }

    @Test
    fun durationShowsPartsAndTotals() {
        show(ToolsFixtures.state(ToolsTab.DURATION, ToolsInputs(duration = "1d 2h + 30m")))
        composeRule.onNodeWithText("Total hours").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("26.5").assertExists()
    }

    @Test
    fun durationErrorsSayWhatAndWhere() {
        show(ToolsFixtures.state(ToolsTab.DURATION, ToolsInputs(duration = "5")))
        composeRule.onNodeWithText("Add a unit such as d, h, m or s after the number at position 1").assertIsDisplayed()
    }

    @Test
    fun timeZonesAddAndRemoveZones() {
        show(ToolsFixtures.state(ToolsTab.TIME_ZONES, ToolsInputs(zoneQuery = "new_york")))
        composeRule.onNodeWithText("Your time zone", substring = true).assertExists()
        composeRule.onNodeWithText("UTC+04:30").assertExists()
        composeRule.onNodeWithText("Next day").assertExists()
        composeRule.onNodeWithText("Add America/New_York").performClick()
        composeRule.onNodeWithText("Remove Kabul").performScrollTo().performClick()
        assertEquals(listOf("add America/New_York", "remove Asia/Kabul"), calls)
    }

    @Test
    fun qrCodeIsDescribedAndShared() {
        show(ToolsFixtures.state(ToolsTab.QR, ToolsInputs(qr = "https://example.com")))
        composeRule.onNodeWithContentDescription("QR code of the text").assertExists()
        composeRule.onNodeWithText("Share QR code").performScrollTo().performClick()
        assertEquals(listOf("share"), calls)
    }

    @Test
    fun qrMessagesAndLoading() {
        show(ToolsFixtures.state(ToolsTab.QR, ToolsInputs(qr = "€".repeat(QrEncoder.MAX_LENGTH))))
        composeRule.onNodeWithText("This text is too long for a QR code.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadingIsAnnounced() {
        show(ToolsUiState())
        composeRule.onNodeWithContentDescription("Loading tools").assertExists()
    }

    @Test
    fun routeSharesTheQrCodeAsAnImage() {
        val viewModel = ToolsViewModel({ flowOf(ToolsFixtures.settings()) }, FakeClock(ToolsFixtures.NOW))
        composeRule.setContent { ToolsTestTheme { ToolsRoute(viewModel = viewModel) } }
        composeRule.onNodeWithText("Type something to make a QR code.").assertDoesNotExist()
        composeRule.onNodeWithText("QR code").performClick()
        composeRule.onNodeWithText("Type something to make a QR code.").assertIsDisplayed()
        composeRule.onNodeWithText("Text or link").performTextInput("hello")
        composeRule.onNodeWithText("Share QR code").performScrollTo().performClick()

        val application = ApplicationProvider.getApplicationContext<Application>()
        val chooser = shadowOf(application).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val send = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        assertEquals("hello", send?.getStringExtra(Intent.EXTRA_TEXT))
        val stream = send?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        assertEquals(QrShare.authority(application), stream?.authority)
    }

    @Test
    fun sharedImagesHaveAQuietZone() {
        val matrix = requireNotNull(QrEncoder.encode("hello") as? QrState.Code).matrix
        val bitmap = QrShare.bitmap(matrix)
        val modulePixels = bitmap.width / (matrix.size + 8)
        assertEquals(bitmap.width, bitmap.height)
        assertEquals(Color.WHITE, bitmap.getPixel(0, 0))
        // The top-left finder pattern starts right after the four-module quiet zone.
        assertEquals(Color.BLACK, bitmap.getPixel(4 * modulePixels, 4 * modulePixels))
    }
}
