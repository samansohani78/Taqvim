/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.lint

import com.android.tools.lint.client.api.LintClient
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.TextFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaqvimIssueRegistryTest {
    @Before
    fun setUpLintClient() {
        LintClient.clientName = LintClient.CLIENT_UNIT_TESTS
    }

    @Test
    fun declaresCurrentApiAndVendor() {
        val registry = TaqvimIssueRegistry()

        assertEquals(CURRENT_API, registry.api)
        assertEquals("ir.taqvim:lint", registry.vendor.identifier)
    }

    @Test
    fun registersEveryPlannedRule() {
        val ids = TaqvimIssueRegistry().issues.map { it.id }

        assertEquals(
            listOf(
                "NoDoubleBang",
                "NoTryCatch",
                "UseRunCatching",
                "NoUnsafeCast",
                "NoHardcodedNonLatinText",
                "HardcodedComposeText",
                "NoGlobalMutableState",
                "PreferPredictiveBack",
                "MissingFarsiTranslation",
            ),
            ids,
        )
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyIssueIsAnExplainedError() {
        TaqvimIssueRegistry().issues.forEach { issue ->
            assertEquals(issue.id, Severity.ERROR, issue.defaultSeverity)
            assertTrue(issue.id, issue.getExplanation(TextFormat.RAW).isNotBlank())
        }
    }
}
