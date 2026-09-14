/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import java.util.Locale

/**
 * Maps a device or app locale to a launch language (T-1501, ADR-0023). The launch languages are ISO 639 codes whose
 * primary regions are in the language table (`fa-IR`, `prs` = `fa-AF`, `kmr` = `ku-TR`, `ckb` = `ckb-IQ`, …), while
 * Android reports BCP 47 tags such as `fa-AF` or `ku`; the best match is chosen in this order:
 * 1. a launch language whose locale has the tag's language and region (`fa-AF` → `prs`);
 * 2. the launch language whose code equals the tag's language (`fa-DE` → `fa`, `ckb` → `ckb`);
 * 3. the first launch language whose locale has the tag's language (`ku` → `kmr`).
 */
object DeviceLanguages {
    /** The launch language code for the BCP 47 [tag], or [UserPreferences.FALLBACK_LANGUAGE]. */
    fun match(tag: String): String = find(tag)?.code ?: UserPreferences.FALLBACK_LANGUAGE

    /** The launch language for the BCP 47 [tag], or `null` when none fits. */
    fun find(tag: String): LanguageSpec? {
        val locale = Locale.forLanguageTag(tag.replace('_', '-'))
        val language = locale.language
        if (language.isEmpty()) return null
        val region = locale.country
        val languages = LanguageTable.languages
        return languages.firstOrNull {
            it.primaryLanguage() == language && region.isNotEmpty() && it.region() == region
        }
            ?: languages.firstOrNull { it.code == language }
            ?: languages.firstOrNull { it.primaryLanguage() == language }
    }

    private fun LanguageSpec.primaryLanguage(): String = Locale.forLanguageTag(localeTag).language

    private fun LanguageSpec.region(): String = Locale.forLanguageTag(localeTag).country
}
