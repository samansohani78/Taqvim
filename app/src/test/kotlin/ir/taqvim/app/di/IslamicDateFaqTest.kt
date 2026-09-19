/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.data.preferences.IslamicOverrideOrigin
import ir.taqvim.data.preferences.UserPreferences
import java.io.File
import org.junit.jupiter.api.Test

/**
 * REVIEW R17: the FAQ answer on Islamic dates must describe what the app does. Iranian Hijri dates are computed by
 * default and the officially announced months are an opt-in override (ADR-0037); the answer used to say the published
 * month starts were used. The answer and the setting live in different modules (`:feature:about`, `:feature:settings`),
 * so they are checked here, where both are merged: in every language the answer names the setting by its own label,
 * and the default really is "computed" for every launch language.
 */
class IslamicDateFaqTest {
    private val about = File("../feature/about/src/main/res")
    private val settings = File("../feature/settings/src/main/res")

    @Test
    fun `the official dates are an opt-in override for every launch language`() {
        val optedIn =
            LanguageTable.languages
                .map { it.code }
                .filter { UserPreferences.defaultsFor(it).islamicOverride.origin != IslamicOverrideOrigin.NONE }
        optedIn.shouldBeEmpty()
    }

    @Test
    fun `in every language the answer names the setting that turns the official dates on`() {
        val directories = about.listFiles().orEmpty().filter { it.name == "values" || it.name.startsWith("values-") }
        val mismatched =
            directories.mapNotNull { directory ->
                val answer = string(File(directory, STRINGS), ANSWER)
                val label = string(File(settings, "${directory.name}/$STRINGS"), SETTING)
                if (answer == null || label == null || label !in answer) directory.name else null
            }
        (directories.size >= LAUNCH_LANGUAGES) shouldBe true
        mismatched.shouldBeEmpty()
    }

    @Test
    fun `the answer no longer says the published month starts are used`() {
        string(File(about, "values/$STRINGS"), ANSWER)?.contains("uses the month starts published") shouldBe false
    }

    private fun string(
        file: File,
        name: String,
    ): String? =
        if (file.isFile) {
            Regex("""<string name="$name">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
                .find(file.readText())
                ?.groupValues
                ?.get(1)
                ?.replace("\\'", "'")
        } else {
            null
        }

    private companion object {
        const val STRINGS = "strings.xml"
        const val ANSWER = "about_faq_islamic_date_a"
        const val SETTING = "settings_item_islamic_override"
        const val LAUNCH_LANGUAGES = 24
    }
}
