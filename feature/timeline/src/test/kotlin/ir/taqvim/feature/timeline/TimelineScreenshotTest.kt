/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * T-900 screenshots: the week and the day in Persian (light, RTL) and English (dark, LTR), with overlapping and nested
 * timed events, an all-day holiday, Tehran prayer lines, the now line and (in the day) the box of a new event.
 * Events are synthetic. Recorded to `src/test/screenshots/timeline_<sample>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TimelineScreenshotTest(
    private val sample: String,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureTimeline() {
        val persian = sample.endsWith("fa")
        val environment =
            ScreenshotEnvironment(
                theme = if (persian) ScreenshotTheme.LIGHT else ScreenshotTheme.DARK,
                layoutDirection = if (persian) LayoutDirection.Rtl else LayoutDirection.Ltr,
            )
        val state = TimelineUiState(content(if (persian) PERSIAN_SETTINGS else ENGLISH_SETTINGS))
        composeRule.captureScreenshot("timeline_$sample", environment) {
            TimelineTestTheme(rtl = persian, dark = !persian) { TimelineScreen(state, onAction = {}) }
        }
    }

    private fun content(settings: TimelineSettings): TimelineContent {
        val mode = if (sample.startsWith("day")) TimelineMode.DAY else TimelineMode.WEEK
        val range = TimelineContentBuilder.range(mode, TODAY, settings.weekStart)
        val weekend = requireNotNull(LanguageTable.forCode(settings.languageCode)).weekend
        val days = range.map { day -> TimelineDay(day, day == TODAY, day.weekday() in weekend, events(day)) }
        return TimelineContent(
            now = TimelineNow(TODAY, NOW_MINUTE),
            mode = mode,
            columns = TimelineContentBuilder.columns(range, days) { TimelineContentBuilder.prayerLines(it, TEHRAN) },
            zoom = TimelineGeometry.DEFAULT_ZOOM,
            draft = if (mode == TimelineMode.DAY) TimelineDraft(TODAY, 900, 975) else null,
            calendar = TimelineContentBuilder.primaryCalendar(settings.calendars, settings.islamicVariant).system,
            islamicVariant = settings.islamicVariant,
            languageCode = settings.languageCode,
            isLoaded = true,
        )
    }

    /** Synthetic events that start a little later on each day of the week after today. */
    private fun events(day: Jdn): List<TimelineEvent> {
        val shift = Math.floorMod((day - TODAY).toInt(), DAYS_PER_WEEK) * SHIFT_MINUTES
        val timed =
            listOf(
                timed("team", 600 + shift, 690 + shift, title = "Team meeting"),
                timed("call", 630 + shift, 720 + shift, TimelineEventKind.DEVICE, "Call"),
                timed("note", 645 + shift, 675 + shift, TimelineEventKind.SUBSCRIPTION, "Class"),
                timed("lunch", 780, 840, TimelineEventKind.PERSONAL, "Lunch"),
            ).filter { it.startMinute >= 0 && it.endMinute <= TimelineGeometry.MINUTES_PER_DAY }
        return if (day == TODAY) timed + allDay("holiday", "Holiday", isHoliday = true) else timed
    }

    companion object {
        private const val NOW_MINUTE = 10 * 60 + 20
        private const val DAYS_PER_WEEK = 7
        private const val SHIFT_MINUTES = 20

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun parameters(): List<Array<Any>> = listOf("week_fa", "day_fa", "week_en", "day_en").map { arrayOf<Any>(it) }
    }
}
