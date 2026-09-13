/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday

/** How a date is written. */
public enum class DateStyle {
    /** CLDR full pattern of the language and calendar: weekday, day, month name, year and calendar abbreviation. */
    LONG,

    /** Numeric day, month and year in the language's order, separator and padding. */
    NUMERIC,

    /** `YYYY-MM-DD` in the date's own calendar. */
    ISO,
}

/**
 * Formats calendar dates for a language (T-202). Dates are calendar-agnostic; conversion happens in `:core:calendar`.
 *
 * LONG interprets the CLDR full date pattern: `E…` weekday, `d`/`dd` day, `M`/`MM` month number, `MMM…`/`LLL…` month
 * name, `y` year (`yy` two digits), `G` calendar abbreviation, and quoted literals. Where the language has no month
 * names for the calendar (docs/DATA_TODO.md) the month is written as a number; where it has no calendar abbreviation
 * the `G` field is left out. Nepali dates use the Gregorian pattern, as CLDR has no Bikram Sambat calendar.
 */
public object DateFormatter {
    private const val QUOTE = '\''
    private const val NAME_WIDTH = 3
    private const val TWO_DIGITS = 2
    private const val CENTURY = 100
    private val SPACES = Regex("""\s{2,}""")

    /**
     * [date] (whose weekday is [weekday]) in [style]. Digits use [numerals], by default the language's numeral system
     * for LONG and NUMERIC and Latin digits for ISO.
     */
    public fun format(
        date: CalendarDate,
        weekday: Weekday,
        language: LanguageSpec,
        style: DateStyle,
        numerals: NumeralSystem? = null,
    ): String =
        when (style) {
            DateStyle.LONG -> formatLong(date, weekday, FormatTable.of(language), numerals ?: language.numerals)
            DateStyle.NUMERIC -> formatNumeric(date, language.datePattern, numerals ?: language.numerals)
            DateStyle.ISO -> Numerals.localizeDigits(date.toIsoLikeString(), numerals ?: NumeralSystem.LATIN)
        }

    private fun formatNumeric(
        date: CalendarDate,
        pattern: DatePattern,
        numerals: NumeralSystem,
    ): String {
        fun padded(value: Int) = if (pattern.zeroPad) value.toString().padStart(TWO_DIGITS, '0') else value.toString()
        val fields =
            when (pattern.order) {
                DateFieldOrder.DMY -> listOf(padded(date.day), padded(date.month), date.year.toString())
                DateFieldOrder.MDY -> listOf(padded(date.month), padded(date.day), date.year.toString())
                DateFieldOrder.YMD -> listOf(date.year.toString(), padded(date.month), padded(date.day))
            }
        return Numerals.localizeDigits(fields.joinToString(pattern.separator.toString()), numerals)
    }

    private fun formatLong(
        date: CalendarDate,
        weekday: Weekday,
        formats: LanguageFormats,
        numerals: NumeralSystem,
    ): String {
        val calendar = if (date.system == CalendarSystem.NEPALI) CalendarSystem.GREGORIAN else date.system
        val pattern = requireNotNull(formats.datePatterns[calendar]) { "no ${date.system} pattern for ${formats.code}" }
        return formatPattern(pattern, date, weekday, formats, numerals)
    }

    /** [date] written with the CLDR date [pattern] and the names in [formats]. */
    internal fun formatPattern(
        pattern: String,
        date: CalendarDate,
        weekday: Weekday,
        formats: LanguageFormats,
        numerals: NumeralSystem,
    ): String {
        val context = FieldContext(date, weekday, formats, numerals)
        val text =
            tokenize(pattern).joinToString("") { token ->
                token.literal
                    ?: context.field(token.letter, token.count)
            }
        return SPACES.replace(text, " ").trim()
    }

    /** A run of one pattern letter, or a literal. */
    private class Token(
        val letter: Char,
        val count: Int,
        val literal: String?,
    )

    private fun tokenize(pattern: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var index = 0
        while (index < pattern.length) {
            val char = pattern[index]
            val end =
                when {
                    char == QUOTE -> quotedEnd(pattern, index)
                    char.isAsciiLetter() -> pattern.indexOfFirst(index) { it != char }
                    else -> index + 1
                }
            tokens +=
                when {
                    char == QUOTE -> Token(char, 0, unquote(pattern.substring(index, end)))
                    char.isAsciiLetter() -> Token(char, end - index, null)
                    else -> Token(char, 0, char.toString())
                }
            index = end
        }
        return tokens
    }

    private fun quotedEnd(
        pattern: String,
        start: Int,
    ): Int {
        val closing = pattern.indexOf(QUOTE, start + 1)
        return if (closing < 0) pattern.length else closing + 1
    }

    private fun unquote(quoted: String): String = if (quoted == "''") "'" else quoted.trim(QUOTE)

    private fun String.indexOfFirst(
        from: Int,
        predicate: (Char) -> Boolean,
    ): Int = (from until length).firstOrNull { predicate(this[it]) } ?: length

    private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

    private class FieldContext(
        private val date: CalendarDate,
        private val weekday: Weekday,
        private val formats: LanguageFormats,
        private val numerals: NumeralSystem,
    ) {
        fun field(
            letter: Char,
            count: Int,
        ): String =
            when (letter) {
                'E' -> {
                    (
                        formats.weekdays[date.system] ?: formats.weekdays.getValue(
                            CalendarSystem.GREGORIAN,
                        )
                    )[weekday.ordinal]
                }

                'd' -> {
                    number(date.day, padded = count >= TWO_DIGITS)
                }

                'M', 'L' -> {
                    month(count)
                }

                'y' -> {
                    year(count)
                }

                'G' -> {
                    formats.eras[date.system].orEmpty()
                }

                else -> {
                    throw IllegalArgumentException("unsupported date pattern letter '$letter'")
                }
            }

        private fun month(count: Int): String {
            val names = formats.monthNames[date.system]
            return if (count >= NAME_WIDTH && names != null) {
                names[date.month - 1]
            } else {
                number(date.month, padded = count == TWO_DIGITS)
            }
        }

        private fun year(count: Int): String =
            if (count == TWO_DIGITS) {
                number(Math.floorMod(date.year, CENTURY), padded = true)
            } else {
                Numerals.localizeDigits(date.year.toString(), numerals)
            }

        private fun number(
            value: Int,
            padded: Boolean,
        ): String {
            val digits = if (padded) value.toString().padStart(TWO_DIGITS, '0') else value.toString()
            return Numerals.localizeDigits(digits, numerals)
        }
    }
}
