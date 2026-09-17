/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import ir.taqvim.core.calendar.OverrideProblem
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.model.CalendarDate
import kotlinx.coroutines.flow.Flow

/** Which official Islamic month starts are applied on top of the computed calendar (ADR-0037). */
enum class IslamicOverrideKind {
    /** None: every Islamic date is computed. */
    NONE,

    /** The official Iranian dates bundled with the app. */
    OFFICIAL,

    /** A file the user imported. */
    IMPORTED,
}

/**
 * The optional official Islamic dates as the settings page shows them: [kind], the first and last covered Hijri months
 * ([firstMonth], [lastMonth], `null` when nothing is applied), whether the chosen file no longer reads ([broken]) and
 * whether its months end before today ([ended]); later dates are computed in both cases.
 */
data class IslamicOverrideStatus(
    val language: LanguageSpec,
    val kind: IslamicOverrideKind,
    val firstMonth: CalendarDate? = null,
    val lastMonth: CalendarDate? = null,
    val broken: Boolean = false,
    val ended: Boolean = false,
)

/** What importing an override file did: [IMPORTED], or why the file was refused. */
enum class OverrideImportResult {
    IMPORTED,

    /** The file could not be opened or read. */
    UNREADABLE,

    /** The file is larger than an override can be. */
    TOO_LARGE,
    NOT_JSON,
    UNSUPPORTED_VERSION,
    TOO_FEW_MONTHS,
    INVALID_MONTH,
    INVALID_LENGTH,
    MISSING_CITATION,
    ;

    companion object {
        /** The result for a file rejected with [problem]. */
        fun of(problem: OverrideProblem): OverrideImportResult = valueOf(problem.name)
    }
}

/** Reads and changes the optional official Islamic dates; bound in `:app` over the user preferences. */
interface IslamicOverrideStore {
    fun status(): Flow<IslamicOverrideStatus>

    /** Applies the bundled official Iranian dates, or goes back to computed dates when [enabled] is `false`. */
    suspend fun setOfficial(enabled: Boolean)

    /** Imports the override file at the content address [uri] (from the system file picker). */
    suspend fun import(uri: String): OverrideImportResult

    /** Removes any override: every Islamic date is computed again. */
    suspend fun remove()
}
