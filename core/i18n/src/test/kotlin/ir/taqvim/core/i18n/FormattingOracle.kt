/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import com.ibm.icu.util.ULocale

/**
 * Normalization applied to both Taqvim and ICU4J output before comparison: every decimal digit becomes ASCII (Taqvim's
 * product default numerals may differ from CLDR's, e.g. Latin digits for `bn`), bidi marks are removed, and grouping
 * separators between digits are dropped (Taqvim writes counts without grouping).
 */
internal fun normalizeForOracle(text: String): String {
    val digits =
        text
            .filterNot { it in BIDI_MARKS }
            .map { char ->
                if (Character.getType(char) ==
                    Character.DECIMAL_DIGIT_NUMBER.toInt()
                ) {
                    '0' + Character.digit(char, 10)
                } else {
                    char
                }
            }.joinToString("")
    return GROUPING.replace(digits, "")
}

/** The ICU locale of a launch language. */
internal val LanguageSpec.icuLocale: ULocale
    get() = ULocale.forLanguageTag(localeTag)

private val BIDI_MARKS = setOf('‎', '‏', '؜')
private val GROUPING = Regex("""(?<=\d)[,٬  .' ](?=\d{3}(?!\d))""")
