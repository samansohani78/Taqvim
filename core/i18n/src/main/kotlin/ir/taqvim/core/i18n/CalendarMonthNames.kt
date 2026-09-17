/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem

/*
 * Year-aware month names (F07, T-108). Most calendars have one fixed list of 12 names per language, but a Hebrew year
 * has 12 or 13 months, so its names depend on the year. Screens look month names up here instead of indexing the
 * flat lists; `null` means the language has no names for the calendar and callers show month numbers as before.
 */

/** The stand-alone name of [date]'s month in this language, or `null` when the language has no names for it. */
public fun LanguageSpec.monthName(date: CalendarDate): String? =
    monthNamesOf(date.system, date.year)?.getOrNull(date.month - 1)

/** The stand-alone month names of [system]'s [year] in this language, month 1 first, or `null` when unavailable. */
public fun LanguageSpec.monthNamesOf(
    system: CalendarSystem,
    year: Int,
): List<String>? = hebrewOr(code, system, year) { monthNames.forSystem(system) }

/** The format-context name of [date]'s month in this language, or `null` when the language has no names for it. */
public fun LanguageFormats.monthName(date: CalendarDate): String? =
    monthNamesOf(date.system, date.year)?.getOrNull(date.month - 1)

/** The format-context month names of [system]'s [year] in this language, month 1 first, or `null` when unavailable. */
public fun LanguageFormats.monthNamesOf(
    system: CalendarSystem,
    year: Int,
): List<String>? = hebrewOr(code, system, year) { monthNames[system] }

private inline fun hebrewOr(
    code: String,
    system: CalendarSystem,
    year: Int,
    other: () -> List<String>?,
): List<String>? =
    if (system == CalendarSystem.HEBREW) {
        HebrewMonthNames.months(code, HebrewMonthNames.isLeapYear(year))
    } else {
        other()
    }
