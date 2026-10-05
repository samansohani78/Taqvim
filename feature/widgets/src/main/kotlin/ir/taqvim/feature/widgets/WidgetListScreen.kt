/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.EmptyState
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.TopBar
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** One widget the user has placed on a home screen (T-1200). */
@Immutable
data class PlacedWidget(
    val appWidgetId: Int,
    val kind: WidgetKind,
)

/** The widget settings screen: every placed Taqvim widget, each opening its own configuration. */
@Immutable
data class WidgetListUiState(
    val loading: Boolean = true,
    val placed: ImmutableList<PlacedWidget> = persistentListOf(),
)

/** What the widget list reports back; [onConfigure] opens the launcher's configuration of that widget. */
data class WidgetListActions(
    val onConfigure: (PlacedWidget) -> Unit = {},
)

/**
 * The widgets a user has placed, so their settings can be reached from inside the app (T-1200).
 *
 * A widget is configured by its `APPWIDGET_CONFIGURE` activity, which the launcher opens when the widget is first
 * placed. Reaching it again depends on the launcher, and several do not offer it at all, so the same screen is listed
 * here per placed widget. Nothing is configurable until a widget exists, hence the empty state rather than a list of
 * widgets the user could add: only the launcher can add one.
 */
@Composable
fun WidgetListScreen(
    state: WidgetListUiState,
    actions: WidgetListActions,
    modifier: Modifier = Modifier,
) {
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(R.string.widget_list_title)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> {
                    val description = stringResource(R.string.widget_list_loading)
                    CircularProgressIndicator(
                        Modifier.align(Alignment.Center).semantics { contentDescription = description },
                    )
                }

                state.placed.isEmpty() -> {
                    EmptyState(
                        title = stringResource(R.string.widget_list_empty_title),
                        message = stringResource(R.string.widget_list_empty_message),
                    )
                }

                else -> {
                    PlacedWidgets(state.placed, actions)
                }
            }
        }
    }
}

@Composable
private fun PlacedWidgets(
    placed: ImmutableList<PlacedWidget>,
    actions: WidgetListActions,
) {
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(placed, key = { it.appWidgetId }) { widget ->
            ListItem(
                headlineContent = { Text(stringResource(widgetName(widget.kind))) },
                supportingContent = { Text(stringResource(widgetDescription(widget.kind))) },
                modifier = Modifier.fillMaxWidth().clickable { actions.onConfigure(widget) },
            )
        }
        item {
            Text(
                stringResource(R.string.widget_list_note),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }
}

@StringRes
internal fun widgetName(kind: WidgetKind): Int = WIDGET_NAMES.getValue(kind)

@StringRes
internal fun widgetDescription(kind: WidgetKind): Int = WIDGET_DESCRIPTIONS.getValue(kind)

private val WIDGET_NAMES: Map<WidgetKind, Int> =
    mapOf(
        WidgetKind.DATE_1X1 to R.string.widget_date_1x1_name,
        WidgetKind.DATE_CLOCK_4X1 to R.string.widget_date_clock_4x1_name,
        WidgetKind.DAY_SUMMARY_2X2 to R.string.widget_day_summary_2x2_name,
        WidgetKind.PRAYER_STRIP_4X2 to R.string.widget_prayer_strip_4x2_name,
        WidgetKind.MONTH_INTERACTIVE to R.string.widget_month_interactive_name,
        WidgetKind.MONTH_BITMAP to R.string.widget_month_bitmap_name,
        WidgetKind.WEEK_STRIP to R.string.widget_week_strip_name,
        WidgetKind.SCHEDULE to R.string.widget_schedule_name,
        WidgetKind.SUN_ARC to R.string.widget_sun_arc_name,
        WidgetKind.MOON to R.string.widget_moon_name,
        WidgetKind.MAP to R.string.widget_map_name,
        WidgetKind.COUNTDOWN to R.string.widget_countdown_name,
    )

private val WIDGET_DESCRIPTIONS: Map<WidgetKind, Int> =
    mapOf(
        WidgetKind.DATE_1X1 to R.string.widget_date_1x1_description,
        WidgetKind.DATE_CLOCK_4X1 to R.string.widget_date_clock_4x1_description,
        WidgetKind.DAY_SUMMARY_2X2 to R.string.widget_day_summary_2x2_description,
        WidgetKind.PRAYER_STRIP_4X2 to R.string.widget_prayer_strip_4x2_description,
        WidgetKind.MONTH_INTERACTIVE to R.string.widget_month_interactive_description,
        WidgetKind.MONTH_BITMAP to R.string.widget_month_bitmap_description,
        WidgetKind.WEEK_STRIP to R.string.widget_week_strip_description,
        WidgetKind.SCHEDULE to R.string.widget_schedule_description,
        WidgetKind.SUN_ARC to R.string.widget_sun_arc_description,
        WidgetKind.MOON to R.string.widget_moon_description,
        WidgetKind.MAP to R.string.widget_map_description,
        WidgetKind.COUNTDOWN to R.string.widget_countdown_description,
    )
