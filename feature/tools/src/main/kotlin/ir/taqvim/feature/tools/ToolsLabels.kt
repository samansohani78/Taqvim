/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.annotation.StringRes
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.workdays.HalfDayPolicy

/** String resources naming the enumerations the workday profile editor offers (F-07). */
internal object ToolsLabels {
    @StringRes
    fun weekday(day: Weekday): Int = WEEKDAYS.getValue(day)

    @StringRes
    fun source(source: EventSource): Int = SOURCES.getValue(source)

    @StringRes
    fun halfDay(policy: HalfDayPolicy): Int =
        when (policy) {
            HalfDayPolicy.HALF -> R.string.tools_profiles_half_day_half
            HalfDayPolicy.FULL_WORKDAY -> R.string.tools_profiles_half_day_full
        }

    private val WEEKDAYS: Map<Weekday, Int> =
        mapOf(
            Weekday.MONDAY to R.string.tools_weekday_monday,
            Weekday.TUESDAY to R.string.tools_weekday_tuesday,
            Weekday.WEDNESDAY to R.string.tools_weekday_wednesday,
            Weekday.THURSDAY to R.string.tools_weekday_thursday,
            Weekday.FRIDAY to R.string.tools_weekday_friday,
            Weekday.SATURDAY to R.string.tools_weekday_saturday,
            Weekday.SUNDAY to R.string.tools_weekday_sunday,
        )

    private val SOURCES: Map<EventSource, Int> =
        mapOf(
            EventSource.IRAN_OFFICIAL to R.string.tools_source_iran,
            EventSource.AFGHANISTAN_OFFICIAL to R.string.tools_source_afghanistan,
            EventSource.NEPAL_OFFICIAL to R.string.tools_source_nepal,
            EventSource.INTERNATIONAL to R.string.tools_source_international,
            EventSource.ANCIENT_IRAN to R.string.tools_source_ancient_iran,
            EventSource.JEWISH to R.string.tools_source_jewish,
            EventSource.CHRISTIAN to R.string.tools_source_christian,
            EventSource.USER to R.string.tools_source_personal,
        )
}
