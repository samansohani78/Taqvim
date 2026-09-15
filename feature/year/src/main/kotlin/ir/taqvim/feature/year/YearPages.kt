/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Test tag of the calendar pager; also its resource id in macrobenchmarks. */
const val YEAR_PAGER_TAG: String = "year_pager"

/** Test tag of the grid of mini months. */
const val YEAR_GRID_TAG: String = "year_grid"

/** Test tag of the year selection grid. */
const val YEAR_PICKER_TAG: String = "year_picker"

private val GRID_PADDING = 8.dp
private val GRID_SPACING = 8.dp
private val YEAR_CELL_PADDING = 12.dp
private const val PICKER_COLUMNS = 4

/** Rows of years shown above the selected year when the year selection opens. */
private const val PICKER_LEAD_ROWS = 3

/** Accumulated pinch scale that changes the zoom by one level. */
private const val ZOOM_STEP = 1.25f

/**
 * The calendar pager (T-805): one page per available calendar, each showing its year that contains the anchor day. A
 * settled swipe reports [YearAction.SelectCalendar]; a calendar selected elsewhere scrolls the pager. Page models are
 * built on [Dispatchers.Default].
 */
@Composable
internal fun CalendarPages(
    content: YearContent,
    builder: YearPageBuilder,
    onAction: (YearAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = content.calendarIndex) { content.calendars.size }
    val shownIndex by rememberUpdatedState(content.calendarIndex)
    val latestOnAction by rememberUpdatedState(onAction)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page != shownIndex) latestOnAction(YearAction.SelectCalendar(page))
        }
    }
    LaunchedEffect(content.calendarIndex) {
        if (pagerState.settledPage != content.calendarIndex && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(content.calendarIndex)
        }
    }
    HorizontalPager(
        state = pagerState,
        modifier = modifier.testTag(YEAR_PAGER_TAG),
        beyondViewportPageCount = 1,
        key = { it },
    ) { page ->
        if (content.isPickingYear && page == content.calendarIndex) {
            YearPicker(content.year, builder.years(page), builder::number, onAction, Modifier.fillMaxSize())
        } else {
            YearPageSlot(page, content, builder, onAction)
        }
    }
}

/** A calendar page: empty until its model is built, then kept while a newer model (loaded holidays) is built. */
@Composable
private fun YearPageSlot(
    index: Int,
    content: YearContent,
    builder: YearPageBuilder,
    onAction: (YearAction) -> Unit,
) {
    val year = remember(builder, index, content.anchorDay) { builder.yearOf(index, content.anchorDay) }
    val days = content.days.takeIf { index == content.calendarIndex }
    val today = content.today
    val page by produceState<YearPage?>(null, builder, index, year, today, days) {
        value = withContext(Dispatchers.Default) { builder.build(index, year, today, days) }
    }
    val built = page
    if (built == null) {
        Box(Modifier.fillMaxSize())
    } else {
        YearPageView(built, content.columns, onAction, Modifier.fillMaxSize())
    }
}

/**
 * The mini months of one year page, [columns] per row. Pinching zooms (fewer or more months per row), and the grid
 * offers the same as accessibility actions; tapping a month opens it.
 */
@Composable
internal fun YearPageView(
    page: YearPage,
    columns: Int,
    onAction: (YearAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnAction by rememberUpdatedState(onAction)
    val zoomIn = stringResource(R.string.year_zoom_in)
    val zoomOut = stringResource(R.string.year_zoom_out)
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier =
            modifier
                .testTag(YEAR_GRID_TAG)
                .pinchToZoom(
                    onZoomIn = { latestOnAction(YearAction.ZoomIn) },
                    onZoomOut = { latestOnAction(YearAction.ZoomOut) },
                ).semantics {
                    customActions =
                        listOf(
                            CustomAccessibilityAction(zoomIn) { true.also { latestOnAction(YearAction.ZoomIn) } },
                            CustomAccessibilityAction(zoomOut) { true.also { latestOnAction(YearAction.ZoomOut) } },
                        )
                },
        contentPadding = PaddingValues(GRID_PADDING),
        horizontalArrangement = Arrangement.spacedBy(GRID_SPACING),
        verticalArrangement = Arrangement.spacedBy(GRID_SPACING),
    ) {
        items(page.months, key = { it.firstDay.value }) { month ->
            MiniMonthView(month, page.weekdayLabels, onOpen = { onAction(YearAction.OpenMonth(month.firstDay)) })
        }
    }
}

/**
 * Year selection: the offered [years] around the shown one ([YearCalendars.pickerYears]), the shown one selected;
 * picking a year shows it, so the list re-centres there and every offered year can be reached.
 */
@Composable
internal fun YearPicker(
    selectedYear: Int,
    years: IntRange,
    label: (Int) -> String,
    onAction: (YearAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listed = YearCalendars.pickerYears(selectedYear, years, PICKER_COLUMNS)
    val first = listed.first
    val initialIndex = (selectedYear - first - PICKER_LEAD_ROWS * PICKER_COLUMNS).coerceAtLeast(0)
    val state = remember(listed) { LazyGridState(firstVisibleItemIndex = initialIndex) }
    val colors = MaterialTheme.colorScheme
    LazyVerticalGrid(
        columns = GridCells.Fixed(PICKER_COLUMNS),
        modifier = modifier.testTag(YEAR_PICKER_TAG),
        state = state,
        contentPadding = PaddingValues(GRID_PADDING),
    ) {
        items(listed.last - first + 1, key = { first + it }) { offset ->
            val year = first + offset
            val selected = year == selectedYear
            Box(
                Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (selected) colors.primaryContainer else Color.Transparent)
                    .selectable(selected, role = Role.Button, onClick = { onAction(YearAction.ShowYear(year)) })
                    .padding(vertical = YEAR_CELL_PADDING),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(year),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                )
            }
        }
    }
}

/**
 * Reports [onZoomIn] or [onZoomOut] for every [ZOOM_STEP] of a two-finger pinch. One-finger gestures are left to the
 * pager and the grid's scrolling.
 */
private fun Modifier.pinchToZoom(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var scale = 1f
            do {
                val event = awaitPointerEvent()
                val pinching = event.changes.count { it.pressed } > 1
                if (pinching) {
                    scale *= event.calculateZoom()
                    if (scale >= ZOOM_STEP || scale <= 1f / ZOOM_STEP) {
                        if (scale > 1f) onZoomIn() else onZoomOut()
                        scale = 1f
                    }
                    event.changes.filter { it.positionChanged() }.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }
