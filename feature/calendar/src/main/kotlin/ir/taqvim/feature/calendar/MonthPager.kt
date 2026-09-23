/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import ir.taqvim.core.calendar.CalendarLimits
import ir.taqvim.core.ui.component.MonthDayList
import ir.taqvim.core.ui.component.MonthDisplayMode
import ir.taqvim.core.ui.component.MonthGrid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext

/** Test tag of the month pager; also its resource id in macrobenchmarks. */
const val MONTH_PAGER_TAG: String = "month_pager"

/**
 * Pages of the month pager: one per month within [CalendarLimits.MAX_MONTH_OFFSET] of today's month either way, the
 * most a pager's `Int` page count holds (about 89 million years each way).
 */
internal object MonthPages {
    /** The page of today's month. */
    const val TODAY_PAGE: Int = CalendarLimits.MAX_MONTH_OFFSET

    /** Every page, [Int.MAX_VALUE]. */
    const val COUNT: Int = TODAY_PAGE * 2 + 1

    /** The page showing the month [offset] months from today's month, limited to the pages. */
    fun pageOf(offset: Int): Int = CalendarLimits.clampMonthOffset(offset) + TODAY_PAGE

    /** Months from today's month to the month on [page], limited to the pages. */
    fun offsetOf(page: Int): Int = page.coerceIn(0, COUNT - 1) - TODAY_PAGE
}

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
    val targetPage = MonthPages.pageOf(content.monthOffset)
    val pagerState = rememberPagerState(initialPage = targetPage) { MonthPages.COUNT }
    val shownOffset by rememberUpdatedState(content.monthOffset)
    val latestOnAction by rememberUpdatedState(onAction)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val offset = MonthPages.offsetOf(page)
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
        MonthPageSlot(MonthPages.offsetOf(page), content, builder, onAction)
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
    // The selection is not a key: it only marks a cell, so it is applied to the built page instead of building the
    // page again on every tap, which converted all 42 days of three composed pages into every calendar (BUG-2).
    val latestSelected by rememberUpdatedState(selected)
    val base by produceState<MonthPage?>(null, builder, offset, today, events) {
        // Each run has its own cookie and closes its section however it ends, a restart cancelling it included.
        val cookie = System.identityHashCode(this)
        CalendarTrace.beginAsync(CalendarTrace.PAGE_PENDING, cookie)
        coroutineContext.job.invokeOnCompletion { CalendarTrace.endAsync(CalendarTrace.PAGE_PENDING, cookie) }
        value =
            withContext(Dispatchers.Default) {
                CalendarTrace.section(CalendarTrace.PAGE_BUILD) {
                    CalendarRangeGuard.orNull("month page $offset") {
                        builder.build(offset, today, latestSelected, events)
                    }
                }
            }
    }
    val built = remember(base, selected) { base?.withSelection(selected) }
    if (built == null) {
        Box(Modifier.fillMaxSize())
    } else {
        MonthPageView(built, onAction, Modifier.fillMaxSize())
    }
}

/**
 * The month page: every update (a new selection, loaded events) is applied in place, so only the cells whose model
 * changed recompose (`MonthPageRecompositionTest`). The grid used to cross-fade on a selection, which composed all
 * 42 cells a second time on every tap and kept an animation running for 150 ms (BUG-2). Taps select a day, long
 * presses create an event and week numbers open the timeline.
 *
 * Above [MonthDisplayMode]'s font-scale cap the page shows [MonthDayList] instead of [MonthGrid] (R10, T-1701): a
 * month grid gives every cell the same fixed size, which cannot fit a system font scale much larger than the
 * default, so it would either clip the day's text or force it back down to a size the user did not ask for.
 */
@Composable
internal fun MonthPageView(
    page: MonthPage,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The callbacks must keep their identity across updates of [page]: a new lambda per update would make Compose
    // recompose every one of the 42 day cells instead of the two whose model changed (BUG-2).
    val shown by rememberUpdatedState(page)
    val latestOnAction by rememberUpdatedState(onAction)
    val onDayClick = remember { { index: Int -> latestOnAction(CalendarAction.SelectDay(shown.days[index])) } }
    val onDayLongClick = remember { { index: Int -> latestOnAction(CalendarAction.CreateEvent(shown.days[index])) } }
    val onWeekClick =
        remember {
            { row: Int ->
                latestOnAction(CalendarAction.OpenWeek(shown.days[row * MonthLayout.DAYS_PER_WEEK]))
            }
        }
    val hasWeekClick = onWeekClick.takeIf { page.grid.weekNumbers != null }
    when (MonthDisplayMode.forFontScale(LocalDensity.current.fontScale)) {
        MonthDisplayMode.GRID -> {
            MonthGrid(
                model = page.grid,
                onDayClick = onDayClick,
                onDayLongClick = onDayLongClick,
                onWeekClick = hasWeekClick,
                modifier = modifier,
            )
        }

        MonthDisplayMode.LIST -> {
            MonthDayList(
                model = page.grid,
                onDayClick = onDayClick,
                onDayLongClick = onDayLongClick,
                onWeekClick = hasWeekClick,
                modifier = modifier,
            )
        }
    }
}
