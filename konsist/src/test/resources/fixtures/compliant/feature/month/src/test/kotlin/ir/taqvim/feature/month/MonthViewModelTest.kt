/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.month

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class MonthViewModelTest {
    @Test
    fun `block body`() {
        check(MonthUiState(title = "").days.isEmpty())
    }

    @Test
    fun `explicit unit`(): Unit =
        runBlocking {
            check(true)
        }
}
