/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.uitesting

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Composable
fun CountedLabel(value: Int) {
    Text("value $value")
}

@Composable
fun OtherLabel(value: Int) {
    Text("other $value")
}

@RunWith(AndroidJUnit4::class)
class RecompositionCounterTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun countsOnlyTheNamedFunctionAndStopsWhenClosed() {
        var counted by mutableIntStateOf(0)
        var other by mutableIntStateOf(0)
        val counter = RecompositionCounter("ir.taqvim.core.uitesting.CountedLabel")
        composeRule.setContent {
            Column {
                CountedLabel(counted)
                OtherLabel(other)
            }
        }
        composeRule.waitForIdle()
        assertEquals(1, counter.count)

        other++
        composeRule.waitForIdle()
        assertEquals("another function's recomposition is not counted", 1, counter.count)

        counted++
        composeRule.waitForIdle()
        assertEquals(2, counter.count)

        counter.close()
        counted++
        composeRule.waitForIdle()
        assertEquals("a closed counter no longer counts", 2, counter.count)
    }
}
