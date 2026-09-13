/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UPostfixExpression

/** Forbids the not-null assertion operator `!!` (docs/PLAN.md §0.2). */
class NoDoubleBangDetector :
    Detector(),
    SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UPostfixExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitPostfixExpression(node: UPostfixExpression) {
                if (node.operator.text == "!!") {
                    context.report(ISSUE, node, context.getLocation(node), MESSAGE)
                }
            }
        }

    companion object {
        private const val MESSAGE =
            "Do not use `!!`; handle absence explicitly (`?:`, `?.let`, or `requireNotNull` with a message)"

        /** The `NoDoubleBang` issue. */
        val ISSUE: Issue =
            taqvimIssue(
                id = "NoDoubleBang",
                brief = "Not-null assertion operator",
                explanation =
                    "`!!` turns a modelling gap into a crash. Taqvim models absence explicitly with nullable " +
                        "types, `Result` and sealed states.",
                detector = NoDoubleBangDetector::class.java,
            )
    }
}
