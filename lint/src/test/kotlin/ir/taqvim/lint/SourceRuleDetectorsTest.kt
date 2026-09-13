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

/** Positive and negative cases for the Kotlin source rules: NoDoubleBang, NoUnsafeCast, try/catch rules. */
class SourceRuleDetectorsTest : LintDetectorTest() {
    override fun getDetector(): Detector = NoDoubleBangDetector()

    override fun getIssues(): List<Issue> = listOf(NoDoubleBangDetector.ISSUE)

    private fun check(
        detector: Detector,
        issues: List<Issue>,
        source: String,
    ): TestLintResult =
        lint()
            .detector(detector)
            .issues(*issues.toTypedArray())
            .files(kotlinFile(source))
            .allowMissingSdk()
            .run()

    private fun kotlinFile(source: String): TestFile = kotlin(source).indented()

    fun testDoubleBangIsReported() {
        check(
            NoDoubleBangDetector(),
            listOf(NoDoubleBangDetector.ISSUE),
            """
            package test.pkg

            fun length(text: String?): Int = text!!.length
            """,
        ).expectErrorCount(1).expectContains("[NoDoubleBang]")
    }

    fun testNullSafeAlternativesAreClean() {
        check(
            NoDoubleBangDetector(),
            listOf(NoDoubleBangDetector.ISSUE),
            """
            package test.pkg

            fun length(text: String?): Int = text?.length ?: requireNotNull(text) { "text" }.length
            fun negate(flag: Boolean): Boolean = !flag
            """,
        ).expectClean()
    }

    fun testUnsafeCastIsReportedButSafeCastIsNot() {
        check(
            NoUnsafeCastDetector(),
            listOf(NoUnsafeCastDetector.ISSUE),
            """
            package test.pkg

            fun unsafe(value: Any): String = value as String
            fun safe(value: Any): String = value as? String ?: ""
            fun check(value: Any): Boolean = value is String
            """,
        ).expectErrorCount(1).expectContains("[NoUnsafeCast]")
    }

    fun testTryStatementIsReported() {
        check(
            TryCatchDetector(),
            listOf(TryCatchDetector.NO_TRY_CATCH, TryCatchDetector.USE_RUN_CATCHING),
            """
            package test.pkg

            fun load() {
                try {
                    compute()
                } catch (e: IllegalStateException) {
                    println(e)
                }
                try {
                    compute()
                } finally {
                    println("done")
                }
            }

            fun compute(): Int = 1
            """,
        ).expectErrorCount(2).expectContains("[NoTryCatch]")
    }

    fun testTryExpressionSuggestsRunCatching() {
        check(
            TryCatchDetector(),
            listOf(TryCatchDetector.NO_TRY_CATCH, TryCatchDetector.USE_RUN_CATCHING),
            """
            package test.pkg

            fun load(): Int? = try { compute() } catch (e: IllegalStateException) { null }

            fun compute(): Int = 1
            """,
        ).expectErrorCount(1).expectContains("[UseRunCatching]")
    }

    fun testRunCatchingIsClean() {
        check(
            TryCatchDetector(),
            listOf(TryCatchDetector.NO_TRY_CATCH, TryCatchDetector.USE_RUN_CATCHING),
            """
            package test.pkg

            fun load(): Int? = runCatching { compute() }.getOrNull()

            fun compute(): Int = 1
            """,
        ).expectClean()
    }
}
