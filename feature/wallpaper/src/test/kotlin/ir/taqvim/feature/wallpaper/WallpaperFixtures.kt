/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.wallpaper

import ir.taqvim.core.ui.component.DayCellModel
import ir.taqvim.core.ui.component.MonthGridModel
import ir.taqvim.core.ui.painter.MoonBitmapModel
import kotlin.time.Duration
import kotlin.time.Instant

/** Content and a recording ticker for the T-1215 wallpaper and daydream tests. */
internal object WallpaperFixtures {
    val NOW: Instant = Instant.parse("2026-09-13T08:30:00Z")
    val SUNSET: Instant = Instant.parse("2026-09-13T15:20:00Z")

    fun content(daylight: Boolean = true): WallpaperContent =
        WallpaperContent(
            title = "22 Shahrivar 1405",
            subtitle = "Sunday · 13 September 2026",
            month =
                MonthGridModel(
                    weekdayLabels = listOf("S", "S", "M", "T", "W", "T", "F"),
                    cells =
                        (1..35).map {
                            DayCellModel(
                                dayLabel = "$it",
                                contentDescription = "Day $it",
                                isToday =
                                    it == 22,
                            )
                        },
                ),
            moon = MoonBitmapModel(illuminatedFraction = 0.02f, waxing = true),
            daylight = daylight,
            nextChangeAt = SUNSET,
        )
}

/** A [WallpaperTicker] that records schedules and runs the pending action on demand. */
internal class RecordingTicker : WallpaperTicker {
    val delays = mutableListOf<Duration>()
    var cancels = 0
    private var pending: (() -> Unit)? = null

    val hasPending: Boolean get() = pending != null

    override fun schedule(
        delay: Duration,
        action: () -> Unit,
    ) {
        delays += delay
        pending = action
    }

    override fun cancel() {
        cancels++
        pending = null
    }

    fun fire() {
        val action = requireNotNull(pending) { "nothing scheduled" }
        pending = null
        action()
    }
}
