/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.uitesting

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1700: the audit finds each planted violation, ignores hidden and accepted nodes, and passes clean screens. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccessibilityAuditTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun violationsOf(
        options: AccessibilityOptions = AccessibilityOptions(),
        content: @Composable () -> Unit,
    ): List<AccessibilityViolation> {
        composeRule.setContent(content)
        composeRule.waitForIdle()
        val root = composeRule.onRoot().fetchSemanticsNode()
        return AccessibilityAudit.audit(root, composeRule.density, options)
    }

    private fun rulesOf(
        options: AccessibilityOptions = AccessibilityOptions(),
        content: @Composable () -> Unit,
    ): List<AccessibilityRule> = violationsOf(options, content).map { it.rule }

    @Test
    fun cleanContentHasNoViolations() {
        val rules =
            rulesOf {
                Column {
                    Text("Heading")
                    Box(Modifier.size(56.dp).clickable(onClickLabel = "Open") {}) { Text("Today") }
                    Row(Modifier.size(width = 160.dp, height = 48.dp).toggleable(value = true, onValueChange = {})) {
                        Checkbox(checked = true, onCheckedChange = null)
                        Text("Notify")
                    }
                    Box(
                        Modifier.size(24.dp).semantics {
                            contentDescription = "Moon"
                            role = Role.Image
                        },
                    )
                }
            }
        assertEquals(emptyList<AccessibilityRule>(), rules)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun plantedViolationsAreFound() {
        val rules =
            rulesOf {
                Column {
                    Box(Modifier.size(56.dp).clickable {})
                    Box(
                        Modifier.size(24.dp).semantics {
                            contentDescription = ""
                            role = Role.Image
                        },
                    )
                    // Semantics-only action: no pointer input, so no touch target expansion.
                    Box(
                        Modifier.size(20.dp).semantics {
                            contentDescription = "Tiny"
                            onClick { true }
                        },
                    )
                    Box(
                        Modifier
                            .size(56.dp)
                            .combinedClickable(onLongClick = {}) {}
                            .semantics { contentDescription = "Day" },
                    )
                    Box(Modifier.size(56.dp).semantics { stateDescription = " " })
                }
            }
        assertEquals(
            setOf(
                AccessibilityRule.UNLABELED_ACTION,
                AccessibilityRule.BLANK_LABEL,
                AccessibilityRule.UNLABELED_IMAGE,
                AccessibilityRule.SMALL_TOUCH_TARGET,
                AccessibilityRule.UNLABELED_LONG_CLICK,
            ),
            rules.toSet(),
        )
    }

    @Test
    fun aSmallTargetIsFineAloneButNotWhenItsWidenedTargetOverlapsANeighbour() {
        val alone = rulesOf { Box(Modifier.size(20.dp).clickable {}.semantics { contentDescription = "Dot" }) }
        assertEquals(emptyList<AccessibilityRule>(), alone)
    }

    @Test
    fun nestedTargetsDoNotCrowdEachOther() {
        val rules =
            violationsOf {
                Column {
                    Box(Modifier.size(120.dp).clickable {}.semantics { contentDescription = "Card" }) {
                        Box(Modifier.size(20.dp).clickable {}.semantics { contentDescription = "Pin" })
                    }
                    Spacer(Modifier.size(48.dp))
                    Box(Modifier.size(30.dp).clickable {}.semantics { contentDescription = "Chip" }) {
                        Box(Modifier.size(20.dp).clickable {}.semantics { contentDescription = "Close" })
                    }
                    Spacer(Modifier.size(48.dp))
                    Box(Modifier.size(width = 40.dp, height = 0.dp).clickable {}) { Text("Folded") }
                }
            }
        assertEquals(emptyList<AccessibilityViolation>(), rules)
    }

    @Test
    fun adjacentSmallTargetsAreBothReported() {
        val rules =
            rulesOf {
                Row {
                    Box(Modifier.size(20.dp).clickable {}.semantics { contentDescription = "Minus" })
                    Box(Modifier.size(20.dp).clickable {}.semantics { contentDescription = "Plus" })
                }
            }
        assertEquals(listOf(AccessibilityRule.SMALL_TOUCH_TARGET, AccessibilityRule.SMALL_TOUCH_TARGET), rules)
    }

    @Test
    fun aSmallTargetPartlyScrolledOutOfViewIsNotReported() {
        val rules =
            rulesOf {
                Box(Modifier.size(width = 200.dp, height = 120.dp)) {
                    Column(Modifier.size(width = 200.dp, height = 50.dp).verticalScroll(rememberScrollState())) {
                        Spacer(Modifier.size(40.dp))
                        Box(
                            Modifier.size(width = 60.dp, height = 20.dp).clickable {}.semantics {
                                contentDescription = "Chip"
                            },
                        )
                        Spacer(Modifier.size(100.dp))
                    }
                    Box(
                        Modifier
                            .offset(y = 50.dp)
                            .size(width = 120.dp, height = 48.dp)
                            .clickable {}
                            .semantics { contentDescription = "Save" },
                    )
                }
            }
        assertEquals(emptyList<AccessibilityRule>(), rules)
    }

    @Test
    fun siblingsWithTheSameLabelAreReportedAndHiddenOnesAreSkipped() {
        val rules =
            rulesOf {
                Column {
                    Box(Modifier.size(56.dp).clickable {}) { Text("Remove") }
                    Box(Modifier.size(56.dp).clickable {}) { Text("Remove") }
                    Box(Modifier.size(10.dp).clickable {}.semantics { hideFromAccessibility() })
                    Box(Modifier.size(10.dp).clearAndSetSemantics { hideFromAccessibility() }.clickable {})
                }
            }
        assertEquals(listOf(AccessibilityRule.DUPLICATE_LABEL, AccessibilityRule.DUPLICATE_LABEL), rules)
    }

    @Test
    fun acceptedViolationsAreIgnored() {
        val options = AccessibilityOptions(ignored = { it.rule == AccessibilityRule.UNLABELED_ACTION })
        val rules = rulesOf(options) { Box(Modifier.size(56.dp).clickable {}) }
        assertTrue(rules.isEmpty())
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun labeledControlsOfEveryKindPass() {
        val rules =
            rulesOf {
                var text by remember { mutableStateOf("") }
                Column {
                    Box(
                        Modifier.size(width = 200.dp, height = 48.dp).semantics {
                            contentDescription = "Volume"
                            setProgress { true }
                        },
                    )
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Name") })
                    Box(
                        Modifier
                            .size(56.dp)
                            .combinedClickable(onLongClickLabel = "Add event", onLongClick = {}) {}
                            .semantics { stateDescription = "Selected" },
                    ) { Text("12") }
                    Box(Modifier.size(0.dp).clickable {}) { Text("Collapsed") }
                }
            }
        assertEquals(emptyList<AccessibilityRule>(), rules)
    }

    @Test
    fun assertAccessibleListsViolationsAndThePropertyIsOffByDefault() {
        composeRule.setContent { Box(Modifier.size(56.dp).clickable {}) }
        val failure = runCatching { composeRule.assertAccessible() }.exceptionOrNull()
        assertTrue(failure is AssertionError)
        assertTrue(failure?.message.orEmpty().contains("UNLABELED_ACTION"))
        assertFalse(AccessibilityAudit.isEnabled())
        composeRule.auditAccessibilityIfEnabled()
    }

    @Test
    fun theScreenshotHookAuditsOnlyWhenTheModuleEnablesIt() {
        composeRule.setContent { Box(Modifier.size(56.dp).clickable {}) { Text("Open") } }
        composeRule.assertAccessible()
        System.setProperty(AccessibilityAudit.ENABLED_PROPERTY, "true")
        val clean = runCatching { composeRule.auditAccessibilityIfEnabled() }
        System.clearProperty(AccessibilityAudit.ENABLED_PROPERTY)
        assertTrue(clean.isSuccess)
    }

    @Test
    fun theScreenshotHookFailsOnViolationsWhenEnabled() {
        composeRule.setContent { Box(Modifier.size(56.dp).clickable {}) }
        System.setProperty(AccessibilityAudit.ENABLED_PROPERTY, "true")
        val failure = runCatching { composeRule.auditAccessibilityIfEnabled() }.exceptionOrNull()
        System.clearProperty(AccessibilityAudit.ENABLED_PROPERTY)
        assertTrue(failure is AssertionError)
    }
}
