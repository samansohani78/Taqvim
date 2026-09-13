/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import java.math.BigDecimal

/** How integer digits are grouped: every three digits, or Indian style (last three, then pairs: 12,34,567). */
public enum class DigitGrouping {
    THOUSANDS,
    INDIAN,
}

/**
 * Decimal numeral systems supported by Taqvim (T-201), identified by the code point of their digit zero, with the
 * separators conventionally used with them (Unicode CLDR number symbols for fa, ar, ne, ta and en).
 */
public enum class NumeralSystem(
    /** Digit zero; digits 1–9 follow it contiguously in Unicode. */
    public val zero: Char,
    /** Decimal separator used when formatting. */
    public val decimalSeparator: Char,
    /** Grouping separator used when formatting. */
    public val groupSeparator: Char,
    /** Grouping style used when formatting. */
    public val grouping: DigitGrouping,
) {
    LATIN('0', '.', ',', DigitGrouping.THOUSANDS),
    PERSIAN('۰', ARABIC_DECIMAL_SEPARATOR, ARABIC_GROUP_SEPARATOR, DigitGrouping.THOUSANDS),
    EASTERN_ARABIC('٠', ARABIC_DECIMAL_SEPARATOR, ARABIC_GROUP_SEPARATOR, DigitGrouping.THOUSANDS),
    DEVANAGARI('०', '.', ',', DigitGrouping.INDIAN),
    TAMIL('௦', '.', ',', DigitGrouping.INDIAN),
    ;

    /** The digit [value] (0–9) in this system. */
    public fun digit(value: Int): Char {
        require(value in 0..MAX_DIGIT) { "digit must be in 0..9 (was $value)" }
        return zero + value
    }
}

private const val MAX_DIGIT = 9
private const val ARABIC_DECIMAL_SEPARATOR = '٫'
private const val ARABIC_GROUP_SEPARATOR = '٬'

/** Formatting and lenient parsing of numbers in any [NumeralSystem]. */
public object Numerals {
    private const val GROUP_SIZE = 3
    private const val INDIAN_GROUP_SIZE = 2
    private const val MINUS_SIGN = '−'
    private val BIDI_MARKS = setOf('‎', '‏', '؜')
    private val DECIMAL_SEPARATORS = setOf('.', ARABIC_DECIMAL_SEPARATOR)
    private val GROUP_SEPARATORS = setOf(',', ARABIC_GROUP_SEPARATOR, ' ', ' ')

    /** [value] with its digits in [system]; grouped with the system's separator when [grouped]. */
    public fun format(
        value: Long,
        system: NumeralSystem,
        grouped: Boolean = false,
    ): String {
        val raw = value.toString()
        val negative = raw.startsWith('-')
        val digits = if (negative) raw.substring(1) else raw
        val body = if (grouped) group(digits, system) else digits
        return (if (negative) "-" else "") + localizeDigits(body, system)
    }

    /** [value] in plain (non-scientific) notation with the digits and separators of [system]. */
    public fun format(
        value: BigDecimal,
        system: NumeralSystem,
        grouped: Boolean = false,
    ): String {
        val plain = value.toPlainString()
        val negative = plain.startsWith('-')
        val unsigned = if (negative) plain.substring(1) else plain
        val integerPart = unsigned.substringBefore('.')
        val fraction = unsigned.substringAfter('.', missingDelimiterValue = "")
        val integerText = if (grouped) group(integerPart, system) else integerPart
        val fractionText = if (fraction.isEmpty()) "" else system.decimalSeparator + fraction
        return (if (negative) "-" else "") + localizeDigits(integerText + fractionText, system)
    }

    /** [text] with every ASCII digit replaced by the corresponding digit of [system]; other characters unchanged. */
    public fun localizeDigits(
        text: String,
        system: NumeralSystem,
    ): String = text.map { if (it in '0'..'9') system.digit(it - '0') else it }.joinToString("")

    /**
     * Parses an integer written with digits of any supported system (mixed digits allowed), optional sign, group
     * separators and bidi marks. Returns `null` for anything else, including decimals and overflow.
     */
    public fun parseLong(text: String): Long? = canonicalize(text)?.toLongOrNull()

    /** Like [parseLong] but accepts a decimal separator (`.` or `٫`); e.g. `۱۲٫۵` → 12.5. */
    public fun parseDecimal(text: String): BigDecimal? =
        canonicalize(text)?.let { runCatching { BigDecimal(it) }.getOrNull() }

    /** The value of [char] if it is a decimal digit of a supported system, otherwise `null`. */
    public fun digitValue(char: Char): Int? =
        NumeralSystem.entries.firstNotNullOfOrNull { system -> (char - system.zero).takeIf { it in 0..MAX_DIGIT } }

    private fun canonicalize(text: String): String? {
        val builder = StringBuilder(text.length)
        for (char in text.trim()) {
            val digit = digitValue(char)
            when {
                digit != null -> builder.append('0' + digit)
                char in DECIMAL_SEPARATORS -> builder.append('.')
                char in GROUP_SEPARATORS || char in BIDI_MARKS -> Unit
                char == '-' || char == MINUS_SIGN -> builder.append('-')
                char == '+' -> builder.append('+')
                else -> return null
            }
        }
        return builder.toString().takeIf { text -> text.any { it in '0'..'9' } }
    }

    private fun group(
        digits: String,
        system: NumeralSystem,
    ): String {
        if (digits.length <= GROUP_SIZE) return digits
        val separator = system.groupSeparator.toString()
        val headGroup = if (system.grouping == DigitGrouping.INDIAN) INDIAN_GROUP_SIZE else GROUP_SIZE
        val head =
            digits
                .dropLast(GROUP_SIZE)
                .reversed()
                .chunked(headGroup)
                .joinToString(separator)
                .reversed()
        return head + separator + digits.takeLast(GROUP_SIZE)
    }
}

/**
 * Traditional (non-positional) Tamil numerals with the special signs ௰ (10), ௱ (100) and ௲ (1000), as defined in
 * the Unicode Tamil block: e.g. 12 = ௰௨, 45 = ௪௰௫, 2026 = ௨௲௨௰௬, 10 000 = ௰௲.
 * A multiplier of one is omitted.
 */
public object TamilTraditionalNumerals {
    private const val TEN = '௰'
    private const val HUNDRED = '௱'
    private const val THOUSAND = '௲'
    private const val THOUSAND_VALUE = 1_000L
    private const val HUNDRED_VALUE = 100L
    private const val TEN_VALUE = 10L
    private val ONE = NumeralSystem.TAMIL.digit(1)
    private val NINE = NumeralSystem.TAMIL.digit(MAX_DIGIT)

    /** Capture groups of [BELOW_THOUSAND]. */
    private const val HUNDRED_COUNT_GROUP = 1
    private const val HUNDRED_SIGN_GROUP = 2
    private const val TEN_COUNT_GROUP = 3
    private const val TEN_SIGN_GROUP = 4
    private const val UNIT_GROUP = 5

    /** `^(?:(d)?(௱))?(?:(d)?(௰))?(d)?$` where d is a Tamil digit 1–9. */
    private val BELOW_THOUSAND = Regex("^(?:([$ONE-$NINE])?($HUNDRED))?(?:([$ONE-$NINE])?($TEN))?([$ONE-$NINE])?\$")

    /** Traditional form of [value], which must be positive (there is no traditional zero). */
    public fun format(value: Long): String {
        require(value >= 1) { "traditional Tamil numerals start at 1 (was $value)" }
        val thousands = value / THOUSAND_VALUE
        val prefix = if (thousands == 0L) "" else multiplier(thousands) + THOUSAND
        return prefix + formatBelowThousand((value % THOUSAND_VALUE).toInt())
    }

    /** Inverse of [format]; returns `null` for malformed input or overflow. */
    public fun parse(text: String): Long? {
        val at = text.lastIndexOf(THOUSAND)
        if (at < 0) return parseBelowThousand(text)
        val multiplier = text.substring(0, at).let { if (it.isEmpty()) 1L else parse(it) }
        val rest = text.substring(at + 1).let { if (it.isEmpty()) 0L else parseBelowThousand(it) }
        return if (multiplier == null || rest == null) {
            null
        } else {
            runCatching { Math.addExact(Math.multiplyExact(multiplier, THOUSAND_VALUE), rest) }.getOrNull()
        }
    }

    private fun multiplier(count: Long): String = if (count == 1L) "" else format(count)

    private fun formatBelowThousand(value: Int): String =
        buildString {
            val hundreds = value / HUNDRED_VALUE.toInt()
            val tens = value / TEN_VALUE.toInt() % TEN_VALUE.toInt()
            val units = value % TEN_VALUE.toInt()
            if (hundreds > 0) append(countPrefix(hundreds)).append(HUNDRED)
            if (tens > 0) append(countPrefix(tens)).append(TEN)
            if (units > 0) append(NumeralSystem.TAMIL.digit(units))
        }

    private fun countPrefix(count: Int): String = if (count == 1) "" else NumeralSystem.TAMIL.digit(count).toString()

    private fun parseBelowThousand(text: String): Long? {
        val match = BELOW_THOUSAND.matchEntire(text) ?: return null
        val groups = match.groupValues
        val hundreds = if (groups[HUNDRED_SIGN_GROUP].isEmpty()) 0 else digitOrOne(groups[HUNDRED_COUNT_GROUP])
        val tens = if (groups[TEN_SIGN_GROUP].isEmpty()) 0 else digitOrOne(groups[TEN_COUNT_GROUP])
        val units = if (groups[UNIT_GROUP].isEmpty()) 0 else digitOrOne(groups[UNIT_GROUP])
        val value = hundreds * HUNDRED_VALUE + tens * TEN_VALUE + units
        return value.takeIf { it > 0 }
    }

    private fun digitOrOne(text: String): Int = text.firstOrNull()?.let { it - NumeralSystem.TAMIL.zero } ?: 1
}
