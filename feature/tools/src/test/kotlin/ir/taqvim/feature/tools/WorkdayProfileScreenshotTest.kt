/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotMatrix
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import ir.taqvim.core.workdays.HalfDayPolicy
import ir.taqvim.core.workdays.WorkdayProfile
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** F-07 screenshots: the list of workday profiles and the editor of one. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WorkdayProfileScreenshotTest(
    private val sample: String,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureProfiles() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val state = if (sample == "workday_profiles") LIST else EDITOR
        composeRule.captureScreenshot(sample, environment) {
            ToolsTestTheme(rtl = rtl, dark = environment.theme.isDark) {
                WorkdayProfileScreen(state, WorkdayProfileActions())
            }
        }
    }

    companion object {
        private val IRAN_OFFICE =
            NamedWorkdayProfile(
                id = 1,
                name = "Iran office",
                profile =
                    WorkdayProfile(
                        weekend = setOf(Weekday.FRIDAY),
                        holidaySources = setOf(EventSource.IRAN_OFFICIAL),
                    ),
                isDefault = true,
            )

        private val CLIENT =
            NamedWorkdayProfile(
                id = 2,
                name = "UAE client",
                profile =
                    WorkdayProfile(
                        weekend = setOf(Weekday.SATURDAY, Weekday.SUNDAY),
                        holidaySources = emptySet(),
                    ),
                isDefault = false,
            )

        private val LIST =
            WorkdayProfileUiState(loading = false, profiles = persistentListOf(IRAN_OFFICE, CLIENT))

        private val EDITOR =
            WorkdayProfileUiState(
                loading = false,
                profiles = persistentListOf(IRAN_OFFICE),
                editing =
                    WorkdayProfileDraft(
                        id = 1,
                        name = "Iran office",
                        weekend = setOf(Weekday.FRIDAY),
                        holidaySources = setOf(EventSource.IRAN_OFFICIAL),
                        halfDays = HalfDayPolicy.HALF,
                        leave = persistentListOf(LeaveRow(2_461_200, 2_461_206, "1405/04/01 – 1405/04/07")),
                        isDefault = true,
                    ),
            )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            listOf(
                arrayOf<Any>("workday_profiles", ScreenshotEnvironment(theme = ScreenshotTheme.LIGHT)),
                arrayOf<Any>(
                    "workday_profile_editor",
                    ScreenshotEnvironment(theme = ScreenshotTheme.DARK, layoutDirection = LayoutDirection.Rtl),
                ),
            ) +
                listOf("workday_profiles", "workday_profile_editor").flatMap { sample ->
                    ScreenshotMatrix.largeText().map { arrayOf<Any>(sample, it) }
                }
    }
}
