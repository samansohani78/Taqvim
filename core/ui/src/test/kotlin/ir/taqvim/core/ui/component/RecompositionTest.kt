/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.uitesting.RecompositionCounter
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** T-1802: a day cell recomposes at most once per state change, and only the cells whose model changed. */
@RunWith(AndroidJUnit4::class)
class RecompositionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dayCellRecomposesOncePerChangeAndSkipsUnrelatedState() {
        var model by mutableStateOf(sampleCells()[5])
        var unrelated by mutableIntStateOf(0)
        RecompositionCounter(DAY_CELL).use { counter ->
            composeRule.setContent {
                TestTheme {
                    Column {
                        Text("$unrelated")
                        DayCell(model, onClick = {})
                    }
                }
            }
            composeRule.waitForIdle()
            assertEquals(1, counter.count)

            unrelated++
            composeRule.waitForIdle()
            assertEquals("unrelated state must not recompose the cell", 1, counter.count)

            model = model.copy(isSelected = true)
            composeRule.waitForIdle()
            assertEquals("one change, one recomposition", 2, counter.count)

            model = model.copy()
            composeRule.waitForIdle()
            assertEquals("an equal model must be skipped", 2, counter.count)
        }
    }

    @Test
    fun movingTheSelectionRecomposesOnlyTheTwoAffectedCells() {
        val cells = sampleCells().map { it.copy(isSelected = false) }
        var month by mutableStateOf(sampleMonth().copy(cells = cells.select(10)))
        RecompositionCounter(DAY_CELL).use { counter ->
            composeRule.setContent { TestTheme { MonthGrid(month, onDayClick = {}) } }
            composeRule.waitForIdle()
            assertEquals(cells.size, counter.count)

            month = month.copy(cells = cells.select(20))
            composeRule.waitForIdle()
            assertEquals(cells.size + 2, counter.count)
        }
    }

    private fun List<DayCellModel>.select(index: Int): List<DayCellModel> =
        mapIndexed { i, cell -> if (i == index) cell.copy(isSelected = true) else cell }

    private companion object {
        const val DAY_CELL = "ir.taqvim.core.ui.component.DayCell"
    }
}
