/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.nlp.DateParser
import ir.taqvim.core.nlp.ParseContext
import ir.taqvim.core.nlp.ParseKind

/**
 * Dates for the search screen in the language of [settings]: result days written in the first available calendar,
 * and "go to date" for queries that read as a single date (T-500).
 */
internal class SearchDates(
    settings: SearchSettings,
) {
    val language: LanguageSpec = LanguageTable.forCode(settings.languageCode) ?: LanguageTable.languages.first()
    private val calendar: CalendarSystem =
        settings.calendars.firstOrNull { it in ParseContext.DEFAULT_CALENDARS } ?: CalendarSystem.GREGORIAN
    private val arithmetic: CalendarArithmetic = ParseContext.DEFAULT_CALENDARS.getValue(calendar)

    /** [jdn] in the primary calendar, LONG style with the weekday. */
    fun label(jdn: Jdn): String = DateFormatter.format(arithmetic.fromJdn(jdn), jdn.weekday(), language, DateStyle.LONG)

    /**
     * The date [query] reads as relative to [today], or `null` when it is not mostly a date: the best reading must
     * be a single day (not a range), at least [MIN_CONFIDENCE] confident, and cover at least half of the query.
     */
    fun suggest(
        query: String,
        today: Jdn,
    ): DateSuggestion? {
        val text = query.trim()
        val context = ParseContext.forLanguage(language, today, calendar)
        val reading = if (text.isEmpty()) null else DateParser.parseBest(text, context)
        val accepted =
            reading?.takeIf {
                it.kind != ParseKind.RANGE && it.confidence >= MIN_CONFIDENCE && it.span.count() * 2 >= text.length
            }
        return accepted?.let { DateSuggestion(it.jdn, label(it.jdn)) }
    }

    private companion object {
        const val MIN_CONFIDENCE = 0.7
    }
}
