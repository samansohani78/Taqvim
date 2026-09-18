/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import ir.taqvim.core.i18n.LanguageTable
import java.io.File
import java.util.Locale
import org.junit.jupiter.api.Test

/**
 * T-1800: `androidResources.localeFilters` in `app/build.gradle.kts` drops every locale that is not a launch
 * language from the resource table. A language added to [LanguageTable] without its filter would silently lose the
 * library translations that go with it, so the list is checked against the table here rather than in a release build.
 */
class LocaleFiltersTest {
    private val filters: Set<String> =
        FILTER_LIST
            .find(File("build.gradle.kts").readText())
            ?.groupValues
            ?.get(1)
            ?.let { list -> QUOTED.findAll(list).map { it.groupValues[1] }.toSet() }
            .orEmpty()

    @Test
    fun `every launch language is kept by a locale filter`() {
        filters.shouldContain("en")
        val missing =
            LanguageTable.languages.filterNot { language ->
                val locale = Locale.forLanguageTag(language.localeTag)
                val script = locale.script.takeIf { it.isNotEmpty() }
                val qualifiers =
                    listOfNotNull(
                        legacy(locale.language),
                        script?.let { "b+${legacy(locale.language)}+$it" },
                        locale.country
                            .takeIf { it.isNotEmpty() && script == null }
                            ?.let { "${legacy(locale.language)}-r$it" },
                    )
                qualifiers.any { it in filters }
            }
        missing.map { it.localeTag }.shouldBeEmpty()
    }

    @Test
    fun `no filter names a locale the app does not offer`() {
        val offered =
            LanguageTable.languages
                .map { legacy(Locale.forLanguageTag(it.localeTag).language) }
                .toSet()
        val strays = filters.filterNot { filter -> language(filter) in offered }
        strays.shouldBeEmpty()
    }

    private fun language(filter: String): String =
        if (filter.startsWith("b+")) filter.removePrefix("b+").substringBefore('+') else filter.substringBefore('-')

    /** Android resource qualifiers keep the obsolete ISO codes: Indonesian is `in`, Hebrew `iw`, Yiddish `ji`. */
    private fun legacy(language: String): String = LEGACY_CODES[language] ?: language

    private companion object {
        val FILTER_LIST = Regex("""val launchLocaleFilters\s*=\s*\n?\s*setOf\(([^)]*)\)""")
        val QUOTED = Regex(""""([^"]+)"""")
        val LEGACY_CODES = mapOf("id" to "in", "he" to "iw", "yi" to "ji")
    }
}
