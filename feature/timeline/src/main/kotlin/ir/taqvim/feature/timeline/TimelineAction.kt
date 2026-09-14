/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import ir.taqvim.core.model.Jdn

/** User actions on the timeline (T-900). */
sealed interface TimelineAction {
    /** Actions that change the shown days. */
    sealed interface Navigation : TimelineAction

    /** Actions on the new event being drawn. */
    sealed interface Draft : TimelineAction

    data class ShowMode(
        val mode: TimelineMode,
    ) : Navigation

    /** The next day or week. */
    data object ShowNext : Navigation

    /** The previous day or week. */
    data object ShowPrevious : Navigation

    /** Back to today; the timeline then follows today. */
    data object GoToToday : Navigation

    /** The week containing [day] (the week-number entry point of the month pager). */
    data class ShowWeekOf(
        val day: Jdn,
    ) : Navigation

    /** Multiplies the hour height by [factor], within the zoom limits. */
    data class ZoomBy(
        val factor: Float,
    ) : TimelineAction

    /** Draws the box a drag from [fromMinute] to [toMinute] on [day] covers ([TimelineDraft.spanning]). */
    data class SetDraft(
        val day: Jdn,
        val fromMinute: Int,
        val toMinute: Int,
    ) : Draft

    /** Moves the box by [steps] 15-minute steps (negative = earlier). */
    data class MoveDraft(
        val steps: Int,
    ) : Draft

    /** Moves the end of the box by [steps] 15-minute steps. */
    data class ResizeDraft(
        val steps: Int,
    ) : Draft

    /** Creates an event with the times of the box. */
    data object ConfirmDraft : Draft

    data object CancelDraft : Draft

    data class OpenEvent(
        val id: String,
        val kind: TimelineEventKind,
    ) : TimelineAction
}

/** One-shot effects of the timeline. */
sealed interface TimelineEffect {
    /** Open the event editor for a new event on [day] from [startMinute] until [endMinute]. */
    data class CreateEvent(
        val day: Jdn,
        val startMinute: Int,
        val endMinute: Int,
    ) : TimelineEffect

    data class NavigateToEvent(
        val id: String,
        val kind: TimelineEventKind,
    ) : TimelineEffect
}
