/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.EventRule
import ir.taqvim.core.events.Occurrence
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.events.DayEvents
import ir.taqvim.data.preferences.StoredCountdown
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.widgets.CountdownMode
import ir.taqvim.feature.widgets.WidgetConfig
import ir.taqvim.feature.widgets.WidgetContentBuilder
import ir.taqvim.feature.widgets.WidgetCountdown
import ir.taqvim.feature.widgets.WidgetCountdownBuilder
import ir.taqvim.feature.widgets.WidgetCountdownOptions
import ir.taqvim.feature.widgets.WidgetCountdownSource
import ir.taqvim.feature.widgets.WidgetCountdownView
import ir.taqvim.feature.widgets.WidgetOccasion
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone

/**
 * [WidgetCountdownSource] (T-1212): today in the widgets' zone, the calendars Taqvim can compute (the user's first,
 * with their Islamic variant) and the dataset events of the coming months as occasions.
 */
internal class PreferencesWidgetCountdownSource(
    private val preferences: UserPreferencesRepository,
    private val rangeEvents: (JdnRange) -> Flow<List<DayEvents>>,
    private val clock: Clock,
    private val deviceZone: () -> TimeZone,
) : WidgetCountdownSource {
    override suspend fun options(): WidgetCountdownOptions {
        val prefs = preferences.preferences.first()
        val today = clock.now().toJdn(prefs.widgetPlace()?.timeZone ?: deviceZone())
        val language = prefs.languageSpec()
        val arithmetic = prefs.availableArithmetic()
        val calendars =
            (prefs.availableCalendars() + arithmetic.values)
                .distinctBy { it.system }
                .ifEmpty { listOf(PersianCalendarSystem) }
        val occasions =
            rangeEvents(today..(today + OCCASION_DAYS))
                .first()
                .flatMap { it.official }
                .distinctBy { it.definition.id }
                .mapNotNull { it.toOccasion(arithmetic, prefs.languageCode, language) }
                .take(MAX_OCCASIONS)
        return WidgetCountdownOptions(today, language, calendars, occasions)
    }

    private companion object {
        const val OCCASION_DAYS = 120
        const val MAX_OCCASIONS = 12
    }
}

/** A dataset occurrence as a countdown target: fixed-date events repeat every year on their date. */
private fun Occurrence.toOccasion(
    arithmetic: Map<CalendarSystem, CalendarArithmetic>,
    languageCode: String,
    language: LanguageSpec,
): WidgetOccasion? {
    val calendar = arithmetic[date.system] ?: return null
    return WidgetOccasion(
        title = definition.title.forLanguage(languageCode),
        calendar = date.system,
        year = date.year,
        month = date.month,
        day = date.day,
        repeatsYearly = definition.rule is EventRule.Fixed,
        dateText = WidgetContentBuilder.dayTitle(calendar, jdn, language),
    )
}

/** The countdown of [config] on [today] in its own calendar, or `null` without one or without that calendar. */
internal fun UserPreferences.widgetCountdown(
    config: WidgetConfig,
    today: Jdn,
    language: LanguageSpec,
): WidgetCountdownView? {
    val countdown = config.countdown ?: return null
    val calendar = availableArithmetic()[countdown.calendar] ?: return null
    return WidgetCountdownBuilder.view(countdown, calendar, today, language)
}

internal fun StoredCountdown.toWidgetCountdown(): WidgetCountdown =
    WidgetCountdown(
        calendar = calendar,
        year = year,
        month = month,
        day = day,
        startJdn = startJdn,
        mode = CountdownMode.entries.firstOrNull { it.name == mode } ?: CountdownMode.UNTIL,
        repeatsYearly = repeatsYearly,
        title = title,
    )

internal fun WidgetCountdown.toStored(): StoredCountdown =
    StoredCountdown(title, calendar, year, month, day, mode.name, repeatsYearly, startJdn)
