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

/** Arithmetic of every calendar, with the user's Islamic variant and optional official months (ADR-0037). */
internal fun UserPreferences.availableArithmetic(): Map<CalendarSystem, CalendarArithmetic> =
    CalendarSystem.entries.associateWith { CalendarCalendars.arithmeticFor(it, islamicVariant, islamicOverride.table) }

/** The user's calendars, in the user's order and without repeats. */
internal fun UserPreferences.availableCalendars(): List<CalendarArithmetic> =
    calendars.distinct().map { CalendarCalendars.arithmeticFor(it, islamicVariant, islamicOverride.table) }
