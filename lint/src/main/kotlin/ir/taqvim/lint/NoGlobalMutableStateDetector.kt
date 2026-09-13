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
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField

/**
 * Forbids global mutable state (docs/PLAN.md §0.2): top-level or `object`/`companion object` properties that
 * are `var`s or hold mutable containers, Compose state or flows. State belongs to ViewModels and repositories.
 */
class NoGlobalMutableStateDetector :
    Detector(),
    SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UField::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitField(node: UField) {
                val property = node.sourcePsi as? KtProperty ?: return
                val owner = globalOwner(property) ?: return
                val reason = mutableReason(property) ?: return
                val location = context.getLocation(property.nameIdentifier ?: property)
                context.report(ISSUE, node, location, "$owner property `${property.name}` $reason; $ADVICE")
            }
        }

    private fun globalOwner(property: KtProperty): String? =
        when {
            property.isTopLevel -> "Top-level"

            // Object literals (`object : Iterator<T> { … }`) are per-instance state, not globals.
            (property.containingClassOrObject as? KtObjectDeclaration)?.isObjectLiteral() == false -> "Object"

            else -> null
        }

    private fun mutableReason(property: KtProperty): String? {
        val source =
            listOfNotNull(property.typeReference?.text, property.initializer?.text, property.delegateExpression?.text)
                .joinToString(" ")
        return when {
            property.isVar -> "is a `var`"
            MUTABLE_HOLDER.containsMatchIn(source) -> "holds mutable state"
            else -> null
        }
    }

    companion object {
        private const val ADVICE = "move the state into a ViewModel or repository and expose it as a Flow"

        private val MUTABLE_HOLDER =
            Regex("""\b(mutable\w*Of|Mutable[A-Z]\w*|ArrayList|HashMap|HashSet|LinkedHashMap|Atomic[A-Z]\w*)\b""")

        /** The `NoGlobalMutableState` issue. */
        val ISSUE: Issue =
            taqvimIssue(
                id = "NoGlobalMutableState",
                brief = "Global mutable state",
                explanation =
                    "Global mutable state breaks unidirectional data flow, leaks across tests and processes, and " +
                        "is invisible to the UI. Own state in a ViewModel/repository and expose `StateFlow`.",
                detector = NoGlobalMutableStateDetector::class.java,
            )
    }
}
