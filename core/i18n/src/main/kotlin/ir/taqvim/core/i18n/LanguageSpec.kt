/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import ir.taqvim.core.model.AsrJuristic
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.model.Weekday

/** Writing direction of a language. */
public enum class TextDirection {
    LTR,
    RTL,
}

/** Order of the year, month and day fields in a numeric date. */
public enum class DateFieldOrder {
    DMY,
    MDY,
    YMD,
}

/** Numeric date layout: field [order], the [separator] between fields, and whether day and month are [zeroPad]ded. */
public data class DatePattern(
    public val order: DateFieldOrder,
    public val separator: Char,
    public val zeroPad: Boolean,
) {
    public companion object {
        private const val FIELDS = "yMd"

        /** Derives the layout from a CLDR short date pattern such as `y/M/d`, `dd.MM.y` or `M/d/yy`. */
        public fun fromCldr(pattern: String): DatePattern {
            require(FIELDS.all { it in pattern }) { "date pattern '$pattern' must contain y, M and d" }
            val order =
                FIELDS
                    .toList()
                    .sortedBy { pattern.indexOf(it) }
                    .joinToString("")
                    .uppercase()
            val firstFieldEnd = pattern.indexOfFirst { it != pattern.first() }
            val separator =
                requireNotNull(
                    pattern.drop(firstFieldEnd).firstOrNull { !it.isLetter() && !it.isWhitespace() && it != '\'' },
                ) { "date pattern '$pattern' has no separator" }
            return DatePattern(
                order = DateFieldOrder.valueOf(order),
                separator = separator,
                zeroPad = "dd" in pattern || "MM" in pattern,
            )
        }
    }
}

/** Localized abbreviations for the two halves of a 12-hour day. */
public data class DayPeriodNames(
    public val am: String,
    public val pm: String,
)

/**
 * Standalone wide month names per calendar. A calendar is `null` when no verified localized data exists yet
 * (docs/DATA_TODO.md); callers then fall back to another language's names.
 */
public data class MonthNames(
    public val gregorian: List<String>,
    public val persian: List<String>?,
    public val islamic: List<String>?,
    public val nepali: List<String>?,
) {
    init {
        listOfNotNull(gregorian, persian, islamic, nepali).forEach { names ->
            require(names.size == MONTHS && names.none { it.isBlank() }) { "expected 12 month names, got $names" }
        }
    }

    /** The names for [system], or `null` when they are not available in this language. */
    public fun forSystem(system: CalendarSystem): List<String>? =
        when (system) {
            CalendarSystem.GREGORIAN -> gregorian
            CalendarSystem.PERSIAN -> persian
            CalendarSystem.ISLAMIC -> islamic
            CalendarSystem.NEPALI -> nepali
        }

    private companion object {
        const val MONTHS = 12
    }
}

/**
 * One launch language (T-200, docs/adr/0007-launch-languages.md). CLDR-derived fields describe the language's
 * primary region ([localeTag]); [numerals], [calendars], [prayerMethod] and [asrJuristic] are product defaults
 * that the user can change in settings.
 *
 * @property andPattern two-item "and" list pattern with `{0}` and `{1}`, or `null` when CLDR has no localized one.
 */
public data class LanguageSpec(
    public val code: String,
    public val localeTag: String,
    public val nativeName: String,
    public val script: String,
    public val direction: TextDirection,
    public val numerals: NumeralSystem,
    public val calendars: List<CalendarSystem>,
    public val weekStart: Weekday,
    public val weekend: Set<Weekday>,
    public val prayerMethod: PrayerMethod,
    public val asrJuristic: AsrJuristic,
    public val monthNames: MonthNames,
    public val datePattern: DatePattern,
    public val dayPeriods: DayPeriodNames,
    public val andPattern: String?,
) {
    init {
        require(calendars.isNotEmpty() && calendars.distinct() == calendars) { "'$code': invalid calendars $calendars" }
        require(weekend.isNotEmpty()) { "'$code': weekend must not be empty" }
        require(andPattern == null || PLACEHOLDERS.all { it in andPattern }) { "'$code': bad and-pattern $andPattern" }
    }

    /** "[first] and [second]" in this language, or `null` when [andPattern] is unavailable. */
    public fun joinWithAnd(
        first: String,
        second: String,
    ): String? =
        andPattern?.let { pattern ->
            PLACEHOLDER.replace(pattern) {
                if (it.value ==
                    "{0}"
                ) {
                    first
                } else {
                    second
                }
            }
        }

    private companion object {
        val PLACEHOLDERS = listOf("{0}", "{1}")
        val PLACEHOLDER = Regex("""\{[01]}""")
    }
}
