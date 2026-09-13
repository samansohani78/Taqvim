/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import android.content.res.Resources
import android.icu.text.DateFormatSymbols
import android.icu.util.ULocale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.TopBar

/**
 * The calendar (home) screen, stateless: the shown month's title over the month pager (T-801). The day details
 * (T-802) and the toolbar actions (T-803) are added by their tasks.
 */
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = state.content
    if (content == null) {
        LoadingCalendar(modifier)
        return
    }
    val builder = rememberMonthPageBuilder(content.settings())
    val heading = remember(builder, content.visibleMonth) { builder.heading(content.visibleMonth) }
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(heading.title, subtitle = heading.subtitle) },
    ) { padding ->
        MonthPager(content, builder, onAction, Modifier.fillMaxSize().padding(padding))
    }
}

@Composable
private fun LoadingCalendar(modifier: Modifier) {
    val description = stringResource(R.string.calendar_loading)
    ScreenSurface(modifier = modifier, topBar = { TopBar(stringResource(R.string.calendar_title)) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.semantics { contentDescription = description })
        }
    }
}

/** The preferences behind [CalendarContent], for building month pages. */
internal fun CalendarContent.settings(): CalendarSettings =
    CalendarSettings(calendars, weekStart, islamicVariant, languageCode, showWeekNumbers)

/** A [MonthPageBuilder] for [settings] with texts from the current resources and dots in the theme's colors. */
@Composable
internal fun rememberMonthPageBuilder(settings: CalendarSettings): MonthPageBuilder {
    val resources = LocalResources.current
    // The same Resources object can change its configuration (locale), so the configuration is a key as well.
    val configuration = LocalConfiguration.current
    val colors = MaterialTheme.colorScheme
    return remember(settings, resources, configuration, colors) {
        val language = LanguageTable.forCode(settings.languageCode) ?: LanguageTable.languages.first()
        MonthPageBuilder(
            calendars = CalendarCalendars(settings),
            language = language,
            texts = monthTexts(resources),
            palette = colors.indicatorPalette(),
            weekdayNames = shortWeekdayNames(language),
        )
    }
}

/** Month page texts from string resources. */
internal fun monthTexts(resources: Resources): MonthTexts =
    MonthTexts(
        monthTitle = { month, year -> resources.getString(R.string.calendar_month_title, month, year) },
        monthRange = { first, last -> resources.getString(R.string.calendar_month_range, first, last) },
        today = resources.getString(R.string.calendar_today),
        holiday = resources.getString(R.string.calendar_holiday),
        events = { count, formatted -> resources.getQuantityString(R.plurals.calendar_events, count, formatted) },
        separator = resources.getString(R.string.calendar_separator),
        newEvent = resources.getString(R.string.calendar_new_event),
        week = { number -> resources.getString(R.string.calendar_week_number, number) },
    )

/** Event dot colors: holidays in the holiday (error) color, other sources in distinct theme roles. */
internal fun ColorScheme.indicatorPalette(): IndicatorPalette =
    IndicatorPalette(
        holiday = error,
        official = primary,
        personal = tertiary,
        device = secondary,
        subscription = outline,
    )

/** Short standalone weekday names of [language] from the platform's CLDR data, in ISO order (Monday first). */
internal fun shortWeekdayNames(language: LanguageSpec): List<String> {
    val names =
        DateFormatSymbols
            .getInstance(ULocale.forLanguageTag(language.localeTag))
            .getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.SHORT)
    // ICU numbers weekdays from Sunday = 1 to Saturday = 7; index 0 is unused.
    return Weekday.entries.map { names[it.isoNumber % MonthLayout.DAYS_PER_WEEK + 1] }
}
