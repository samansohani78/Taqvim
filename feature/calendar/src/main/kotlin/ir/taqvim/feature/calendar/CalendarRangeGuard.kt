/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import android.util.Log
import ir.taqvim.core.calendar.CalendarRangeException
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.attempt

/** What a guarded call does with a day outside a calendar's range; the default writes a warning to the log. */
internal fun interface RangeFailureSink {
    fun report(
        what: String,
        failure: CalendarRangeException,
    )
}

/**
 * BUG-1: keeps a day outside what the shown calendars can express from reaching the user as a crash. Only
 * [CalendarRangeException] is caught — every other failure still propagates, so this never hides an ordinary bug — and
 * each catch is reported to a [RangeFailureSink] (the log in the app, a recorder in tests). Screens fall back to
 * [nearestValid], the furthest day towards the wanted one that every shown calendar can still express.
 */
internal object CalendarRangeGuard {
    private const val TAG = "CalendarRange"

    /** Writes the warning to logcat; the only place this guard touches the Android log. */
    val LOG: RangeFailureSink =
        RangeFailureSink { what, failure -> Log.w(TAG, "$what is outside the supported calendar range", failure) }

    /** [block]'s value, or `null` when it asked for a day outside a calendar's range (reported to [sink]). */
    fun <T> orNull(
        what: String,
        sink: RangeFailureSink = LOG,
        block: () -> T,
    ): T? =
        attempt(block).fold(
            onSuccess = { it },
            onFailure = { failure ->
                val outOfRange = failure as? CalendarRangeException ?: throw failure
                sink.report(what, outOfRange)
                null
            },
        )

    /** Whether every shown calendar can express [day]. */
    fun isInRange(
        calendars: CalendarCalendars,
        day: Jdn,
        sink: RangeFailureSink = LOG,
    ): Boolean = orNull("day ${day.value}", sink) { calendars.datesOf(day) } != null

    /**
     * The day closest to [wanted] that every calendar of [calendars] can express, searched between [anchor] (a day
     * that works, normally today) and [wanted]. Returns [wanted] when it is already in range, and [anchor] when no day
     * between them is: a bisection of at most 64 steps, so it stays bounded whatever the distance.
     */
    fun nearestValid(
        calendars: CalendarCalendars,
        wanted: Jdn,
        anchor: Jdn,
        sink: RangeFailureSink = LOG,
    ): Jdn {
        if (isInRange(calendars, wanted, sink)) return wanted
        if (!isInRange(calendars, anchor, sink)) return anchor
        var good = anchor.value
        var bad = wanted.value
        while (if (bad > good) bad - good > 1 else good - bad > 1) {
            val middle = good + (bad - good) / 2
            if (isInRange(calendars, Jdn(middle), sink)) good = middle else bad = middle
        }
        return Jdn(good)
    }
}
