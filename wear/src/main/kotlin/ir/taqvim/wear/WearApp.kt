/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

/** The watch app's only activity (T-1600). */
class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = wearGraph()
        // Test tags readable as resource ids by the watch smoke tests on the managed device (T-1600).
        setContent { WearApp(graph, Modifier.semantics { testTagsAsResourceId = true }) }
    }
}

/** The test tag of the screen shown for [route]; the smoke tests read it as a resource id. */
internal fun screenTag(route: String): String = "wear:" + route.replace('/', ':')

/** The test tag of the button that opens [route]. */
internal fun openTag(route: String): String = "wear:open:" + route.replace('/', ':')

/** Screens of the watch app. */
internal object WearRoutes {
    const val TODAY = "today"
    const val MONTH = "month"
    const val CONVERTER = "converter"
    const val SETTINGS = "settings"
    const val LANGUAGE = "settings/language"
    const val CALENDAR = "settings/calendar"
    const val METHOD = "settings/method"
    const val CITY = "settings/city"
}

/** A destination tagged with its route, so the watch smoke tests can wait for the screen (T-1600). */
internal fun NavGraphBuilder.screen(
    route: String,
    content: @Composable () -> Unit,
) {
    composable(route) { Box(Modifier.fillMaxSize().testTag(screenTag(route))) { content() } }
}

/** The watch app: today first, with month, converter and settings one tap away; swipe right goes back. */
@Composable
fun WearApp(
    graph: WearGraph,
    modifier: Modifier = Modifier,
) {
    val navController = rememberSwipeDismissableNavController()
    MaterialTheme {
        AppScaffold(modifier = modifier, timeText = { TimeText() }) {
            SwipeDismissableNavHost(navController = navController, startDestination = WearRoutes.TODAY) {
                screen(WearRoutes.TODAY) {
                    val model = viewModel { TodayViewModel(graph) }
                    val state by model.uiState.collectAsStateWithLifecycle()
                    TodayScreen(state, onOpen = { navController.navigate(it) })
                }
                screen(WearRoutes.MONTH) {
                    val model = viewModel { MonthViewModel(graph) }
                    val state by model.uiState.collectAsStateWithLifecycle()
                    MonthScreen(state, onShow = model::show)
                }
                screen(WearRoutes.CONVERTER) {
                    val model = viewModel { ConverterViewModel(graph) }
                    val state by model.uiState.collectAsStateWithLifecycle()
                    ConverterScreen(state, onStep = model::step, onSwitch = model::switchCalendar)
                }
                settingsDestinations(
                    graph,
                    onOpen = { navController.navigate(it) },
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}
