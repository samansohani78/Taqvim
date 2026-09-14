/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Test tag of the month pane (T-806). */
const val CALENDAR_MONTH_PANE_TAG: String = "calendar_month_pane"

/** Test tag of the day-details pane (T-806). */
const val CALENDAR_DETAILS_PANE_TAG: String = "calendar_details_pane"

/** Material window size class breakpoint between compact and medium width, in dp. */
internal const val MEDIUM_WIDTH_DP = 600

/** How the month and the day details share the window (T-806). */
enum class CalendarLayout {
    /** Compact width: the month above the details, scrolling together. */
    STACKED,

    /** Medium and expanded width: the month and the details side by side. */
    TWO_PANE,

    /** A half-opened foldable lying flat (horizontal hinge): the month above the fold, the details below it. */
    TABLETOP,
}

/** The layout for [widthDp] of available width; [isTabletop] when a foldable is in tabletop posture. */
internal fun calendarLayoutFor(
    widthDp: Int,
    isTabletop: Boolean,
): CalendarLayout =
    when {
        isTabletop -> CalendarLayout.TABLETOP
        widthDp >= MEDIUM_WIDTH_DP -> CalendarLayout.TWO_PANE
        else -> CalendarLayout.STACKED
    }

/** Whether the window's foldable posture is tabletop; `false` on devices without a folding feature. */
@Composable
internal fun isTabletopPosture(): Boolean = currentWindowAdaptiveInfoV2().windowPosture.isTabletop

/** The month pane and the details pane arranged for [layout]; each pane receives its modifier. */
@Composable
internal fun CalendarPanes(
    layout: CalendarLayout,
    monthPane: @Composable (Modifier) -> Unit,
    detailsPane: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (layout) {
        CalendarLayout.STACKED -> {
            Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().testTag(CALENDAR_MONTH_PANE_TAG)) { monthPane(Modifier.fillMaxWidth()) }
                detailsPane(Modifier.detailsPane())
            }
        }

        CalendarLayout.TWO_PANE -> {
            Row(modifier.fillMaxSize()) {
                MonthBox(Modifier.weight(1f).fillMaxHeight(), monthPane)
                VerticalDivider()
                DetailsColumn(Modifier.weight(1f).fillMaxHeight(), detailsPane)
            }
        }

        CalendarLayout.TABLETOP -> {
            Column(modifier.fillMaxSize()) {
                MonthBox(Modifier.weight(1f).fillMaxWidth(), monthPane)
                HorizontalDivider()
                DetailsColumn(Modifier.weight(1f).fillMaxWidth(), detailsPane)
            }
        }
    }
}

/** The month pane filling [modifier]; the pane's test tag stays on the box so the month keeps its own tags. */
@Composable
private fun MonthBox(
    modifier: Modifier,
    monthPane: @Composable (Modifier) -> Unit,
) {
    Box(modifier.testTag(CALENDAR_MONTH_PANE_TAG)) { monthPane(Modifier.fillMaxSize()) }
}

/** A details pane that scrolls on its own. */
@Composable
private fun DetailsColumn(
    modifier: Modifier,
    detailsPane: @Composable (Modifier) -> Unit,
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        detailsPane(Modifier.detailsPane())
    }
}

private fun Modifier.detailsPane(): Modifier =
    fillMaxWidth().testTag(CALENDAR_DETAILS_PANE_TAG).padding(horizontal = 16.dp, vertical = 8.dp)
