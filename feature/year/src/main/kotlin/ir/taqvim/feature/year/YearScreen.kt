/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import android.content.res.Resources
import android.icu.text.DateFormatSymbols
import android.icu.util.ULocale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.SegmentedTabs
import ir.taqvim.core.ui.component.TopBar

private val CONTROLS_PADDING = 8.dp

/**
 * The year view (T-805), stateless: the shown year and its years in the other calendars, calendar tabs, year controls
 * and the calendar pager of mini months (or the year selection).
 */
@Composable
fun YearScreen(
    state: YearUiState,
    onAction: (YearAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = state.content
    if (content == null) {
        LoadingYear(modifier)
        return
    }
    val builder = rememberYearPageBuilder(content.settings())
    YearFrame(content, builder, onAction, modifier) { bodyModifier ->
        CalendarPages(content, builder, onAction, bodyModifier)
    }
}

/** The top bar, calendar tabs and year controls around [body], which fills the remaining height. */
@Composable
internal fun YearFrame(
    content: YearContent,
    builder: YearPageBuilder,
    onAction: (YearAction) -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable (Modifier) -> Unit,
) {
    val heading =
        remember(builder, content.calendarIndex, content.year) {
            builder.heading(content.calendarIndex, content.year)
        }
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(heading.title, subtitle = heading.subtitle) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (content.calendars.size > 1) {
                val names = content.calendars.map { calendarName(it) }
                SegmentedTabs(
                    tabs = names,
                    selectedIndex = content.calendarIndex,
                    onSelect = { onAction(YearAction.SelectCalendar(it)) },
                    modifier = Modifier.padding(horizontal = CONTROLS_PADDING),
                )
            }
            YearControls(content.isPickingYear, onAction)
            body(Modifier.weight(1f).fillMaxWidth())
        }
    }
}

/** Previous year, year selection, today and next year. */
@Composable
private fun YearControls(
    isPickingYear: Boolean,
    onAction: (YearAction) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = CONTROLS_PADDING)) {
        ControlButton(stringResource(R.string.year_previous)) { onAction(YearAction.ShowPreviousYear) }
        if (isPickingYear) {
            ControlButton(stringResource(R.string.year_show_months)) { onAction(YearAction.CloseYearPicker) }
        } else {
            ControlButton(stringResource(R.string.year_choose)) { onAction(YearAction.OpenYearPicker) }
        }
        ControlButton(stringResource(R.string.year_today)) { onAction(YearAction.GoToToday) }
        ControlButton(stringResource(R.string.year_next)) { onAction(YearAction.ShowNextYear) }
    }
}

@Composable
private fun RowScope.ControlButton(
    label: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = Modifier.weight(1f)) {
        val maxSize = MaterialTheme.typography.labelLarge.fontSize
        Text(label, maxLines = 1, autoSize = TextAutoSize.StepBased(MIN_CONTROL_TEXT_SIZE, maxSize))
    }
}

/** Smallest size control labels shrink to at large font scales instead of being cut off (T-1701). */
private val MIN_CONTROL_TEXT_SIZE = 8.sp

@Composable
private fun calendarName(system: CalendarSystem): String =
    stringResource(
        when (system) {
            CalendarSystem.PERSIAN -> R.string.year_calendar_persian
            CalendarSystem.ISLAMIC -> R.string.year_calendar_islamic
            CalendarSystem.GREGORIAN -> R.string.year_calendar_gregorian
            CalendarSystem.NEPALI -> R.string.year_calendar_nepali
        },
    )

@Composable
private fun LoadingYear(modifier: Modifier) {
    val description = stringResource(R.string.year_loading)
    ScreenSurface(modifier = modifier, topBar = { TopBar(stringResource(R.string.year_title)) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.semantics { contentDescription = description })
        }
    }
}

/** The preferences behind [YearContent], for building year pages. */
internal fun YearContent.settings(): YearSettings = YearSettings(calendars, weekStart, islamicVariant, languageCode)

/** A [YearPageBuilder] for [settings] with texts from the current resources. */
@Composable
internal fun rememberYearPageBuilder(settings: YearSettings): YearPageBuilder {
    val resources = LocalResources.current
    // The same Resources object can change its configuration (locale), so the configuration is a key as well.
    val configuration = LocalConfiguration.current
    return remember(settings, resources, configuration) {
        val language = LanguageTable.forCode(settings.languageCode) ?: LanguageTable.languages.first()
        YearPageBuilder(YearCalendars(settings), language, yearTexts(resources), narrowWeekdayNames(language))
    }
}

/** Year page texts from string resources. */
internal fun yearTexts(resources: Resources): YearTexts =
    YearTexts(
        monthTitle = { month, year -> resources.getString(R.string.year_month_title, month, year) },
        range = { first, last -> resources.getString(R.string.year_range, first, last) },
        today = resources.getString(R.string.year_today),
        holidays = { count, formatted -> resources.getQuantityString(R.plurals.year_holidays, count, formatted) },
        separator = resources.getString(R.string.year_separator),
    )

/** Narrow standalone weekday names of [language] from the platform's CLDR data, in ISO order (Monday first). */
internal fun narrowWeekdayNames(language: LanguageSpec): List<String> {
    val names =
        DateFormatSymbols
            .getInstance(ULocale.forLanguageTag(language.localeTag))
            .getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.NARROW)
    // ICU numbers weekdays from Sunday = 1 to Saturday = 7; index 0 is unused.
    return Weekday.entries.map { names[it.isoNumber % YearPageBuilder.DAYS_PER_WEEK + 1] }
}
