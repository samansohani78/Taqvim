/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import org.junit.jupiter.api.Test

class ComposeStabilityTest {
    private val composables =
        """
        restartable skippable scheme("[androidx.compose.ui.UiComposable]") fun ir.taqvim.feature.calendar.DayEventsTab(
          content: CalendarContent
          unstable language: LanguageSpec
          stable onAction: Function1<CalendarAction, Unit>
          stable modifier: Modifier? = @static <expression>
        )
        fun ir.taqvim.core.ui.component.rememberCenteredValue(
          stable state: LazyListState
          unstable range: IntRange
        )
        restartable skippable scheme("[androidx.glance.GlanceComposable]") fun ir.taqvim.feature.widgets.WidgetFrame(
          config: WidgetConfig
          unstable onClick: Intent? = @static null
        )
        """.trimIndent()

    private val classes =
        """
        unstable class ir.taqvim.feature.calendar.CalendarUiState {
          unstable val content: CalendarContent?
          <runtime stability> = Unstable
        }
        stable class ir.taqvim.core.ui.component.DayCellModel {
          <runtime stability> = Stable
        }
        unstable class ir.taqvim.feature.calendar.CalendarViewModel {
          <runtime stability> = Unstable
        }
        unstable class ir.taqvim.feature.calendar.CalendarContent {
          <runtime stability> = Unstable
        }
        unstable class ir.taqvim.feature.map.MapModel {
        }
        """.trimIndent()

    @Test
    fun `unstable parameters are reported with their composable`() {
        ComposeStability.unstableParameters(composables).map { it.toString() } shouldContainExactly
            listOf(
                "ir.taqvim.feature.calendar.DayEventsTab — language: LanguageSpec",
                "ir.taqvim.core.ui.component.rememberCenteredValue — range: IntRange",
                "ir.taqvim.feature.widgets.WidgetFrame — onClick: Intent?",
            )
    }

    @Test
    fun `only unstable UiState, Content and Model classes count`() {
        ComposeStability.unstableUiModels(classes).map { it.owner } shouldContainExactly
            listOf(
                "ir.taqvim.feature.calendar.CalendarUiState",
                "ir.taqvim.feature.calendar.CalendarContent",
                "ir.taqvim.feature.map.MapModel",
            )
    }

    @Test
    fun `documented exceptions remove findings by key`() {
        val accepted =
            ComposeStability.exceptions(
                """
                # comment
                ir.taqvim.feature.widgets.WidgetFrame(onClick) | Glance click intent

                ir.taqvim.feature.map.MapModel | test entry
                """.trimIndent(),
            )
        accepted shouldBe setOf("ir.taqvim.feature.widgets.WidgetFrame(onClick)", "ir.taqvim.feature.map.MapModel")
        ComposeStability.violations(listOf(composables), listOf(classes), accepted).map { it.key } shouldContainExactly
            listOf(
                "ir.taqvim.feature.calendar.DayEventsTab(language)",
                "ir.taqvim.core.ui.component.rememberCenteredValue(range)",
                "ir.taqvim.feature.calendar.CalendarUiState",
                "ir.taqvim.feature.calendar.CalendarContent",
            )
    }

    @Test
    fun `stable reports have no violations and exceptions need a reason`() {
        ComposeStability.violations(listOf("fun a.B(\n  stable x: Int\n)"), listOf(""), emptySet()).shouldBeEmpty()
        shouldThrow<GradleException> { ComposeStability.exceptions("a.B(x)") }.message shouldContain "without a reason"
    }
}
