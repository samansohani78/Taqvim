/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.lint

import com.android.SdkConstants.ANDROID_URI
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import java.util.EnumSet
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.w3c.dom.Attr

/**
 * Requires predictive back (Android 13+ `OnBackInvokedCallback`, Compose back handlers): flags
 * `onBackPressed()` overrides and calls, manual `KEYCODE_BACK` handling, and manifests that opt out with
 * `android:enableOnBackInvokedCallback="false"`.
 */
class PreferPredictiveBackDetector :
    Detector(),
    SourceCodeScanner,
    XmlScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UMethod::class.java, UCallExpression::class.java, USimpleNameReferenceExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitMethod(node: UMethod) {
                val function = node.sourcePsi as? KtNamedFunction ?: return
                val isOverride = function.hasModifier(KtTokens.OVERRIDE_KEYWORD)
                if (node.name == BACK_PRESSED && node.uastParameters.isEmpty() && isOverride) {
                    report(context, node, context.getNameLocation(node), OVERRIDE_MESSAGE)
                }
            }

            override fun visitCallExpression(node: UCallExpression) {
                if (node.methodName == BACK_PRESSED && node.valueArgumentCount == 0) {
                    report(context, node, context.getLocation(node), CALL_MESSAGE)
                }
            }

            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                if (node.identifier == KEYCODE_BACK) {
                    report(context, node, context.getLocation(node), KEYCODE_MESSAGE)
                }
            }
        }

    override fun getApplicableAttributes(): Collection<String> = listOf(ENABLE_ON_BACK_INVOKED_CALLBACK)

    override fun visitAttribute(
        context: XmlContext,
        attribute: Attr,
    ) {
        if (attribute.namespaceURI == ANDROID_URI && attribute.value == "false") {
            context.report(ISSUE, attribute, context.getLocation(attribute), MANIFEST_MESSAGE)
        }
    }

    private fun report(
        context: JavaContext,
        node: UElement,
        location: Location,
        message: String,
    ) = context.report(ISSUE, node, location, message)

    companion object {
        private const val BACK_PRESSED = "onBackPressed"
        private const val KEYCODE_BACK = "KEYCODE_BACK"
        private const val ENABLE_ON_BACK_INVOKED_CALLBACK = "enableOnBackInvokedCallback"
        private const val OVERRIDE_MESSAGE =
            "Do not override `onBackPressed()`; register an `OnBackPressedCallback` or use a Compose back handler"
        private const val CALL_MESSAGE =
            "Do not call `onBackPressed()`; dispatch through `onBackPressedDispatcher` so predictive back works"
        private const val KEYCODE_MESSAGE = "Do not handle `KEYCODE_BACK` manually; it bypasses predictive back"
        private const val MANIFEST_MESSAGE = "Do not opt out of predictive back (`enableOnBackInvokedCallback`)"

        /** The `PreferPredictiveBack` issue. */
        val ISSUE: Issue =
            taqvimIssue(
                id = "PreferPredictiveBack",
                brief = "Legacy back handling",
                explanation =
                    "Predictive back animates the destination before the gesture completes. Legacy " +
                        "`onBackPressed`/`KEYCODE_BACK` handling and opting out in the manifest break it.",
                implementation =
                    Implementation(
                        PreferPredictiveBackDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.MANIFEST),
                        Scope.JAVA_FILE_SCOPE,
                        Scope.MANIFEST_SCOPE,
                    ),
                category = Category.USABILITY,
            )
    }
}
