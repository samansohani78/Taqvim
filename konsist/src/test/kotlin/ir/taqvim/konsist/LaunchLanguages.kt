/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import ir.taqvim.core.i18n.FormatTable
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.TextDirection
import java.io.File

/** Facts about the 24 launch languages (ADR-0007) that the translation checks need, from `:core:i18n` (T-1702). */
object LaunchLanguages {
    /** Path of the same-as-source exception list, relative to the project root. */
    const val SAME_AS_SOURCE_FILE = "config/i18n/same-as-source.txt"
    private const val REASON_SEPARATOR = " | "

    /** Launch language codes in `LanguageTable` order. */
    val codes: List<String> get() = LanguageTable.languages.map { it.code }

    /** Codes of the right-to-left launch languages. */
    val rightToLeft: Set<String>
        get() =
            LanguageTable.languages
                .filter { it.direction == TextDirection.RTL }
                .map { it.code }
                .toSet()

    /** Android plural quantities a launch language distinguishes (CLDR rules), or `null` for other languages. */
    fun pluralQuantities(code: String): Set<String>? =
        LanguageTable.forCode(code)?.let { language ->
            FormatTable
                .of(language)
                .pluralRules.categories
                .map { it.name.lowercase() }
                .toSet()
        }

    /** `resDir: name` → reason for every line of the same-as-source exception list under [root]. */
    fun sameAsSourceExceptions(root: File): Map<String, String> =
        File(root, SAME_AS_SOURCE_FILE)
            .readLines()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .associate { it.substringBefore(REASON_SEPARATOR).trim() to it.substringAfter(REASON_SEPARATOR, "").trim() }
}
