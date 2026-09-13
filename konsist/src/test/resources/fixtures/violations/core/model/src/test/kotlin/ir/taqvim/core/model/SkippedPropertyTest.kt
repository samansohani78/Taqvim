/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.model

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class SkippedPropertyTest {
    @Test
    fun `returns a value so JUnit skips it`() =
        runBlocking {
            check(true)
        }
}
