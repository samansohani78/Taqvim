/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import android.content.res.Resources
import android.icu.text.DateFormatSymbols
import android.icu.util.ULocale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday

/** Texts of the timeline in the app language, from string resources, CLDR names and the day's calendar (T-900). */
internal class TimelineLabels(
    private val resources: Resources,
    private val language: LanguageSpec,
    private val calendar: CalendarArithmetic,
    /** Abbreviated weekday names in ISO order (Monday first). */
    private val weekdayNames: List<String>,
) {
    fun number(value: Int): String = Numerals.localizeDigits(value.toString(), language.numerals)

    /** `HH:MM` of [minute] of the day (24:00 at the end of the day), in the language's digits. */
    fun time(minute: Int): String {
        val clamped = minute.coerceIn(0, TimelineGeometry.MINUTES_PER_DAY)
        val hours = (clamped / TimelineGeometry.MINUTES_PER_HOUR).toString().padStart(2, '0')
        val minutes = (clamped % TimelineGeometry.MINUTES_PER_HOUR).toString().padStart(2, '0')
        return Numerals.localizeDigits("$hours:$minutes", language.numerals)
    }

    fun hour(hour: Int): String = time(hour * TimelineGeometry.MINUTES_PER_HOUR)

    /** Short weekday and day of the month, e.g. "Sat 15". */
    fun dayHeader(day: Jdn): String =
        resources.getString(
            R.string.timeline_day_header,
            weekdayNames[day.weekday().ordinal],
            number(calendar.fromJdn(day).day),
        )

    /** The full date of [day] for screen readers and the day title. */
    fun dayDescription(day: Jdn): String =
        DateFormatter.format(calendar.fromJdn(day), day.weekday(), language, DateStyle.LONG)

    /** The title: the full date in day mode; the month (or both months) of the week in week mode. */
    fun title(content: TimelineContent): String {
        val first = content.columns.first().jdn
        if (content.mode == TimelineMode.DAY) return dayDescription(first)
        val firstMonth = monthTitle(first)
        val lastMonth = monthTitle(content.columns.last().jdn)
        return if (firstMonth == lastMonth) {
            firstMonth
        } else {
            resources.getString(R.string.timeline_range, firstMonth, lastMonth)
        }
    }

    fun event(event: TimelineEvent): String =
        if (event.isAllDay) {
            resources.getString(R.string.timeline_event_all_day, event.title)
        } else {
            val start = time(event.startMinute)
            resources.getString(R.string.timeline_event_timed, event.title, start, time(event.endMinute))
        }

    fun draft(draft: TimelineDraft): String =
        resources.getString(R.string.timeline_draft, time(draft.startMinute), time(draft.endMinute))

    /** The day column for screen readers: its date, prayer times and the current time on today. */
    fun column(
        column: TimelineColumn,
        now: TimelineNow,
    ): String {
        val parts = mutableListOf(dayDescription(column.jdn))
        if (column.prayerLines.isNotEmpty()) {
            val lines =
                column.prayerLines.joinToString(resources.getString(R.string.timeline_separator)) { line ->
                    resources.getString(R.string.timeline_prayer_line, prayer(line.kind), time(line.minute))
                }
            parts += resources.getString(R.string.timeline_prayer_lines, lines)
        }
        if (now.day == column.jdn) parts += resources.getString(R.string.timeline_now, time(now.minute))
        return parts.joinToString(resources.getString(R.string.timeline_separator))
    }

    fun prayer(kind: PrayerLineKind): String =
        resources.getString(
            when (kind) {
                PrayerLineKind.FAJR -> R.string.timeline_prayer_fajr
                PrayerLineKind.SUNRISE -> R.string.timeline_prayer_sunrise
                PrayerLineKind.DHUHR -> R.string.timeline_prayer_dhuhr
                PrayerLineKind.ASR -> R.string.timeline_prayer_asr
                PrayerLineKind.SUNSET -> R.string.timeline_prayer_sunset
                PrayerLineKind.MAGHRIB -> R.string.timeline_prayer_maghrib
                PrayerLineKind.ISHA -> R.string.timeline_prayer_isha
            },
        )

    private fun monthTitle(day: Jdn): String {
        val date = calendar.fromJdn(day)
        val name = language.monthNames.forSystem(calendar.system)?.getOrNull(date.month - 1) ?: number(date.month)
        return resources.getString(R.string.timeline_month_title, name, number(date.year))
    }

    companion object {
        /** Labels for [languageCode] and the day calendar [calendar] with texts from [resources]. */
        fun create(
            resources: Resources,
            languageCode: String,
            calendar: CalendarSystem,
            variant: IslamicVariant,
        ): TimelineLabels {
            val language = LanguageTable.forCode(languageCode) ?: LanguageTable.languages.first()
            val arithmetic = TimelineContentBuilder.primaryCalendar(listOf(calendar), variant)
            return TimelineLabels(resources, language, arithmetic, abbreviatedWeekdayNames(language))
        }

        /** Abbreviated standalone weekday names of [language] from the platform's CLDR data, Monday first. */
        private fun abbreviatedWeekdayNames(language: LanguageSpec): List<String> {
            val names =
                DateFormatSymbols
                    .getInstance(ULocale.forLanguageTag(language.localeTag))
                    .getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.ABBREVIATED)
            // ICU numbers weekdays from Sunday = 1 to Saturday = 7; index 0 is unused.
            return Weekday.entries.map { names[it.isoNumber % Weekday.entries.size + 1] }
        }
    }
}

/** [TimelineLabels] for [content] with texts from the current resources. */
@Composable
internal fun rememberTimelineLabels(content: TimelineContent): TimelineLabels {
    val resources = LocalResources.current
    // The same Resources object can change its configuration (locale), so the configuration is a key as well.
    val configuration = LocalConfiguration.current
    return remember(content.languageCode, content.calendar, content.islamicVariant, resources, configuration) {
        TimelineLabels.create(resources, content.languageCode, content.calendar, content.islamicVariant)
    }
}
