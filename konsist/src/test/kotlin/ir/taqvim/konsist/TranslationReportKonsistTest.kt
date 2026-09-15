/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import com.lemonappdev.konsist.api.Konsist
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * T-1702: translation completeness per language and module. Writes `konsist/build/reports/translations/` (run alone
 * with `./gradlew :konsist:translationReport`); English is the source and Persian must be complete.
 */
class TranslationReportKonsistTest {
    @Test
    fun `writes the completeness report with English and Persian complete`() {
        val root = File(Konsist.projectRootPath)
        val folders = TranslationCatalog.folders(root)
        val languages = TranslationReport.allLanguages(folders, LaunchLanguages.codes)
        val rows = TranslationReport.completeness(folders, languages)
        val output = File(root, "konsist/build/reports/translations").apply { mkdirs() }

        File(output, "translations.md").writeText(TranslationReport.markdown(rows, languages))
        File(output, "translations.json").writeText(TranslationReport.json(rows, languages))

        rows.shouldNotBeEmpty()
        TranslationReport.percent(rows, "en") shouldBe 100.0
        TranslationReport.percent(rows, "fa") shouldBe 100.0
    }

    @Test
    fun `counts translations, comments and short uncommented strings per module`(
        @TempDir tree: File,
    ) {
        TranslationKonsistTest.plantedTree(tree)
        val folders = TranslationCatalog.folders(tree)
        val languages = TranslationReport.allLanguages(folders, listOf("en", "fa", "ar"))
        val rows = TranslationReport.completeness(folders, languages)

        languages shouldBe listOf("en", "fa", "ar", "zh")
        rows shouldBe
            listOf(
                ModuleCompleteness(
                    module = "feature/month",
                    sourceCount = 8,
                    translated = mapOf("en" to 8, "fa" to 8, "ar" to 0, "zh" to 1),
                    withComment = 1,
                    shortWithoutComment = 3,
                ),
            )
        TranslationReport.percent(rows, "zh") shouldBe 12.5
        TranslationReport.moduleLabel("app/src/debug/res") shouldBe "app (debug)"
        TranslationReport.moduleLabel("wear/src/main/res") shouldBe "wear"
        TranslationReport.percent(emptyList(), "fa") shouldBe 100.0
        TranslationReport.markdown(rows, languages).let {
            it shouldContain "| zh | 1 | 8 | 12.5 % |"
            it shouldContain "| feature/month | 8 | 8 | 8 | 1 | 1 | 3 |"
        }
        TranslationReport.json(rows, languages) shouldContain
            "\"modules\":[{\"module\":\"feature/month\",\"source\":8," +
            "\"translated\":{\"en\":8,\"fa\":8,\"ar\":0,\"zh\":1}"
    }
}
