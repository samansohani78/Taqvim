/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.runtime.Immutable
import ir.taqvim.core.model.Jdn

/** Where the calendar screen leads; the app's navigation provides these. */
@Immutable
class CalendarNavigation(
    val onOpenEventEditor: (day: Jdn) -> Unit = {},
    val onOpenEvent: (event: DayEventItem) -> Unit = {},
    val onOpenTimeline: (firstDay: Jdn) -> Unit = {},
    val onMessage: (message: CalendarMessage) -> Unit = {},
    /** Opens a cited primary source, e.g. in the browser. */
    val onOpenUrl: (url: String) -> Unit = {},
    /** Opens the search screen (T-803 toolbar, T-804). */
    val onOpenSearch: () -> Unit = {},
    /** Opens shift work (T-803 menu). */
    val onOpenShiftWork: () -> Unit = {},
    /** Opens the planetary hours of [day] (T-803 menu, astronomy). */
    val onOpenPlanetaryHours: (day: Jdn) -> Unit = {},
)
