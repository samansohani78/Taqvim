/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotMatrix
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import ir.taqvim.core.workdays.ShiftRotation
import ir.taqvim.core.workdays.ShiftType
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** F-08 screenshots: the rotations a user keeps and the editor that builds one. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShiftRotationScreenshotTest(
    private val sample: String,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureShifts() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val state = if (sample == "shift_rotations") LIST else EDITOR
        composeRule.captureScreenshot(sample, environment) {
            ToolsTestTheme(rtl = rtl, dark = environment.theme.isDark) {
                ShiftRotationScreen(state, ShiftRotationActions())
            }
        }
    }

    companion object {
        private val DAY = ShiftType("Day", SHIFT_COLORS[0])
        private val NIGHT = ShiftType("Night", SHIFT_COLORS[1])
        private val OFF = ShiftType("Off", SHIFT_COLORS[2])

        private val ROTATION =
            ShiftRotation(
                id = 1,
                name = "Two on, two off",
                anchor = Jdn(2_461_121),
                pattern = listOf(DAY, DAY, NIGHT, NIGHT, OFF, OFF),
            )

        private val LIST = ShiftRotationUiState(loading = false, rotations = persistentListOf(ROTATION))

        private val EDITOR =
            ShiftRotationUiState(
                loading = false,
                rotations = persistentListOf(ROTATION),
                editing =
                    ShiftRotationDraft(
                        id = 1,
                        name = "Two on, two off",
                        anchorJdn = 2_461_121,
                        anchorText = "1405/01/01",
                        types = persistentListOf(DAY, NIGHT, OFF),
                        pattern = persistentListOf(0, 0, 1, 1, 2, 2),
                    ),
            )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            listOf(
                arrayOf<Any>("shift_rotations", ScreenshotEnvironment(theme = ScreenshotTheme.LIGHT)),
                arrayOf<Any>(
                    "shift_rotation_editor",
                    ScreenshotEnvironment(theme = ScreenshotTheme.DARK, layoutDirection = LayoutDirection.Rtl),
                ),
            ) +
                listOf("shift_rotations", "shift_rotation_editor").flatMap { sample ->
                    ScreenshotMatrix.largeText().map { arrayOf<Any>(sample, it) }
                }
    }
}
