/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import com.lemonappdev.konsist.api.Konsist
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** T-1702: structural translation quality rules on every module, and planted violations (docs/i18n/TRANSLATING.md). */
class TranslationKonsistTest {
    private val root = File(Konsist.projectRootPath)

    @Test
    fun `translations keep placeholders, plural categories, punctuation and control characters`() {
        val folders = TranslationCatalog.folders(root)
        val exceptions = LaunchLanguages.sameAsSourceExceptions(root).keys

        folders.shouldNotBeEmpty()
        TranslationChecks.placeholderMismatches(folders).shouldBeEmpty()
        TranslationChecks.pluralCategoryIssues(folders, LaunchLanguages::pluralQuantities).shouldBeEmpty()
        TranslationChecks.sameAsSource(folders, exceptions).shouldBeEmpty()
        TranslationChecks.rtlPunctuation(folders, LaunchLanguages.rightToLeft).shouldBeEmpty()
        TranslationChecks.bidiControls(folders).shouldBeEmpty()
    }

    @Test
    fun `every same-as-source exception names an existing translation and gives a reason`() {
        val translated = TranslationChecks.translated(TranslationCatalog.folders(root))
        val existing = translated.map { "${it.folder.resDir}: ${it.name}" }.toSet()
        val exceptions = LaunchLanguages.sameAsSourceExceptions(root)

        exceptions.filterValues(String::isBlank).keys.shouldBeEmpty()
        (exceptions.keys - existing).shouldBeEmpty()
    }

    @Test
    fun `debug builds include the pseudo-locales`() {
        File(root, "app/build.gradle.kts").readText() shouldContain "isPseudoLocalesEnabled = true"
    }

    @Test
    fun `escaped percent signs are literal text, not placeholders`() {
        TranslationChecks.placeholders("Lit %1\$s%%") shouldBe listOf("%1\$s")
        TranslationChecks.placeholders("%1\$s٪ روشن") shouldBe listOf("%1\$s")
        TranslationChecks.placeholders("100%% l") shouldBe emptyList()
        TranslationChecks.placeholders("%% %d %2\$s") shouldBe listOf("%2\$s", "%d")
    }

    @Test
    fun `folder qualifiers map to languages`() {
        TranslationCatalog.languageOf("values") shouldBe ""
        TranslationCatalog.languageOf("values-fa") shouldBe "fa"
        TranslationCatalog.languageOf("values-fa-rIR") shouldBe "fa"
        TranslationCatalog.languageOf("values-b+az+Latn") shouldBe "az"
        TranslationCatalog.languageOf("values-b+ckb") shouldBe "ckb"
        listOf("values-night", "values-v28", "values-land", "values-car", "values-sw600dp").forEach {
            TranslationCatalog.languageOf(it) shouldBe null
        }
    }

    @Test
    fun `planted violations are reported`(
        @TempDir tree: File,
    ) {
        plantedTree(tree)
        val folders = TranslationCatalog.folders(tree)
        val res = "feature/month/src/main/res"

        TranslationChecks.placeholderMismatches(folders) shouldBe
            listOf(
                "$res [fa] count: placeholders [%1\$s] but source has [%1\$d]",
                "$res [fa] days: 'other' placeholders differ from the source [%1\$d]",
                "$res [fa] items: 1 items but source has 2",
            )
        TranslationChecks.pluralCategoryIssues(folders, LaunchLanguages::pluralQuantities) shouldBe
            listOf(
                "$res [fa] days: missing plural categories [one]",
                "$res [zh] days: plural categories [one] are never selected in this language",
            )
        TranslationChecks.sameAsSource(folders, emptySet()) shouldBe
            listOf("$res [fa] title: identical to the English source")
        TranslationChecks.sameAsSource(folders, setOf("$res: title")).shouldBeEmpty()
        TranslationChecks.rtlPunctuation(folders, LaunchLanguages.rightToLeft) shouldBe
            listOf(
                "$res [fa] brand: ASCII ',' in right-to-left text",
                "$res [fa] query: ASCII '?' in right-to-left text",
            )
        TranslationChecks.bidiControls(folders) shouldBe listOf("$res [fa] mark: bidi control")
    }

    @Test
    fun `translator comments, types and launch-language facts are read`(
        @TempDir tree: File,
    ) {
        plantedTree(tree)
        val source = TranslationCatalog.folders(tree).first { it.language == null }

        (source.entries.getValue("title") as StringEntry.Single).hasComment shouldBe true
        (source.entries.getValue("count") as StringEntry.Single).hasComment shouldBe false
        (source.entries.getValue("days") as StringEntry.Plural).items.keys shouldBe setOf("one", "other")
        source.entries.keys.contains("fixed") shouldBe false
        LaunchLanguages.rightToLeft shouldBe setOf("fa", "prs", "ps", "ar", "ckb", "ur")
        LaunchLanguages.pluralQuantities("ar").orEmpty() shouldContainAll listOf("zero", "one", "two", "few", "many")
        LaunchLanguages.pluralQuantities("xx") shouldBe null
    }

    companion object {
        /** A module with English, Persian and Chinese resources containing one violation of every rule. */
        fun plantedTree(tree: File) {
            write(
                tree,
                "values",
                """<!-- Title of the month screen --><string name="title">Calendar</string>""" +
                    """<string name="count">%1${'$'}d days</string>""" +
                    """<string name="fixed" translatable="false">X</string>""" +
                    """<plurals name="days"><item quantity="one">%1${'$'}d day</item>""" +
                    """<item quantity="other">%1${'$'}d days</item></plurals>""" +
                    """<string-array name="items"><item>A %1${'$'}s</item><item>B</item></string-array>""" +
                    """<string name="brand">Taqvim, today</string><string name="mark">Hello</string>""" +
                    """<string name="query">Today?</string><string name="version">Version %1${'$'}s, beta</string>""",
            )
            write(
                tree,
                "values-fa",
                """<string name="title">Calendar</string><string name="count">%1${'$'}s روز</string>""" +
                    """<plurals name="days"><item quantity="other">%1${'$'}d روز %2${'$'}s</item></plurals>""" +
                    """<string-array name="items"><item>الف %1${'$'}s</item></string-array>""" +
                    """<string name="brand">تقویم, امروز</string><string name="mark">${'\u200F'}سلام</string>""" +
                    """<string name="query">امروز?</string>""" +
                    """<string name="version">نسخهٔ %1${'$'}s، beta, 1.0</string>""",
            )
            write(
                tree,
                "values-zh",
                """<plurals name="days"><item quantity="one">一天</item>""" +
                    """<item quantity="other">%1${'$'}d 天</item></plurals>""",
            )
            write(tree, "values-night", """<string name="title">Night</string>""")
        }

        private fun write(
            tree: File,
            folder: String,
            body: String,
        ) {
            val file = File(tree, "feature/month/src/main/res/$folder/strings.xml")
            file.parentFile.mkdirs()
            file.writeText("<resources>$body</resources>")
        }
    }
}
