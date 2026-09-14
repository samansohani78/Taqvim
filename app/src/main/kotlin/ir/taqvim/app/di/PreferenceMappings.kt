/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.calendar.CalendarCalendars

/** The language table entry of the app language, or of [UserPreferences.FALLBACK_LANGUAGE] for unknown codes. */
internal fun UserPreferences.languageSpec(): LanguageSpec =
    LanguageTable.forCode(languageCode)
        ?: requireNotNull(
            LanguageTable.forCode(UserPreferences.FALLBACK_LANGUAGE),
        ) { "language table lacks a fallback" }

/** Arithmetic of every calendar Taqvim can compute, with the user's Islamic variant (Nepali waits for T-105). */
internal fun UserPreferences.availableArithmetic(): Map<CalendarSystem, CalendarArithmetic> =
    CalendarSystem.entries
        .mapNotNull { system -> CalendarCalendars.arithmeticFor(system, islamicVariant)?.let { system to it } }
        .toMap()

/** The user's calendars that can be computed, in the user's order and without repeats. */
internal fun UserPreferences.availableCalendars(): List<CalendarArithmetic> =
    calendars.distinct().mapNotNull { CalendarCalendars.arithmeticFor(it, islamicVariant) }
