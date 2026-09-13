/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

class ProjectStructureKonsistTest {
    @Test
    fun `every production package lives under ir taqvim`() {
        Konsist
            .scopeFromProduction()
            .files
            .filterNot { it.path.contains("/build-logic/") }
            .assertTrue { it.packagee?.name?.startsWith("ir.taqvim") ?: false }
    }
}
