/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import ir.taqvim.core.events.EventCategory
import ir.taqvim.core.i18n.LanguageTable
import java.time.Duration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * The religious occasions switch is in the settings list itself, not behind a dialog a user has to find first
 * (T-302, T-1500).
 *
 * The categories used to be one row called "Kinds of day shown", which opened a dialog of checkboxes: a user who
 * wants religious occasions gone had to guess that phrase meant them. Each kind of day is now its own switch,
 * labelled in the user's words, and this is what proves it is reachable without opening anything.
 */
@RunWith(RobolectricTestRunner::class)
class ReligiousToggleTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val religious = SettingsItemId.EVENT_CATEGORY_RELIGIOUS

    private fun text(resource: Int): String = context.getString(resource)

    private fun settle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        composeRule.waitForIdle()
    }

    private fun store() = FakeGeneralSettingsStore(requireNotNull(LanguageTable.forCode("en")))

    private fun show(store: FakeGeneralSettingsStore) {
        val viewModel = SettingsHomeViewModel(store, null)
        composeRule.setContent {
            LocationTestTheme {
                SettingsHomeRoute(
                    initialItem = null,
                    navigation = SettingsNavigation { },
                    viewModel = viewModel,
                )
            }
        }
        settle()
    }

    @Test
    fun `the religious occasions switch is a row of the settings list`() {
        show(store())

        composeRule.onNodeWithText(text(religious.tab.title)).performClick()
        settle()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text(religious.title)))

        // Its own words, on the page itself — no dialog was opened to get here.
        composeRule.onNodeWithText(text(religious.title)).assertIsDisplayed()
    }

    @Test
    fun `switching it off removes only the religious category, and records the choice`() {
        val store = store()
        show(store)
        composeRule.onNodeWithText(text(religious.tab.title)).performClick()
        settle()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text(religious.title)))

        composeRule.onNodeWithText(text(religious.title)).performClick()
        settle()

        val saved = store.current
        saved.enabledEventCategories shouldNotContain EventCategory.RELIGIOUS
        saved.enabledEventCategories.contains(EventCategory.NATIONAL) shouldBe true
        saved.eventCategoriesChosen shouldBe true
    }

    @Test
    fun `every kind of day has its own switch`() {
        val ids =
            SettingsItemId.entries.filter { it.name.startsWith("EVENT_CATEGORY_") }.map {
                SettingsCatalog.control(it)
            }

        ids.size shouldBe EventCategory.entries.size - 1 // PERSONAL is never filtered by category
        ids.all { it is SettingsControl.Toggle } shouldBe true
    }
}
