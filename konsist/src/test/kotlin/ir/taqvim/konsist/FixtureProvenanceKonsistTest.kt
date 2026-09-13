/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import com.lemonappdev.konsist.api.Konsist
import io.kotest.matchers.collections.shouldBeEmpty
import ir.taqvim.core.testing.FixtureAudit
import java.io.File
import org.junit.jupiter.api.Test

/** Every golden fixture in the repository cites its primary source (docs/PLAN.md §8.2). */
class FixtureProvenanceKonsistTest {
    @Test
    fun `golden fixtures carry a valid provenance header`() {
        FixtureAudit.findInvalid(File(Konsist.projectRootPath)).shouldBeEmpty()
    }
}
