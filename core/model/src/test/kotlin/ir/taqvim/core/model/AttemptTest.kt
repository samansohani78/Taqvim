/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.coroutines.cancellation.CancellationException
import org.junit.jupiter.api.Test

class AttemptTest {
    @Test
    fun `values and ordinary failures become results`() {
        attempt { 42 }.getOrNull() shouldBe 42
        attempt { error("broken") }.exceptionOrNull().shouldBeInstanceOf<IllegalStateException>()
    }

    @Test
    fun `cancellation is rethrown`() {
        shouldThrow<CancellationException> { attempt { throw CancellationException("stopped") } }
    }
}
