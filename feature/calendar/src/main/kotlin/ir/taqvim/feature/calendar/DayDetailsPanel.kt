/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.calendar.DatePeriod
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.ui.component.MoonDisc
import ir.taqvim.core.ui.component.ProgressRing
import ir.taqvim.core.ui.component.SegmentedTabs
import kotlin.math.abs
import kotlin.math.roundToInt

private val INDICATOR_SIZE = 44.dp
private const val PERCENT = 100

/** The day details under the month (T-802): Calendars, Events and Times tabs of the selected day. */
@Composable
internal fun DayDetailsPanel(
    content: CalendarContent,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = remember(content.languageCode) { languageOf(content.languageCode) }
    val labels = DayDetailsTab.entries.map { stringResource(DayDetailsLabels.of(it)) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SegmentedTabs(
            tabs = labels,
            selectedIndex = content.selectedTab.ordinal,
            onSelect = { onAction(CalendarAction.SelectTab(DayDetailsTab.entries[it])) },
        )
        when (content.selectedTab) {
            DayDetailsTab.CALENDARS -> DayCalendarsTab(content, language)
            DayDetailsTab.EVENTS -> DayEventsTab(content, language, onAction)
            DayDetailsTab.TIMES -> DayTimesTab(content.times, language)
        }
    }
}

/** The launch language [code], or the first launch language when unknown. */
internal fun languageOf(code: String): LanguageSpec = LanguageTable.forCode(code) ?: LanguageTable.languages.first()

/** [value] in the digits of [language]. */
internal fun number(
    value: Long,
    language: LanguageSpec,
): String = Numerals.format(value, language.numerals)

/** The Calendars tab: the day in every calendar, its distance from today, week, season, Sun and Moon. */
@Composable
internal fun DayCalendarsTab(
    content: CalendarContent,
    language: LanguageSpec,
    modifier: Modifier = Modifier,
) {
    val overview = content.overview
    if (overview == null) {
        DetailsLoading(stringResource(R.string.calendar_details_loading), modifier)
        return
    }
    val resources = LocalResources.current
    val weekday = content.selectedDay.weekday()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        content.selectedDates.forEachIndexed { index, date ->
            val origin = content.selectedOrigins.getOrNull(index)?.takeIf { date.system == CalendarSystem.ISLAMIC }
            DetailRow(
                stringResource(DayDetailsLabels.of(date.system)),
                DateFormatter.format(date, weekday, language, DateStyle.LONG),
                note = origin?.let { stringResource(DayDetailsLabels.of(it)) },
            )
        }
        DetailRow(stringResource(R.string.calendar_distance_label), distanceText(resources, overview, language))
        IndicatorRow(stringResource(R.string.calendar_week_label), weekText(resources, overview, language)) { text ->
            ProgressRing(overview.dayOfWeek / MonthLayout.DAYS_PER_WEEK.toFloat(), text, Modifier.size(INDICATOR_SIZE))
        }
        IndicatorRow(
            stringResource(R.string.calendar_season_label),
            seasonText(resources, overview, language),
        ) { text ->
            ProgressRing(overview.dayOfSeason / overview.seasonLength.toFloat(), text, Modifier.size(INDICATOR_SIZE))
        }
        IndicatorRow(stringResource(R.string.calendar_sky_label), skyText(resources, overview, language)) { text ->
            MoonDisc(
                illuminatedFraction = overview.moon.illuminatedFraction,
                waxing = overview.moon.brightLimbOnRight,
                contentDescription = text,
                modifier = Modifier.size(INDICATOR_SIZE),
            )
        }
    }
}

/** A labelled value, read as one element. */
@Composable
internal fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    note: String? = null,
) {
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
        note?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A labelled [text] beside a graphic [indicator] that is announced with the same text. */
@Composable
private fun IndicatorRow(
    label: String,
    text: String,
    indicator: @Composable (description: String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        indicator(text)
        DetailRow(label, text)
    }
}

/** A progress indicator announced with [description]. */
@Composable
internal fun DetailsLoading(
    description: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

internal fun distanceText(
    resources: Resources,
    overview: DayOverview,
    language: LanguageSpec,
): String {
    val days = overview.daysFromToday
    val count = abs(days).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    val relative =
        when {
            days == 0L -> resources.getString(R.string.calendar_today)
            days > 0 -> resources.getQuantityString(R.plurals.calendar_days_later, count, number(abs(days), language))
            else -> resources.getQuantityString(R.plurals.calendar_days_ago, count, number(abs(days), language))
        }
    val period = overview.period
    return if (period.years == 0 && period.months == 0) {
        relative
    } else {
        resources.getString(R.string.calendar_distance_with_period, relative, periodText(resources, period, language))
    }
}

private fun periodText(
    resources: Resources,
    period: DatePeriod,
    language: LanguageSpec,
): String =
    listOf(
        R.plurals.calendar_period_years to period.years,
        R.plurals.calendar_period_months to period.months,
        R.plurals.calendar_period_days to period.days,
    ).filter { (_, value) -> value != 0 }
        .joinToString(resources.getString(R.string.calendar_separator)) { (plural, value) ->
            resources.getQuantityString(plural, abs(value), number(abs(value).toLong(), language))
        }

internal fun weekText(
    resources: Resources,
    overview: DayOverview,
    language: LanguageSpec,
): String =
    resources.getString(
        R.string.calendar_week_progress,
        number(overview.dayOfWeek.toLong(), language),
        number(overview.weekOfYear.toLong(), language),
    )

internal fun seasonText(
    resources: Resources,
    overview: DayOverview,
    language: LanguageSpec,
): String =
    resources.getString(
        R.string.calendar_season_progress,
        resources.getString(DayDetailsLabels.of(overview.season)),
        number(overview.dayOfSeason.toLong(), language),
        number(overview.seasonLength.toLong(), language),
    )

internal fun skyText(
    resources: Resources,
    overview: DayOverview,
    language: LanguageSpec,
): String {
    val moon = overview.moon
    val percent = number((moon.illuminatedFraction * PERCENT).roundToInt().toLong(), language)
    val moonText =
        resources.getString(
            R.string.calendar_moon,
            resources.getString(DayDetailsLabels.of(moon.phase)),
            resources.getString(R.string.calendar_percent, percent),
        )
    val signs =
        resources.getString(
            R.string.calendar_zodiac,
            resources.getString(DayDetailsLabels.of(overview.sunSign)),
            resources.getString(DayDetailsLabels.of(moon.sign)),
        )
    return moonText + resources.getString(R.string.calendar_separator) + signs
}
