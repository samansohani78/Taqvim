/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.lint

import com.android.tools.lint.client.api.LintClient
import com.android.tools.lint.detector.api.CURRENT_API
import org.junit.Assert.assertEquals
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
    fun issueIdsAreUnique() {
        val ids = TaqvimIssueRegistry().issues.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }
}
