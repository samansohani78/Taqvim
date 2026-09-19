/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import android.app.Application
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/** T-1504 UI tests: what each page shows, its actions, and the report intent built without personal data. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AboutScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val application = ApplicationProvider.getApplicationContext<Application>()

    private fun show(
        state: AboutUiState,
        actions: AboutActions = AboutActions(),
    ) {
        composeRule.setContent { AboutTestTheme { AboutScreen(state, actions) } }
    }

    private fun route(crashes: CrashReportSource = FakeCrashes()): AboutViewModel {
        val viewModel =
            AboutViewModel(
                { flowOf(AboutFixtures.info) },
                { flowOf(AboutFixtures.entries) },
                FakeLicenses(AboutFixtures.catalog),
                { AboutFixtures.device },
                crashes,
            )
        composeRule.setContent { AboutTestTheme { AboutRoute(viewModel = viewModel) } }
        return viewModel
    }

    /** Makes an e-mail app visible to the package manager, as a phone with one installed would. */
    private fun installEmailApp() {
        val component = ComponentName("mail.example", "mail.example.ComposeActivity")
        val shadow = shadowOf(application.packageManager)
        shadow.addActivityIfNotPresent(component)
        shadow.addIntentFilterForActivity(
            component,
            IntentFilter(Intent.ACTION_SENDTO).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataScheme("mailto")
            },
        )
    }

    @Test
    fun homeShowsFactsLinksAndPages() {
        val calls = mutableListOf<String>()
        val actions =
            AboutActions(
                onOpenLink = { calls += it },
                onOpenLicenses = { calls += "licenses" },
                onOpenDataSources = { calls += "data" },
                onOpenDiagnostics = { calls += "diagnostics" },
                onRequestReport = { calls += "report" },
            )
        show(AboutUiState(info = AboutFixtures.info), actions)

        composeRule.onNodeWithText("Taqvim").assertIsDisplayed()
        composeRule.onNodeWithText("Version 1.0.0 (42, debug)").assertIsDisplayed()
        composeRule.onNodeWithText("Source code").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        listOf("Website", "Open-source licenses", "Data sources", "Diagnostics", "Report a problem").forEach {
            composeRule.onNodeWithText(it).performScrollTo().performClick()
        }
        assertEquals(listOf("https://taqvim.example", "licenses", "data", "diagnostics", "report"), calls)
    }

    @Test
    fun licensePagesListGroupsAndOpenTexts() {
        val calls = mutableListOf<String>()
        val ready = LicensesContent.Ready(AboutFixtures.catalog.groups(), 3)
        show(
            AboutUiState(page = AboutPage.LICENSES, canGoBack = true, licenses = ready),
            AboutActions(onOpenLicense = { calls += it }, onBack = { calls += "back" }),
        )

        composeRule.onNodeWithText("3 third-party libraries").assertIsDisplayed()
        composeRule.onNodeWithText("androidx.core:core").assertIsDisplayed()
        composeRule.onNodeWithText("1.13.1, 1.16.0").assertIsDisplayed()
        composeRule.onNodeWithText("Apache License 2.0").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertEquals(listOf("Apache-2.0", "back"), calls)
    }

    @Test
    fun licenseTextShowsTheBundledTextOrItsLink() {
        val opened = mutableListOf<String>()
        val text = LicenseTextContent(AboutFixtures.apache, "Apache License\nVersion 2.0", loading = false)
        show(
            AboutUiState(page = AboutPage.LICENSE_TEXT, canGoBack = true, licenseText = text),
            AboutActions(onOpenLink = { opened += it }),
        )

        composeRule.onNodeWithText("Apache License 2.0").assertIsDisplayed()
        composeRule.onNodeWithText("Apache License\nVersion 2.0").assertIsDisplayed()
        composeRule.onNodeWithText("Open the license page").performClick()
        assertEquals(listOf("https://www.apache.org/licenses/LICENSE-2.0"), opened)
    }

    @Test
    fun missingLicenseTextPointsToTheLicensePage() {
        val text = LicenseTextContent(AboutFixtures.mit, text = null, loading = false)
        show(AboutUiState(page = AboutPage.LICENSE_TEXT, canGoBack = true, licenseText = text))

        composeRule
            .onNodeWithText("The full text is not included in the app; open the license page.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Open the license page").assertDoesNotExist()
    }

    @Test
    fun dataSourcesOpenTheBundledLicenseTextAndTheCcByDeed() {
        val calls = mutableListOf<String>()
        show(
            AboutUiState(page = AboutPage.DATA_SOURCES, canGoBack = true),
            AboutActions(onOpenLink = { calls += it }, onOpenDataLicense = { calls += "unicode" }),
        )

        composeRule.onNodeWithText("Unicode CLDR").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Read the license of Unicode CLDR").performClick()
        composeRule.onAllNodesWithText("Open source page")[0].performClick()
        // ADR-0039: the plate data is CC BY 4.0, so its row credits the creators and links the license deed.
        val plates = "EarthByte plate model (Matthews et al. 2016)"
        val plateLicense = "Read the license of $plates"
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasContentDescription(plateLicense))
        composeRule.onNodeWithText("Matthews, K. J.", substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(plateLicense).performClick()
        assertEquals(
            listOf("unicode", DataSource.CLDR.url, "https://creativecommons.org/licenses/by/4.0/"),
            calls,
        )
    }

    @Test
    fun diagnosticsFilterCopyAndShare() {
        val calls = mutableListOf<String>()
        val content = diagnosticsContent(AboutFixtures.entries, DiagnosticLevel.WARN)
        show(
            AboutUiState(page = AboutPage.DIAGNOSTICS, canGoBack = true, diagnostics = content),
            AboutActions(
                onMinimumLevel = { calls += it.name },
                onCopyDiagnostics = { calls += "copy" },
                onShareDiagnostics = { calls += "share" },
            ),
        )

        composeRule.onNodeWithText("2 entries hidden by the filter").assertIsDisplayed()
        composeRule.onNodeWithText("Reminder for [redacted] was late").assertIsDisplayed()
        composeRule.onNodeWithText(AboutFixtures.TITLE, substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Errors only").performClick()
        composeRule.onNodeWithText("Copy").performClick()
        composeRule.onNodeWithText("Share").performClick()
        assertEquals(listOf("ERROR", "copy", "share"), calls)
    }

    @Test
    fun reportIntentIsBuiltWithoutPersonalDataAfterConfirmation() {
        installEmailApp()
        route()
        composeRule.onNodeWithText("Report a problem").performScrollTo().performClick()
        composeRule.onNodeWithText("Send a problem report?").assertIsDisplayed()
        assertNull(shadowOf(application).nextStartedActivity)
        composeRule.onNodeWithText("Continue").performClick()

        val chooser = shadowOf(application).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val send = requireNotNull(chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java))
        assertEquals(Intent.ACTION_SENDTO, send.action)
        assertEquals("mailto:", send.dataString)
        assertEquals(listOf(AboutFixtures.SUPPORT), send.getStringArrayExtra(Intent.EXTRA_EMAIL)?.toList())
        val body = requireNotNull(send.getStringExtra(Intent.EXTRA_TEXT))
        assertTrue(body.contains("1.0.0") && body.contains("Pixel 8") && body.contains("16 (API 36)"))
        AboutFixtures.personalData.forEach { assertFalse("report contains $it", body.contains(it)) }
        composeRule.onNodeWithText("Send a problem report?").assertDoesNotExist()
    }

    @Test
    fun reportFallsBackToTheShareSheetWithoutAnEmailApp() {
        route()
        composeRule.onNodeWithText("Report a problem").performScrollTo().performClick()
        composeRule.onNodeWithText("Continue").performClick()

        val chooser = shadowOf(application).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val send = requireNotNull(chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java))
        assertEquals(Intent.ACTION_SEND, send.action)
        assertNull(send.getStringArrayExtra(Intent.EXTRA_EMAIL))
        val body = requireNotNull(send.getStringExtra(Intent.EXTRA_TEXT))
        assertTrue(body.contains("1.0.0"))
        AboutFixtures.personalData.forEach { assertFalse("report contains $it", body.contains(it)) }
    }

    @Test
    fun startingAnIntentNoAppCanHandleReportsFailureInsteadOfCrashing() {
        shadowOf(application).checkActivities(true)

        val started = AboutIntents.start(application, Intent("ir.taqvim.test.NOTHING_HANDLES_THIS"))

        assertFalse(started)
    }

    @Test
    fun cancellingTheReportStartsNothing() {
        route()
        composeRule.onNodeWithText("Report a problem").performScrollTo().performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Send a problem report?").assertDoesNotExist()
        assertNull(shadowOf(application).nextStartedActivity)
    }

    @Test
    fun copiedDiagnosticsAreRedacted() {
        route()
        composeRule.onNodeWithText("Diagnostics").performScrollTo().performClick()
        composeRule.onNodeWithText("Copy").performClick()

        val clip = requireNotNull(application.getSystemService(ClipboardManager::class.java).primaryClip)
        val text = clip.getItemAt(0).text.toString()
        assertEquals(4, text.lines().size)
        AboutFixtures.personalData.forEach { assertFalse("clipboard contains $it", text.contains(it)) }
    }

    @Test
    fun `the crash entry appears only after a crash and clearing forgets it`() {
        val crashes = FakeCrashes(listOf(AboutFixtures.crash))
        route(crashes)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Last crash").performScrollTo().performClick()
        composeRule.waitForIdle()

        // The stored record is shown redacted, with the facts that identify the run.
        composeRule.onNodeWithText("device=OnePlus PJZ110", substring = true).assertIsDisplayed()
        AboutFixtures.personalData.forEach {
            composeRule.onAllNodesWithText(it, substring = true).assertCountEquals(0)
        }

        composeRule.onNodeWithText("Clear").performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("Last crash").assertCountEquals(0)
    }

    @Test
    fun `a run that never crashed has no crash entry`() {
        route()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("Last crash").assertCountEquals(0)
    }
}
