/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import android.content.Intent
import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            SegmentedTabs(
                tabs = labels,
                selectedIndex = content.selectedTab.ordinal,
                onSelect = { onAction(CalendarAction.SelectTab(DayDetailsTab.entries[it])) },
                modifier = Modifier.weight(1f),
            )
            ShareDayButton(content, language)
        }
        when (content.selectedTab) {
            DayDetailsTab.CALENDARS -> DayCalendarsTab(content, language, onAction = onAction)
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

/**
 * Shares the day as text, with the primary source of every official event (T-802).
 *
 * This is the one thing the app can hand someone that another calendar cannot: not "Nowruz is on 21 March" but the
 * gazette, page and URL it comes from. The citations are already on screen; this lets them leave with the reader.
 * Nothing is uploaded — the text goes to whichever app the user picks from the system chooser.
 */
@Composable
private fun ShareDayButton(
    content: CalendarContent,
    language: LanguageSpec,
) {
    val context = LocalContext.current
    val label = stringResource(R.string.calendar_share_day)
    val heading = stringResource(R.string.calendar_share_sources)
    val weekday = content.selectedDay.weekday()
    val details = content.dayDetails
    IconButton(
        enabled = details != null,
        onClick = {
            val text =
                DayShareText.build(
                    dateLines =
                        content.selectedDates.map { DateFormatter.format(it, weekday, language, DateStyle.LONG) },
                    entries = details?.events.orEmpty().map(::shareEntry),
                    sourcesHeading = heading,
                )
            val send =
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, text)
            runCatching { context.startActivity(Intent.createChooser(send, label)) }
        },
    ) {
        Icon(ImageVector.vectorResource(R.drawable.calendar_ic_share), contentDescription = label)
    }
}

private fun shareEntry(event: DayEventItem): DayShareText.Entry =
    DayShareText.Entry(
        title = event.title,
        sources = event.citations.map { DayShareText.Source(it.title, it.page, it.url) },
    )

/**
 * The other Islamic methods, revealed only when they disagree (F-06).
 *
 * A date that every method agrees on raises no question, so showing five identical rows would be noise; the row
 * appears exactly when a user has something to ask about, and stays collapsed until they do. Each row names the
 * method, its reading of the day, and where that reading came from — an official override reads differently from a
 * computed one, and that difference is usually the answer.
 */
@Composable
private fun IslamicVariantsDisclosure(
    content: CalendarContent,
    language: LanguageSpec,
) {
    val rows = content.islamicVariantDates
    val weekday = content.selectedDay.weekday()
    val disagree = rows.map { it.date }.distinct().size > 1
    if (!disagree) return
    var expanded by rememberSaveable(content.selectedDay.value) { mutableStateOf(false) }
    val label =
        stringResource(if (expanded) R.string.calendar_variants_hide else R.string.calendar_variants_differ)
    TextButton(onClick = { expanded = !expanded }) { Text(label, style = MaterialTheme.typography.labelLarge) }
    if (!expanded) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            DetailRow(
                stringResource(DayDetailsLabels.of(row.variant)),
                DateFormatter.format(row.date, weekday, language, DateStyle.LONG),
                note = stringResource(DayDetailsLabels.of(row.origin)),
            )
        }
    }
}

/** The day in each of the user's calendars, with where an Islamic date came from and the variants that disagree. */
@Composable
private fun DayInEveryCalendar(
    content: CalendarContent,
    language: LanguageSpec,
) {
    val weekday = content.selectedDay.weekday()
    content.selectedDates.forEachIndexed { index, date ->
        val origin = content.selectedOrigins.getOrNull(index)?.takeIf { date.system == CalendarSystem.ISLAMIC }
        DetailRow(
            stringResource(DayDetailsLabels.of(date.system)),
            DateFormatter.format(date, weekday, language, DateStyle.LONG),
            note = origin?.let { stringResource(DayDetailsLabels.of(it)) },
        )
        if (date.system == CalendarSystem.ISLAMIC) IslamicVariantsDisclosure(content, language)
    }
}

/** The Calendars tab: the day in every calendar, its distance from today, week, season, Sun and Moon. */
@Composable
internal fun DayCalendarsTab(
    content: CalendarContent,
    language: LanguageSpec,
    modifier: Modifier = Modifier,
    onAction: (CalendarAction) -> Unit = {},
) {
    val overview = content.overview
    if (overview == null) {
        DetailsLoading(stringResource(R.string.calendar_details_loading), modifier)
        return
    }
    val resources = LocalResources.current
    // The primary calendar is the first of the shown dates, and its year is what the week number is compared with.
    val primaryYear = content.selectedDates.firstOrNull()?.year
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DayInEveryCalendar(content, language)
        ShiftOnThisDay(content, onAction)
        DetailRow(stringResource(R.string.calendar_distance_label), distanceText(resources, overview, language))
        IndicatorRow(
            stringResource(R.string.calendar_week_label),
            weekText(
                resources,
                overview,
                primaryYear ?: overview.weekBasedYear,
                language,
            ),
        ) { text ->
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

/**
 * "Day 3 of the week, week 12 of the year" — and the year itself when the week belongs to another one (A-08).
 *
 * On the last days of a year whose successor's week 1 has already begun, "week 1 of the year" beside a date in the
 * old year reads like a mistake. [shownYear] is the year of the date on screen, so the two can be compared.
 */
internal fun weekText(
    resources: Resources,
    overview: DayOverview,
    shownYear: Int,
    language: LanguageSpec,
): String =
    if (overview.weekBasedYear == shownYear) {
        resources.getString(
            R.string.calendar_week_progress,
            number(overview.dayOfWeek.toLong(), language),
            number(overview.weekOfYear.toLong(), language),
        )
    } else {
        resources.getString(
            R.string.calendar_week_progress_other_year,
            number(overview.dayOfWeek.toLong(), language),
            number(overview.weekOfYear.toLong(), language),
            number(overview.weekBasedYear.toLong(), language),
        )
    }

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
