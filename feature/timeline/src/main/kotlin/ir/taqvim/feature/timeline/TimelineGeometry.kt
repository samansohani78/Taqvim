/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import ir.taqvim.core.model.Jdn
import kotlin.math.floor

/** Minutes, pixels and zoom of the timeline (T-900). Offsets are measured from midnight at the top of a day column. */
object TimelineGeometry {
    const val MINUTES_PER_DAY: Int = 1_440
    const val MINUTES_PER_HOUR: Int = 60
    const val HOURS_PER_DAY: Int = 24

    /** The drag box moves and grows in steps of this many minutes. */
    const val SNAP_MINUTES: Int = 15

    const val MIN_ZOOM: Float = 0.5f
    const val MAX_ZOOM: Float = 2f
    const val DEFAULT_ZOOM: Float = 1f

    /** Zoom factor of one zoom-in step (buttons, keyboard and accessibility actions). */
    const val ZOOM_STEP: Float = 1.25f

    /** [zoom] limited to [MIN_ZOOM]..[MAX_ZOOM]; not-a-number gives the default. */
    fun clampZoom(zoom: Float): Float = if (zoom.isNaN()) DEFAULT_ZOOM else zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)

    /** The minute at [offset] pixels below midnight when an hour is [hourHeight] pixels tall, within the day. */
    fun minuteAt(
        offset: Float,
        hourHeight: Float,
    ): Int = floor(offset / hourHeight * MINUTES_PER_HOUR).toInt().coerceIn(0, MINUTES_PER_DAY)

    /** The offset in pixels of [minute] below midnight when an hour is [hourHeight] pixels tall. */
    fun offsetOf(
        minute: Int,
        hourHeight: Float,
    ): Float = minute * hourHeight / MINUTES_PER_HOUR

    /** [minute] within the day, rounded down to a [SNAP_MINUTES] step. */
    fun snapDown(minute: Int): Int = minute.coerceIn(0, MINUTES_PER_DAY) / SNAP_MINUTES * SNAP_MINUTES

    /** [minute] within the day, rounded to the nearest [SNAP_MINUTES] step. */
    fun snapNearest(minute: Int): Int = snapDown(minute.coerceIn(0, MINUTES_PER_DAY) + SNAP_MINUTES / 2)
}

/**
 * A new event being drawn on [day] from [startMinute] until [endMinute]: whole [TimelineGeometry.SNAP_MINUTES] steps,
 * at least one step long, within the day.
 */
data class TimelineDraft(
    val day: Jdn,
    val startMinute: Int,
    val endMinute: Int,
) {
    init {
        require(
            startMinute % SNAP == 0 && endMinute % SNAP == 0 && startMinute >= 0 &&
                endMinute >= startMinute + SNAP && endMinute <= TimelineGeometry.MINUTES_PER_DAY,
        ) { "draft needs whole steps within the day (was $startMinute..$endMinute)" }
    }

    val lengthMinutes: Int
        get() = endMinute - startMinute

    /** Moved by [steps] steps (negative = earlier), keeping its length within the day. */
    fun moved(steps: Int): TimelineDraft {
        val start = (startMinute + steps * SNAP).coerceIn(0, TimelineGeometry.MINUTES_PER_DAY - lengthMinutes)
        return copy(startMinute = start, endMinute = start + lengthMinutes)
    }

    /** Its end moved by [steps] steps, keeping at least one step and staying within the day. */
    fun resized(steps: Int): TimelineDraft =
        copy(endMinute = (endMinute + steps * SNAP).coerceIn(startMinute + SNAP, TimelineGeometry.MINUTES_PER_DAY))

    companion object {
        private const val SNAP = TimelineGeometry.SNAP_MINUTES

        /**
         * The box a drag from [fromMinute] to [toMinute] draws on [day], in either direction: from the step containing
         * the earlier minute to the step nearest the later one, at least one step long.
         */
        fun spanning(
            day: Jdn,
            fromMinute: Int,
            toMinute: Int,
        ): TimelineDraft {
            val last = TimelineGeometry.MINUTES_PER_DAY - SNAP
            val start = TimelineGeometry.snapDown(minOf(fromMinute, toMinute)).coerceAtMost(last)
            val end = TimelineGeometry.snapNearest(maxOf(fromMinute, toMinute)).coerceAtLeast(start + SNAP)
            return TimelineDraft(day, start, end)
        }
    }
}
