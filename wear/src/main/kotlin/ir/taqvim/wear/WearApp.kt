/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
        setContent { WearApp(graph) }
    }
}

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

/** The watch app: today first, with month, converter and settings one tap away; swipe right goes back. */
@Composable
fun WearApp(graph: WearGraph) {
    val navController = rememberSwipeDismissableNavController()
    MaterialTheme {
        AppScaffold(timeText = { TimeText() }) {
            SwipeDismissableNavHost(navController = navController, startDestination = WearRoutes.TODAY) {
                composable(WearRoutes.TODAY) {
                    val model = viewModel { TodayViewModel(graph) }
                    val state by model.uiState.collectAsStateWithLifecycle()
                    TodayScreen(state, onOpen = { navController.navigate(it) })
                }
                composable(WearRoutes.MONTH) {
                    val model = viewModel { MonthViewModel(graph) }
                    val state by model.uiState.collectAsStateWithLifecycle()
                    MonthScreen(state, onShow = model::show)
                }
                composable(WearRoutes.CONVERTER) {
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
