/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeSettings
import ir.taqvim.feature.search.ToolEntry
import ir.taqvim.feature.settings.SettingsDestination
import kotlin.random.Random
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

/**
 * T-703: rapid random navigation — tab switches, back presses, links, screens and feature callbacks — never
 * crashes, keeps the calendar at the bottom of the back stack and leaves exactly the top screen composed once
 * transitions settle (popped screens and their ViewModel stores are gone). The seed is fixed and printed on failure;
 * set `-Dtaqvim.monkey.seed=<n>` to replay or explore another sequence.
 */
@RunWith(AndroidJUnit4::class)
class NavigationMonkeyTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val navigator = AppNavigator()
    private val router = AppRouter(navigator::navigate, NoExternalActions, showMessage = {})

    // Robolectric starts TaqvimApplication, and so Koin, for every test.
    @After
    fun stopDependencyGraph() {
        stopKoin()
    }

    @Test
    fun rapidRandomNavigationKeepsOneScreenAndAConsistentBackStack() {
        val seed = System.getProperty(SEED_PROPERTY)?.toLongOrNull() ?: DEFAULT_SEED
        val random = Random(seed)
        composeRule.setContent {
            TaqvimTheme(ThemeSettings(), TextDirection.LTR) {
                val snackbar = remember { SnackbarHostState() }
                AppNavigationFrame(navigator.backStack.selectedTab, navigator::select, snackbar) {
                    AppNavDisplay(navigator, router)
                }
            }
        }
        repeat(EVENTS) { index ->
            val event = MonkeyEvent.entries.random(random)
            runCatching {
                perform(event, random)
                composeRule.waitForIdle()
                assertConsistent()
            }.onFailure {
                throw AssertionError("navigation monkey seed $seed failed at event ${index + 1}: $event", it)
            }
        }
    }

    private fun perform(
        event: MonkeyEvent,
        random: Random,
    ) {
        when (event) {
            MonkeyEvent.TAB -> composeRule.onNodeWithTag(tabTag(TopLevelTab.entries.random(random))).performClick()
            MonkeyEvent.BACK -> if (navigator.backStack.canPop) Espresso.pressBack()
            MonkeyEvent.LINK -> onMain { navigator.navigate(DeepLinks.parse(LINKS.random(random))) }
            MonkeyEvent.SCREEN -> onMain { navigator.navigate(DESTINATIONS.random(random)) }
            MonkeyEvent.CALLBACK -> onMain { callback(random) }
        }
    }

    private fun callback(random: Random) {
        val day = Jdn(TODAY_JDN + random.nextLong(-400, 400))
        when (random.nextInt(CALLBACKS)) {
            0 -> router.calendar().onOpenEventEditor(day)
            1 -> router.year().onOpenMonth(day)
            2 -> router.search().onOpenDay(day)
            3 -> router.search().onOpenTool(ToolEntry.entries.random(random))
            else -> router.settings().onOpen(SettingsDestination.entries.random(random))
        }
    }

    private fun onMain(action: () -> Unit) = composeRule.runOnUiThread(action)

    private fun assertConsistent() {
        val stack = navigator.backStack
        assertEquals(AppDestination.Calendar, stack.entries.first())
        val shown =
            composeRule
                .onAllNodes(IS_SCREEN, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }
        assertEquals(listOf(destinationTag(stack.top)), shown)
    }

    private enum class MonkeyEvent { TAB, BACK, LINK, SCREEN, CALLBACK }

    private object NoExternalActions : ExternalActions {
        override fun openUrl(url: String) = Unit

        override fun openDeviceEvent(id: Long) = Unit
    }

    private companion object {
        const val EVENTS = 500
        const val DEFAULT_SEED = 703L
        const val SEED_PROPERTY = "taqvim.monkey.seed"
        const val CALLBACKS = 5
        const val TODAY_JDN = 2_461_297L

        val IS_SCREEN =
            SemanticsMatcher("is a destination screen") {
                it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("destination:") == true
            }

        val LINKS =
            listOf(
                "taqvim://calendar",
                "taqvim://day/1405-01-01",
                "taqvim://times",
                "taqvim://astronomy",
                "taqvim://map",
                "taqvim://timeline",
                "taqvim://event/new",
                "taqvim://search?q=nowruz",
                "taqvim://convert?date=1405-01-01",
                "taqvim://settings/about",
                "taqvim://settings/backup",
                "taqvim://not-a-screen",
            )

        val DESTINATIONS =
            listOf(
                AppDestination.Times,
                AppDestination.Tools,
                AppDestination.More,
                AppDestination.Year,
                AppDestination.Agenda,
                AppDestination.Astronomy,
                AppDestination.WorldMap,
                AppDestination.MapPick,
                AppDestination.Compass,
                AppDestination.Level,
                AppDestination.Search,
                AppDestination.LocationSettings,
                AppDestination.AthanSettings,
                AppDestination.Subscriptions,
                AppDestination.Backup,
                AppDestination.Privacy,
                AppDestination.About,
                AppDestination.Settings(),
                AppDestination.Timeline(TODAY_JDN),
                AppDestination.EventEditor(day = TODAY_JDN),
                AppDestination.Day(TODAY_JDN + 1),
            )
    }
}
