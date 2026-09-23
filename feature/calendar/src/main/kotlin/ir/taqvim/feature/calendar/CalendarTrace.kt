/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import android.annotation.SuppressLint
import android.os.Build
import android.os.Trace

/**
 * Named sections of the calendar screen's work in a system trace (Perfetto, macrobenchmarks), so a slow month page
 * can be attributed to the step that made it slow (BUG-2). A section costs a flag check while tracing is off.
 */
internal object CalendarTrace {
    /** Building a month page's model, off the main thread. */
    const val PAGE_BUILD = "MonthPage:build"

    /** From a page being composed until its model is ready; the page is blank meanwhile (async section). */
    const val PAGE_PENDING = "MonthPage:pending"

    /**
     * Runs [block] inside the trace section [name]; a failure still closes the section and is rethrown. The lint check
     * expects `try`/`finally`, which this project replaces with `runCatching` (PLAN §0.2); the section is closed on
     * every path because `runCatching` returns whatever [block] does.
     */
    @SuppressLint("UnclosedTrace")
    inline fun <T> section(
        name: String,
        block: () -> T,
    ): T {
        Trace.beginSection(name)
        val result = runCatching(block)
        Trace.endSection()
        return result.getOrThrow()
    }

    /** Opens the async section [name] identified by [cookie] (API 29+; earlier systems have none); [endAsync] closes it. */
    fun beginAsync(
        name: String,
        cookie: Int,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Trace.beginAsyncSection(name, cookie)
    }

    /** Closes the async section [name] opened with [cookie]. */
    fun endAsync(
        name: String,
        cookie: Int,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Trace.endAsyncSection(name, cookie)
    }
}
