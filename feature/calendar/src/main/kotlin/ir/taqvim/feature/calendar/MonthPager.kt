/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ir.taqvim.core.ui.component.MonthGrid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Test tag of the month pager; also its resource id in macrobenchmarks. */
const val MONTH_PAGER_TAG: String = "month_pager"

/** Months the pager reaches on each side of today's month (a century each way). */
internal const val MONTH_PAGES_EACH_WAY = 1_200
private const val PAGE_COUNT = MONTH_PAGES_EACH_WAY * 2 + 1
private const val SELECTION_FADE_MILLIS = 150

/**
 * The month pager (T-801): one page per primary-calendar month around today's month. The neighbouring pages are
 * composed ahead (±1) and every page model is built on [Dispatchers.Default]; a settled swipe reports
 * [CalendarAction.ShowMonth], and a month shown by the view model (today, a search result) scrolls the pager.
 */
@Composable
internal fun MonthPager(
    content: CalendarContent,
    builder: MonthPageBuilder,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetPage = (content.monthOffset + MONTH_PAGES_EACH_WAY).coerceIn(0, PAGE_COUNT - 1)
    val pagerState = rememberPagerState(initialPage = targetPage) { PAGE_COUNT }
    val shownOffset by rememberUpdatedState(content.monthOffset)
    val latestOnAction by rememberUpdatedState(onAction)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val offset = page - MONTH_PAGES_EACH_WAY
            if (offset != shownOffset) latestOnAction(CalendarAction.ShowMonth(offset))
        }
    }
    LaunchedEffect(targetPage) {
        if (pagerState.settledPage != targetPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(targetPage)
        }
    }
    HorizontalPager(
        state = pagerState,
        modifier = modifier.testTag(MONTH_PAGER_TAG),
        beyondViewportPageCount = 1,
        key = { it },
    ) { page ->
        MonthPageSlot(page - MONTH_PAGES_EACH_WAY, content, builder, onAction)
    }
}

/** A pager page: empty until its model is built, then kept while a newer model (selection, events) is built. */
@Composable
private fun MonthPageSlot(
    offset: Int,
    content: CalendarContent,
    builder: MonthPageBuilder,
    onAction: (CalendarAction) -> Unit,
) {
    val events = content.months.firstOrNull { it.offset == offset }?.days
    val today = content.today
    val selected = content.selectedDay
    val page by produceState<MonthPage?>(null, builder, offset, today, selected, events) {
        value = withContext(Dispatchers.Default) { builder.build(offset, today, selected, events) }
    }
    val built = page
    if (built == null) {
        Box(Modifier.fillMaxSize())
    } else {
        MonthPageView(built, onAction, Modifier.fillMaxSize())
    }
}

/**
 * The grid of one month page. A change of the selected day cross-fades the grid; other updates (loaded events) are
 * applied in place. Taps select a day, long presses create an event and week numbers open the timeline.
 */
@Composable
internal fun MonthPageView(
    page: MonthPage,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = page.selectedIndex,
        modifier = modifier,
        animationSpec = tween(SELECTION_FADE_MILLIS),
        label = "month selection",
    ) { selectedIndex ->
        // A fading-out grid keeps the page it was shown with; the current one follows every update of [page].
        val entered = remember { page }
        val shown = if (selectedIndex == page.selectedIndex) page else entered
        val onWeekClick: ((Int) -> Unit)? =
            if (shown.grid.weekNumbers == null) {
                null
            } else {
                { row -> onAction(CalendarAction.OpenWeek(shown.days[row * MonthLayout.DAYS_PER_WEEK])) }
            }
        MonthGrid(
            model = shown.grid,
            onDayClick = { onAction(CalendarAction.SelectDay(shown.days[it])) },
            onDayLongClick = { onAction(CalendarAction.CreateEvent(shown.days[it])) },
            onWeekClick = onWeekClick,
        )
    }
}
