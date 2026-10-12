/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

/**
 * Exactly which parts of [WidgetData] a widget of a [WidgetKind] under one normalized [WidgetConfig] reads.
 *
 * Everything the widgets could show was being computed for every widget that asked for anything. A place chosen for
 * prayer times meant two full prayer-time calculations before a Moon widget could draw the Moon; `calendarDates`
 * formatted the day in every calendar the user keeps for widgets that show one date; and a widget declaring
 * [WidgetDependency.EVENTS] assembled today's events even when its configuration had the event list switched off.
 *
 * So the loader asks this first and computes only what is in it. The `when` over [WidgetKind] is exhaustive on
 * purpose: a new widget cannot be added without saying what it reads.
 *
 * @property dayEvents today's event lines ([WidgetData.events]) — the only field that needs the day assembled, and
 *   so the only one that reaches Room and the subscription cache.
 * @property holiday whether today is a day off ([WidgetData.isHoliday]), which comes from the dataset alone.
 * @property calendarDates today in every calendar the user keeps ([WidgetData.calendarDates]).
 * @property nextPrayer the next prayer time ([WidgetData.nextPrayer]).
 * @property prayerTimes the whole day's prayer times ([WidgetData.prayers]).
 * @property dayRange facts for a range of days (the month, week and schedule grids), loaded by their own builder.
 */
data class WidgetNeeds(
    val dayEvents: Boolean = false,
    val holiday: Boolean = false,
    val calendarDates: Boolean = false,
    val nextPrayer: Boolean = false,
    val prayerTimes: Boolean = false,
    val dayRange: Boolean = false,
) {
    /** Whether anything here needs the day assembled from the personal and subscription sources. */
    val assemblesToday: Boolean get() = dayEvents

    /** Whether a prayer-time calculation is needed at all. The strip marks its next time, so it needs one too. */
    val anyPrayerTimes: Boolean get() = nextPrayer || prayerTimes

    companion object {
        /** What [kind] reads under [config], which must already be [WidgetConfig.normalizedFor] that kind. */
        fun of(
            kind: WidgetKind,
            config: WidgetConfig,
        ): WidgetNeeds {
            val holiday = config.shows(WidgetContent.HOLIDAYS)
            val events = config.shows(WidgetContent.EVENTS)
            val next = config.shows(WidgetContent.NEXT_PRAYER)
            return when (kind) {
                // Date lines and, where the user keeps it, holiday colour.
                WidgetKind.DATE_1X1, WidgetKind.DATE_CLOCK_4X1 -> {
                    WidgetNeeds(holiday = holiday)
                }

                // T-1216 reads one field and nothing else: no events, no holiday, no prayer times.
                WidgetKind.THREE_DATES -> {
                    WidgetNeeds(calendarDates = true)
                }

                WidgetKind.DAY_SUMMARY_2X2 -> {
                    WidgetNeeds(dayEvents = events, holiday = holiday, nextPrayer = next)
                }

                // The strip always draws the day's times; the date lines above it are dropped at the smallest size,
                // but the configuration, not the size, is what this is built from.
                WidgetKind.PRAYER_STRIP_4X2 -> {
                    WidgetNeeds(holiday = holiday, prayerTimes = true)
                }

                // The grids carry their own per-day holiday and event facts for the whole range they draw, so
                // today on its own is not read: loading it as well was the same query twice.
                WidgetKind.MONTH_INTERACTIVE,
                WidgetKind.MONTH_BITMAP,
                WidgetKind.WEEK_STRIP,
                WidgetKind.SCHEDULE,
                -> {
                    WidgetNeeds(dayRange = true)
                }

                WidgetKind.SUN_ARC -> {
                    WidgetNeeds(nextPrayer = next)
                }

                // The Moon, the map and the countdown are built from their own parts alone.
                WidgetKind.MOON, WidgetKind.MAP, WidgetKind.COUNTDOWN -> {
                    WidgetNeeds()
                }
            }
        }
    }
}
