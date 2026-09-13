/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULiteralExpression

/**
 * Keeps user-facing text out of Kotlin (docs/PLAN.md §0.2, T-204):
 * - [NON_LATIN_TEXT] — string literals containing non-Latin letters or digits (e.g. Persian text);
 * - [COMPOSE_TEXT] — literal strings passed as Compose `Text` content or a `contentDescription`.
 */
class HardcodedTextDetector :
    Detector(),
    SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(ULiteralExpression::class.java, UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitLiteralExpression(node: ULiteralExpression) {
                val value = node.value as? String ?: return
                if (value.codePoints().anyMatch(::isNonLatinLetterOrDigit)) {
                    context.report(NON_LATIN_TEXT, node, context.getLocation(node), NON_LATIN_MESSAGE)
                }
            }

            override fun visitCallExpression(node: UCallExpression) {
                val call = node.sourcePsi as? KtCallExpression ?: return
                call.valueArguments
                    .filter { isUserFacingArgument(node.methodName, call, it) }
                    .mapNotNull { it.getArgumentExpression() as? KtStringTemplateExpression }
                    .filter { !it.hasInterpolation() && literalText(it).isNotBlank() }
                    .forEach { context.report(COMPOSE_TEXT, node, context.getLocation(it), COMPOSE_TEXT_MESSAGE) }
            }
        }

    private fun literalText(literal: KtStringTemplateExpression): String =
        literal.entries.joinToString("") { entry -> entry.text }

    private fun isUserFacingArgument(
        methodName: String?,
        call: KtCallExpression,
        argument: KtValueArgument,
    ): Boolean {
        val name = argument.getArgumentName()?.asName?.identifier
        return if (name != null) {
            name in USER_FACING_PARAMETERS
        } else {
            methodName in TEXT_COMPOSABLES && call.valueArguments.firstOrNull() == argument
        }
    }

    companion object {
        private const val NON_LATIN_MESSAGE = "User-facing text must come from string resources (`strings.xml`)"
        private const val COMPOSE_TEXT_MESSAGE = "Hard-coded UI text; use `stringResource(R.string.…)`"
        private val TEXT_COMPOSABLES = setOf("Text", "BasicText")
        private val USER_FACING_PARAMETERS = setOf("text", "contentDescription")
        private val NEUTRAL_SCRIPTS =
            setOf(Character.UnicodeScript.LATIN, Character.UnicodeScript.COMMON, Character.UnicodeScript.INHERITED)

        private fun isNonLatinLetterOrDigit(codePoint: Int): Boolean =
            Character.isLetterOrDigit(codePoint) && Character.UnicodeScript.of(codePoint) !in NEUTRAL_SCRIPTS

        /** The `NoHardcodedNonLatinText` issue. */
        val NON_LATIN_TEXT: Issue =
            taqvimIssue(
                id = "NoHardcodedNonLatinText",
                brief = "Hard-coded non-Latin text",
                explanation =
                    "Persian and other non-Latin text in Kotlin is almost always user-facing and escapes " +
                        "translation. Put it in `strings.xml`; genuine data tables must be suppressed explicitly.",
                detector = HardcodedTextDetector::class.java,
                category = Category.I18N,
            )

        /** The `HardcodedComposeText` issue. */
        val COMPOSE_TEXT: Issue =
            taqvimIssue(
                id = "HardcodedComposeText",
                brief = "Hard-coded Compose text",
                explanation =
                    "Text shown by `Text` or announced through `contentDescription` must be localizable, so it " +
                        "has to come from string resources.",
                detector = HardcodedTextDetector::class.java,
                category = Category.I18N,
            )
    }
}
