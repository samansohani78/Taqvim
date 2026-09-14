/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.uitesting

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Accessibility rules checked on the merged semantics tree (T-1700). */
enum class AccessibilityRule {
    /** A clickable, long-clickable, toggleable, adjustable or editable node has nothing TalkBack can read. */
    UNLABELED_ACTION,

    /** A content description or state description exists but is blank. */
    BLANK_LABEL,

    /** A node announced as an image has no description. */
    UNLABELED_IMAGE,

    /** An interactive node is smaller than the minimum touch target in width or height. */
    SMALL_TOUCH_TARGET,

    /** A long-press action has no label, so TalkBack can only say "double-tap and hold". */
    UNLABELED_LONG_CLICK,

    /** Sibling actions share label and state, so a TalkBack user cannot tell them apart. */
    DUPLICATE_LABEL,
}

/** One finding: the [rule], the spoken [label] of the node (may be empty) and its [bounds] in the root. */
data class AccessibilityViolation(
    val rule: AccessibilityRule,
    val label: String,
    val bounds: Rect,
) {
    override fun toString(): String = "$rule '$label' at $bounds"
}

/**
 * Audit settings. [minTouchTarget] follows the Material and Android accessibility guidance (48 dp); [ignored] lets a
 * test accept a documented exception, e.g. a dense grid whose cells also offer custom actions on the container.
 */
data class AccessibilityOptions(
    val minTouchTarget: Dp = MIN_TOUCH_TARGET,
    val ignored: (AccessibilityViolation) -> Boolean = { false },
) {
    private companion object {
        val MIN_TOUCH_TARGET = 48.dp
    }
}

/** Walks the merged semantics tree and reports [AccessibilityRule] violations. */
object AccessibilityAudit {
    /** System property that turns the audit on inside [captureScreenshot] for a module's tests. */
    const val ENABLED_PROPERTY: String = "taqvim.a11y.audit"

    private const val TOLERANCE_PX = 0.5f

    /** Whether [ENABLED_PROPERTY] is set to `true` for this test JVM. */
    fun isEnabled(): Boolean = System.getProperty(ENABLED_PROPERTY).toBoolean()

    /** Every violation under [root], after [AccessibilityOptions.ignored]. Hidden subtrees are skipped. */
    fun audit(
        root: SemanticsNode,
        density: Density,
        options: AccessibilityOptions = AccessibilityOptions(),
    ): List<AccessibilityViolation> {
        val found = mutableListOf<AccessibilityViolation>()
        visit(root) { node ->
            found += nodeViolations(node, density, options)
            found += duplicateLabels(node.children)
        }
        return found.filterNot(options.ignored)
    }

    private fun visit(
        node: SemanticsNode,
        action: (SemanticsNode) -> Unit,
    ) {
        if (node.isHidden()) return
        action(node)
        node.children.forEach { visit(it, action) }
    }

    private fun nodeViolations(
        node: SemanticsNode,
        density: Density,
        options: AccessibilityOptions,
    ): List<AccessibilityViolation> {
        val label = node.spokenLabel()
        val interactive = node.isInteractive()
        val rules =
            buildList {
                if (node.hasBlankLabel()) add(AccessibilityRule.BLANK_LABEL)
                if (interactive && label.isBlank()) add(AccessibilityRule.UNLABELED_ACTION)
                if (node.isUndescribedImage()) add(AccessibilityRule.UNLABELED_IMAGE)
                if (interactive && node.isSmall(density, options.minTouchTarget)) {
                    add(AccessibilityRule.SMALL_TOUCH_TARGET)
                }
                if (node.hasUnlabeledLongClick()) add(AccessibilityRule.UNLABELED_LONG_CLICK)
            }
        return rules.map { AccessibilityViolation(it, label, node.boundsInRoot) }
    }

    private fun SemanticsNode.descriptions(): List<String> =
        config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()

    private fun SemanticsNode.hasBlankLabel(): Boolean =
        descriptions().any { it.isBlank() } ||
            config.getOrNull(SemanticsProperties.StateDescription)?.isBlank() == true

    private fun SemanticsNode.isUndescribedImage(): Boolean =
        config.getOrNull(SemanticsProperties.Role) == Role.Image && descriptions().all { it.isBlank() }

    /** Text fields expose an unlabeled long-press for text selection that TalkBack already explains. */
    private fun SemanticsNode.hasUnlabeledLongClick(): Boolean {
        val longClick = config.getOrNull(SemanticsActions.OnLongClick) ?: return false
        return longClick.label.isNullOrBlank() && !config.contains(SemanticsActions.SetText)
    }

    private fun duplicateLabels(siblings: List<SemanticsNode>): List<AccessibilityViolation> =
        siblings
            .filter { it.isInteractive() && !it.isHidden() && it.spokenLabel().isNotBlank() }
            .groupBy { node ->
                listOf(
                    node.spokenLabel(),
                    node.config.getOrNull(SemanticsProperties.StateDescription),
                    node.config.getOrNull(SemanticsProperties.Selected),
                    node.config.getOrNull(SemanticsProperties.ToggleableState),
                )
            }.values
            .filter { it.size > 1 }
            .flatten()
            .map { AccessibilityViolation(AccessibilityRule.DUPLICATE_LABEL, it.spokenLabel(), it.boundsInRoot) }

    private fun SemanticsNode.isHidden(): Boolean = config.contains(SemanticsProperties.HideFromAccessibility)

    private fun SemanticsNode.isInteractive(): Boolean =
        config.contains(SemanticsActions.OnClick) ||
            config.contains(SemanticsActions.OnLongClick) ||
            config.contains(SemanticsProperties.ToggleableState) ||
            config.contains(SemanticsActions.SetProgress) ||
            config.contains(SemanticsActions.SetText)

    private fun SemanticsNode.spokenLabel(): String {
        val texts = config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        return (descriptions() + texts).filter { it.isNotBlank() }.joinToString(" ").trim()
    }

    /**
     * Compares the laid-out size, not the touch bounds: Compose widens the touch bounds of every interactive node to
     * 48 dp, but widened targets of small neighbours overlap, so the visible target itself must reach the minimum.
     */
    private fun SemanticsNode.isSmall(
        density: Density,
        minimum: Dp,
    ): Boolean {
        if (size.width == 0 || size.height == 0) return false
        val minimumPx = with(density) { minimum.toPx() } - TOLERANCE_PX
        return size.width < minimumPx || size.height < minimumPx
    }
}

/** Runs [assertAccessible] when [AccessibilityAudit.isEnabled]; called by [captureScreenshot] for every state. */
fun ComposeContentTestRule.auditAccessibilityIfEnabled(options: AccessibilityOptions = AccessibilityOptions()) {
    if (AccessibilityAudit.isEnabled()) assertAccessible(options)
}

/**
 * Fails with every [AccessibilityViolation] found in all composition roots (dialogs and popups included).
 * Used by [captureScreenshot] when [AccessibilityAudit.isEnabled] and directly by screen tests.
 */
fun ComposeContentTestRule.assertAccessible(options: AccessibilityOptions = AccessibilityOptions()) {
    waitForIdle()
    val violations =
        onAllNodes(isRoot()).fetchSemanticsNodes().flatMap { root ->
            AccessibilityAudit.audit(root, density, options)
        }
    if (violations.isNotEmpty()) {
        throw AssertionError("Accessibility violations (T-1700):\n" + violations.joinToString("\n"))
    }
}
