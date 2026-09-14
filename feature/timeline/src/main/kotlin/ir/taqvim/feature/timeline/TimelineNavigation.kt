/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import androidx.compose.runtime.Immutable
import ir.taqvim.core.model.Jdn

/** Where the timeline leads; the app's navigation provides these. */
@Immutable
class TimelineNavigation(
    /** Opens the event editor for a new event on a day from a start minute until an end minute. */
    val onCreateEvent: (day: Jdn, startMinute: Int, endMinute: Int) -> Unit = { _, _, _ -> },
    /** Opens an existing event. */
    val onOpenEvent: (id: String, kind: TimelineEventKind) -> Unit = { _, _ -> },
)
