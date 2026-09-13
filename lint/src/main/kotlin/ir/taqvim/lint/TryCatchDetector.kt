/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UTryExpression
import org.jetbrains.uast.skipParenthesizedExprUp

/**
 * Enforces `runCatching`/`Result` instead of raw `try` (docs/PLAN.md §0.2):
 * - [USE_RUN_CATCHING] — a `try` used as a value; offers a quick fix for the `catch → null` idiom;
 * - [NO_TRY_CATCH] — every other `try` statement (try/catch and try/finally).
 */
class TryCatchDetector :
    Detector(),
    SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UTryExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitTryExpression(node: UTryExpression) {
                if (skipParenthesizedExprUp(node.uastParent) is UBlockExpression) {
                    context.report(NO_TRY_CATCH, node, context.getLocation(node), NO_TRY_CATCH_MESSAGE)
                } else {
                    val fix = runCatchingFix(node)
                    context.report(USE_RUN_CATCHING, node, context.getLocation(node), USE_RUN_CATCHING_MESSAGE, fix)
                }
            }
        }

    private fun runCatchingFix(node: UTryExpression): LintFix? {
        val tryBody =
            node.tryClause.sourcePsi
                ?.text
                ?.let(::blockContent)
        val catchBody =
            node.catchClauses
                .singleOrNull()
                ?.body
                ?.sourcePsi
                ?.text
                ?.let(::blockContent)
        if (tryBody.isNullOrEmpty() || node.finallyClause != null || catchBody != "null") return null
        return LintFix
            .create()
            .name("Replace with runCatching { … }.getOrNull()")
            .replace()
            .all()
            .with("runCatching { $tryBody }.getOrNull()")
            .reformat(true)
            .build()
    }

    private fun blockContent(text: String): String =
        text
            .trim()
            .removePrefix("{")
            .removeSuffix("}")
            .trim()

    companion object {
        private const val NO_TRY_CATCH_MESSAGE = "Do not use `try`; use `runCatching { }` and handle the `Result`"
        private const val USE_RUN_CATCHING_MESSAGE = "Use `runCatching { }` instead of a `try` expression"

        /** The `NoTryCatch` issue. */
        val NO_TRY_CATCH: Issue =
            taqvimIssue(
                id = "NoTryCatch",
                brief = "Raw try statement",
                explanation =
                    "Taqvim production code reports failures through `Result` (`runCatching`), so errors are " +
                        "values the caller must handle instead of hidden control flow.",
                detector = TryCatchDetector::class.java,
            )

        /** The `UseRunCatching` issue. */
        val USE_RUN_CATCHING: Issue =
            taqvimIssue(
                id = "UseRunCatching",
                brief = "try expression instead of runCatching",
                explanation =
                    "A `try` used as a value is exactly what `runCatching { … }` expresses: " +
                        "`try { x } catch (e: E) { null }` becomes `runCatching { x }.getOrNull()`.",
                detector = TryCatchDetector::class.java,
            )
    }
}
