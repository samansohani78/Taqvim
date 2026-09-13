/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue

/** T-204: Persian completeness. Translations in these fixtures are Latin placeholders; only names matter. */
class MissingFarsiTranslationDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = MissingFarsiTranslationDetector()

    override fun getIssues(): List<Issue> = listOf(MissingFarsiTranslationDetector.ISSUE)

    private fun check(vararg files: TestFile): TestLintResult = lint().files(*files).allowMissingSdk().run()

    private val defaults =
        xml(
            "res/values/strings.xml",
            """
            <resources>
                <string name="app_name">Taqvim</string>
                <string name="today">Today</string>
                <string name="build_id" translatable="false">abc</string>
                <plurals name="days_left">
                    <item quantity="one">%d day left</item>
                    <item quantity="other">%d days left</item>
                </plurals>
                <string-array name="seasons">
                    <item>Spring</item>
                </string-array>
            </resources>
            """,
        ).indented()

    fun testUntranslatedResourcesAreReported() {
        check(
            defaults,
            xml(
                "res/values-fa/strings.xml",
                """
                <resources>
                    <string name="app_name">Taqvim-fa</string>
                </resources>
                """,
            ).indented(),
        ).expectErrorCount(3)
            .expectContains(MissingFarsiTranslationDetector.messageFor("today"))
            .expectContains(MissingFarsiTranslationDetector.messageFor("days_left"))
            .expectContains(MissingFarsiTranslationDetector.messageFor("seasons"))
    }

    fun testMissingPersianFolderReportsEveryTranslatableResource() {
        check(defaults).expectErrorCount(4).expectContains("[MissingFarsiTranslation]")
    }

    fun testCompletePersianIsClean() {
        check(
            defaults,
            xml(
                "res/values-fa/strings.xml",
                """
                <resources>
                    <string name="app_name">Taqvim-fa</string>
                    <string name="today">Today-fa</string>
                    <plurals name="days_left">
                        <item quantity="one">%d day-fa</item>
                        <item quantity="other">%d days-fa</item>
                    </plurals>
                    <string-array name="seasons">
                        <item>Spring-fa</item>
                    </string-array>
                </resources>
                """,
            ).indented(),
        ).expectClean()
    }

    fun testOtherLocalesMayBeIncomplete() {
        check(
            xml("res/values/strings.xml", """<resources><string name="today">Today</string></resources>"""),
            xml("res/values-fa/strings.xml", """<resources><string name="today">Today-fa</string></resources>"""),
            xml("res/values-de/strings.xml", """<resources></resources>"""),
        ).expectClean()
    }
}
