/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.uitesting

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1701: the layout audit finds truncated text and content pushed off screen, and passes content that fits. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayoutAuditTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val long = "A long label that cannot fit into a narrow box on one line"

    private fun rulesOf(
        options: LayoutOptions = LayoutOptions(),
        content: @Composable () -> Unit,
    ): List<LayoutRule> {
        composeRule.setContent(content)
        composeRule.waitForIdle()
        val root = composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        return LayoutAudit.audit(root, options).map { it.rule }
    }

    @Test
    fun contentThatFitsHasNoViolations() {
        val rules =
            rulesOf {
                Column(Modifier.fillMaxWidth()) {
                    Text("Short")
                    Text(long)
                    val shrink = TextAutoSize.StepBased(minFontSize = 2.sp)
                    Text(long, Modifier.width(120.dp), maxLines = 1, autoSize = shrink)
                    OutlinedTextField(value = long, onValueChange = {}, singleLine = true)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        Text("First", Modifier.requiredWidth(400.dp))
                        Text("Beyond the edge", Modifier.requiredWidth(400.dp))
                        Text("ساعت‌ها و روزها", style = TextStyle(textDirection = TextDirection.Rtl), maxLines = 1)
                    }
                }
            }
        assertEquals(emptyList<LayoutRule>(), rules)
    }

    @Test
    fun ellipsizedAndClippedTextIsTruncated() {
        val rules =
            rulesOf {
                Column {
                    Text(long, Modifier.width(60.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(Modifier.width(60.dp).height(12.dp)) {
                        Text(long, overflow = TextOverflow.Clip)
                    }
                }
            }
        assertEquals(listOf(LayoutRule.TEXT_TRUNCATED, LayoutRule.TEXT_TRUNCATED), rules)
    }

    @Test
    fun contentPushedPastTheWindowEdgeIsReported() {
        val rules = rulesOf { Text("Far", Modifier.offset(x = 5000.dp)) }
        assertEquals(listOf(LayoutRule.OUT_OF_WINDOW), rules)
    }

    @Test
    fun unwrappedLinesDroppedLinesAndBothWindowEdgesAreReported() {
        composeRule.setContent {
            Column {
                Text(long, Modifier.width(60.dp), softWrap = false, overflow = TextOverflow.Visible)
                Text(long, Modifier.width(60.dp), maxLines = 2, overflow = TextOverflow.Clip)
                Text("Left", Modifier.offset(x = (-5000).dp))
                Box(Modifier.size(10.dp).offset(x = 5000.dp).semantics { contentDescription = "Far box" })
                Box(Modifier.size(0.dp).offset(x = 5000.dp).semantics { contentDescription = "Empty" })
            }
        }
        composeRule.waitForIdle()
        val root = composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val found = LayoutAudit.audit(root).map { it.rule to it.text }
        assertEquals(
            listOf(
                LayoutRule.TEXT_TRUNCATED to long,
                LayoutRule.TEXT_TRUNCATED to long,
                LayoutRule.OUT_OF_WINDOW to "Left",
                LayoutRule.OUT_OF_WINDOW to "Far box",
            ),
            found,
        )
    }

    @Test
    fun acceptedViolationsAreIgnored() {
        val options = LayoutOptions(ignored = { it.rule == LayoutRule.TEXT_TRUNCATED && it.text == long })
        val rules =
            rulesOf(options) { Text(long, Modifier.width(60.dp), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        assertTrue(rules.isEmpty())
    }

    @Test
    fun assertLayoutFitsListsViolationsAndThePropertyIsOffByDefault() {
        composeRule.setContent { Text(long, Modifier.width(60.dp), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        val failure = runCatching { composeRule.assertLayoutFits() }.exceptionOrNull()
        assertTrue(failure is AssertionError)
        assertTrue(failure?.message.orEmpty().contains("TEXT_TRUNCATED"))
        assertFalse(LayoutAudit.isEnabled())
        composeRule.auditLayoutIfEnabled()
    }

    @Test
    fun theScreenshotHookAuditsOnlyWhenTheModuleEnablesIt() {
        composeRule.setContent { Text(long, Modifier.width(60.dp), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        System.setProperty(LayoutAudit.ENABLED_PROPERTY, "true")
        val failure = runCatching { composeRule.auditLayoutIfEnabled() }.exceptionOrNull()
        System.clearProperty(LayoutAudit.ENABLED_PROPERTY)
        assertTrue(failure is AssertionError)
    }
}
