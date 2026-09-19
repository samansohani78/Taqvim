/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import android.app.Application
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/** T-1901 UI tests: FAQ questions and answers in both languages, search, expanding, links and the report action. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FaqScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val application = ApplicationProvider.getApplicationContext<Application>()

    private fun show(
        state: AboutUiState,
        actions: AboutActions = AboutActions(),
    ) {
        composeRule.setContent { AboutTestTheme { AboutScreen(state, actions) } }
    }

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
    }

    @Test
    fun everyQuestionHasAnEnglishAndPersianQuestionAndAnswer() {
        val english = faqTexts(application.resources)
        val configuration =
            Configuration(application.resources.configuration).apply { setLocale(Locale.forLanguageTag("fa")) }
        val persian = faqTexts(application.createConfigurationContext(configuration).resources)

        FaqEntry.entries.forEach { entry ->
            val en = english.getValue(entry)
            val fa = persian.getValue(entry)
            listOf(en.topic, en.question, en.answer, fa.topic, fa.question, fa.answer).forEach {
                assertTrue("blank text of $entry", it.isNotBlank())
            }
            assertNotEquals("untranslated question of $entry", en.question, fa.question)
            assertNotEquals("untranslated answer of $entry", en.answer, fa.answer)
            assertTrue("no Persian letters in $entry", fa.answer.any { Character.UnicodeScript.of(it.code) == ARABIC })
        }
    }

    @Test
    fun homeOpensTheFaq() {
        val calls = mutableListOf<String>()
        show(AboutUiState(info = AboutFixtures.info), AboutActions(onOpenFaq = { calls += "faq" }))

        composeRule.onNodeWithText("Questions and answers").performScrollTo().performClick()
        assertEquals(listOf("faq"), calls)
    }

    @Test
    fun questionsExpandAndAnswersOpenTheirScreen() {
        val calls = mutableListOf<String>()
        val expanded = FaqContent(expanded = persistentSetOf(FaqEntry.MAIN_CALENDAR, FaqEntry.NEPALI))
        show(
            AboutUiState(page = AboutPage.FAQ, canGoBack = true, faq = expanded),
            AboutActions(
                onToggleFaq = { calls += "toggle ${it.name}" },
                onOpenFaqLink = { calls += "open ${it.name}" },
            ),
        )

        composeRule.onNodeWithText("Calendars").assertIsDisplayed()
        composeRule
            .onNode(hasText("How do I change the main or secondary calendar?") and stateIs("Expanded"))
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Open in Taqvim").performClick()
        scrollTo("Is the Nepali (Bikram Sambat) calendar available?")
        composeRule
            .onNodeWithText("Not yet. It will be added when the official month tables are available.", substring = true)
            .assertIsDisplayed()
        scrollTo("Are there home screen widgets?")
        composeRule.onNode(hasText("Are there home screen widgets?") and stateIs("Collapsed")).performClick()
        assertEquals(listOf("toggle MAIN_CALENDAR", "open MAIN_CALENDAR", "toggle WIDGETS"), calls)
    }

    @Test
    fun searchNoResultsAndReport() {
        val calls = mutableListOf<String>()
        show(
            AboutUiState(page = AboutPage.FAQ, canGoBack = true, faq = faqContent(emptyMap(), "zzz", emptySet())),
            AboutActions(onFaqQuery = { calls += "query $it" }, onRequestReport = { calls += "report" }),
        )

        composeRule.onNodeWithText("No question matches your search").assertIsDisplayed()
        composeRule.onNodeWithText("zzz").performTextInput("y")
        composeRule.onNodeWithText("Report a problem").performScrollTo().performClick()
        assertEquals(2, calls.size)
        assertTrue(calls[0], calls[0].startsWith("query ") && "zzz" in calls[0] && "y" in calls[0])
        assertEquals("report", calls[1])
    }

    @Test
    fun routeOpensAnAnswerLinkInsideTheApp() {
        val viewModel =
            AboutViewModel(
                { flowOf(AboutFixtures.info) },
                { flowOf(emptyList()) },
                FakeLicenses(AboutFixtures.catalog),
                { AboutFixtures.device },
                FakeCrashes(),
            )
        composeRule.setContent { AboutTestTheme { AboutRoute(viewModel = viewModel) } }

        composeRule.onNodeWithText("Questions and answers").performScrollTo().performClick()
        composeRule.onNodeWithText("Search the questions").performTextInput("passphrase")
        composeRule.onNodeWithText("I forgot the passphrase of a backup").performClick()
        composeRule.onNodeWithText("Open in Taqvim").performClick()
        composeRule.waitForIdle()

        val started = shadowOf(application).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals("taqvim://settings/backup", started.dataString)
        assertEquals(application.packageName, started.`package`)
    }

    private fun stateIs(description: String): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, description)

    private companion object {
        val ARABIC: Character.UnicodeScript = Character.UnicodeScript.ARABIC
    }
}
