/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/**
 * T-1500 table-driven UI test: for every item of [SettingsCatalog], interacting with it on the settings home changes
 * the stored value (or opens its page); settings search and deep links reach items in other tabs.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsHomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val opened = mutableListOf<SettingsDestination>()

    private fun text(resource: Int): String = context.getString(resource)

    private fun settle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        composeRule.waitForIdle()
    }

    private fun show(
        store: FakeGeneralSettingsStore,
        initialItem: SettingsItemId? = null,
    ) {
        val viewModel = SettingsHomeViewModel(store, initialItem)
        composeRule.setContent {
            LocationTestTheme {
                SettingsHomeRoute(
                    initialItem = initialItem,
                    navigation = SettingsNavigation { opened += it },
                    viewModel = viewModel,
                )
            }
        }
        settle()
    }

    private fun openRow(id: SettingsItemId) {
        composeRule.onNodeWithText(text(id.tab.title)).performClick()
        settle()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text(id.title)))
        composeRule.onNodeWithText(text(id.title)).performClick()
        settle()
    }

    private fun clickOption(option: SettingsOption) {
        val label = option.text ?: text(option.label)
        composeRule.onNode(hasText(label) and hasAnyAncestor(isDialog())).performScrollTo().performClick()
        settle()
    }

    /** Interacts with [id] once and returns the settings it is expected to store. */
    private fun interact(
        id: SettingsItemId,
        control: SettingsControl,
        before: GeneralSettings,
    ): GeneralSettings =
        when (control) {
            is SettingsControl.Toggle -> {
                openRow(id)
                control.write(before, !control.read(before))
            }

            is SettingsControl.Choice -> {
                val option = control.options.first { control.write(before, it.key) != before }
                openRow(id)
                clickOption(option)
                control.write(before, option.key)
            }

            is SettingsControl.MultiChoice -> {
                val chosen = control.read(before)
                val option = control.options.first { (chosen - it.key).isNotEmpty() || it.key !in chosen }
                val next = if (option.key in chosen) chosen - option.key else chosen + option.key
                openRow(id)
                clickOption(option)
                composeRule.onNodeWithText(text(R.string.settings_dialog_done)).performClick()
                settle()
                control.write(before, next)
            }

            is SettingsControl.Link -> {
                openRow(id)
                assertEquals(id.name, control.destination, opened.last())
                before
            }

            SettingsControl.ClearRecentSearches -> {
                openRow(id)
                before.copy(hasRecentSearches = false)
            }
        }

    @Test
    fun everySettingChangesTheStoredValue() {
        val store = FakeGeneralSettingsStore(LocationFixtures.english)
        show(store)

        SettingsItemId.entries.forEach { id ->
            val expected = interact(id, SettingsCatalog.control(id), store.current)
            assertEquals(id.name, expected, store.current)
        }
        assertEquals(1, store.clears)
        assertEquals(SettingsItemId.entries.count { SettingsCatalog.control(it) is SettingsControl.Link }, opened.size)
    }

    @Test
    fun searchReachesOtherTabsAndDeepLinksHighlightTheirItem() {
        val store = FakeGeneralSettingsStore(LocationFixtures.english)
        show(store, initialItem = SettingsItemId.HIGH_LATITUDE)

        composeRule.onNodeWithText(text(SettingsItemId.HIGH_LATITUDE.title)).assertIsSelected()

        composeRule.onNode(hasSetTextAction()).performTextInput("contrast")
        settle()
        composeRule.onNodeWithText(text(SettingsItemId.HIGH_CONTRAST.title)).assertIsDisplayed().performClick()
        settle()
        assertEquals(true, store.current.highContrast)

        composeRule.onNode(hasSetTextAction()).performTextReplacement("zzzz")
        settle()
        composeRule.onNodeWithText(context.getString(R.string.settings_search_no_results, "zzzz")).assertIsDisplayed()
    }

    @Test
    fun loadingIsAnnounced() {
        composeRule.setContent {
            LocationTestTheme { SettingsHomeScreen(SettingsHomeUiState(), SettingsHomeActions()) }
        }

        composeRule.onNodeWithContentDescription(text(R.string.settings_home_loading)).assertIsDisplayed()
    }
}
