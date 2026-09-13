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
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.uast.UBinaryExpressionWithType
import org.jetbrains.uast.UElement

/** Forbids the unsafe cast operator `as`; `as?` with explicit handling is allowed (docs/PLAN.md §0.2). */
class NoUnsafeCastDetector :
    Detector(),
    SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UBinaryExpressionWithType::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitBinaryExpressionWithType(node: UBinaryExpressionWithType) {
                val cast = node.sourcePsi as? KtBinaryExpressionWithTypeRHS ?: return
                if (cast.operationReference.text == "as") {
                    context.report(ISSUE, node, context.getLocation(cast.operationReference), MESSAGE)
                }
            }
        }

    companion object {
        private const val MESSAGE = "Do not use the unsafe cast `as`; use `as?` and handle the mismatch"

        /** The `NoUnsafeCast` issue. */
        val ISSUE: Issue =
            taqvimIssue(
                id = "NoUnsafeCast",
                brief = "Unsafe cast",
                explanation =
                    "`as` throws `ClassCastException` at runtime. Use `as?` with `?:` or a `when` type check so " +
                        "the mismatch is handled.",
                detector = NoUnsafeCastDetector::class.java,
            )
    }
}
