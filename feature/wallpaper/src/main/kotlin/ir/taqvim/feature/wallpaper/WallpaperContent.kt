/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.wallpaper

import ir.taqvim.core.ui.component.MonthGridModel
import ir.taqvim.core.ui.painter.MoonBitmapModel
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * What the live wallpaper (T-1215) shows, every text already localized: today's [title] and [subtitle] (weekday and
 * other calendars), the current [month] with today marked, tonight's [moon], and a day or night sky ([daylight]).
 * [nextChangeAt] is when any of it changes next (sunrise, sunset or the day change).
 */
data class WallpaperContent(
    val title: String,
    val subtitle: String,
    val month: MonthGridModel,
    val moon: MoonBitmapModel,
    val daylight: Boolean,
    val nextChangeAt: Instant,
)

/** The wallpaper's content at an instant; bound in `:app`. */
fun interface WallpaperContentSource {
    suspend fun load(now: Instant): WallpaperContent
}

/** What the daydream (T-1215) shows under its clock: today's [date] and [otherDates], until [nextChangeAt]. */
data class DreamContent(
    val date: String,
    val otherDates: String,
    val nextChangeAt: Instant,
)

/** The daydream's content at an instant; bound in `:app`. */
fun interface DreamContentSource {
    suspend fun load(now: Instant): DreamContent
}

/**
 * When the wallpaper and the daydream draw again (battery-conscious): only at the next change of their content, never
 * sooner than [MIN_DELAY] and, as a safety net against a missed clock change, at least once every [MAX_DELAY]. When
 * content could not be loaded, they try again after [RETRY].
 */
object WallpaperRedrawPolicy {
    val MIN_DELAY: Duration = 1.minutes
    val MAX_DELAY: Duration = 24.hours
    val RETRY: Duration = 30.minutes

    /** How long to wait at [now] before redrawing content that changes at [nextChangeAt] (`null`: loading failed). */
    fun delay(
        now: Instant,
        nextChangeAt: Instant?,
    ): Duration = ((nextChangeAt ?: (now + RETRY)) - now).coerceIn(MIN_DELAY, MAX_DELAY)

    /** The result of [block], or `null` when it fails; cancellation is never swallowed. */
    suspend fun <T> loadOrNull(block: suspend () -> T): T? {
        val result = runCatching { block() }
        val failure = result.exceptionOrNull()
        if (failure is CancellationException) throw failure
        return result.getOrNull()
    }
}
