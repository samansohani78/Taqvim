/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.uitesting

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.text.TextLayoutResult

/** Layout rules checked on the unmerged semantics tree for RTL and large font scales (T-1701). */
enum class LayoutRule {
    /** Text did not fit its layout: it was clipped, drawn past its bounds or ellipsized. */
    TEXT_TRUNCATED,

    /** A node reaches past the left or right edge of its window outside any horizontally scrollable container. */
    OUT_OF_WINDOW,
}

/** One finding: the [rule], the [text] of the node (may be empty) and its [bounds] in the root. */
data class LayoutViolation(
    val rule: LayoutRule,
    val text: String,
    val bounds: Rect,
) {
    override fun toString(): String = "$rule '$text' at $bounds"
}

/** Audit settings; [ignored] lets a test accept a documented exception (e.g. a user title kept on one line). */
data class LayoutOptions(
    val ignored: (LayoutViolation) -> Boolean = { false },
)

/**
 * Finds text that does not fit and content pushed off screen. Text truncation comes from each text node's
 * [TextLayoutResult.hasVisualOverflow]; nodes whose semantics are cleared (e.g. a day cell that speaks one summary)
 * expose no text layout and must avoid truncation by design.
 */
object LayoutAudit {
    /** System property that turns the audit on inside [captureScreenshot] for a module's tests. */
    const val ENABLED_PROPERTY: String = "taqvim.layout.audit"

    private const val TOLERANCE_PX = 1f

    /** Whether [ENABLED_PROPERTY] is set to `true` for this test JVM. */
    fun isEnabled(): Boolean = System.getProperty(ENABLED_PROPERTY).toBoolean()

    /** Every violation under [root] (an unmerged root), after [LayoutOptions.ignored]. */
    fun audit(
        root: SemanticsNode,
        options: LayoutOptions = LayoutOptions(),
    ): List<LayoutViolation> {
        val window = root.boundsInRoot
        val found = mutableListOf<LayoutViolation>()
        visit(root, insideScroll = false) { node, insideScroll ->
            if (node.isTruncated()) found += LayoutViolation(LayoutRule.TEXT_TRUNCATED, node.text(), node.boundsInRoot)
            if (!insideScroll && node.isOutside(window)) {
                found += LayoutViolation(LayoutRule.OUT_OF_WINDOW, node.text(), node.boundsInRoot)
            }
        }
        return found.filterNot(options.ignored)
    }

    private fun visit(
        node: SemanticsNode,
        insideScroll: Boolean,
        action: (SemanticsNode, Boolean) -> Unit,
    ) {
        action(node, insideScroll)
        val scrolls = insideScroll || node.config.contains(SemanticsProperties.HorizontalScrollAxisRange)
        node.children.forEach { visit(it, scrolls, action) }
    }

    private fun SemanticsNode.isTruncated(): Boolean {
        val getLayout = config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: return false
        val layouts = mutableListOf<TextLayoutResult>()
        getLayout(layouts)
        return layouts.any { it.isCutOff() }
    }

    /**
     * Whether lines were dropped, ellipsized, or reach past the text's own size by more than a pixel. The layout's
     * `hasVisualOverflow` also reports sub-pixel differences for text sized to its own width, so it is not used.
     */
    private fun TextLayoutResult.isCutOff(): Boolean {
        val paragraph = multiParagraph
        val lines = 0 until lineCount
        val ellipsized = lines.any { isLineEllipsized(it) }
        val tooWide = lines.any { getLineRight(it) - getLineLeft(it) > size.width + TOLERANCE_PX }
        return ellipsized || tooWide || paragraph.didExceedMaxLines || paragraph.height > size.height + TOLERANCE_PX
    }

    private fun SemanticsNode.isOutside(window: Rect): Boolean {
        val bounds = boundsInRoot
        val visible = bounds.width > 0f && bounds.height > 0f
        return visible && (bounds.left < window.left - TOLERANCE_PX || bounds.right > window.right + TOLERANCE_PX)
    }

    private fun SemanticsNode.text(): String =
        (config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            config.getOrNull(SemanticsProperties.ContentDescription).orEmpty())
            .filter { it.isNotBlank() }
            .joinToString(" ")
}

/** Runs [assertLayoutFits] when [LayoutAudit.isEnabled]; called by [captureScreenshot] for every state. */
fun ComposeContentTestRule.auditLayoutIfEnabled(options: LayoutOptions = LayoutOptions()) {
    if (LayoutAudit.isEnabled()) assertLayoutFits(options)
}

/** Fails with every [LayoutViolation] found in all composition roots (dialogs and popups included). */
fun ComposeContentTestRule.assertLayoutFits(options: LayoutOptions = LayoutOptions()) {
    waitForIdle()
    val violations =
        onAllNodes(isRoot(), useUnmergedTree = true).fetchSemanticsNodes().flatMap { root ->
            LayoutAudit.audit(root, options)
        }
    if (violations.isNotEmpty()) {
        throw AssertionError("Layout violations (T-1701):\n" + violations.joinToString("\n"))
    }
}
