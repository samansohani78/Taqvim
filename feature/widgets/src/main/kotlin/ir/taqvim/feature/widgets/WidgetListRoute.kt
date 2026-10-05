/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

/**
 * The widget settings screen bound to its [WidgetListViewModel] (T-1200).
 *
 * Configuring a widget leaves the app for the widget's own `APPWIDGET_CONFIGURE` activity, so the list is re-read
 * when the screen comes back: a widget may have been removed from the home screen meanwhile.
 */
@Composable
fun WidgetListRoute(
    modifier: Modifier = Modifier,
    viewModel: WidgetListViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    val actions =
        remember(context) {
            WidgetListActions(
                onConfigure = { widget ->
                    val intent =
                        Intent(context, WidgetConfigActivity::class.java)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget.appWidgetId)
                    runCatching { context.startActivity(intent) }
                },
            )
        }
    WidgetListScreen(state, actions, modifier)
}
