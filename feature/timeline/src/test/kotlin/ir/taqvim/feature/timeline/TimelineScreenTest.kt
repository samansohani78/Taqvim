/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.withKeyDown
import io.kotest.assertions.withClue
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Jdn
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** T-900 UI: drag to create an event, the box's handle, keys and actions, opening events, modes and zoom. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalTestApi::class)
class TimelineScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val english = requireNotNull(LanguageTable.forCode("en"))
    private val days = FakeTimelineDaysSource()

    private fun show(
        navigation: TimelineNavigation = TimelineNavigation(),
        now: TimelineNow? = TimelineNow(TODAY, 8 * 60),
    ): TimelineViewModel {
        val viewModel =
            TimelineViewModel(
                FakeTimelineSettingsSource(ENGLISH_SETTINGS),
                FakeTimelineClockSource(now),
                FakeTimelinePlaceSource(null),
                days,
            )
        composeRule.setContent { TimelineTestTheme { TimelineRoute(navigation = navigation, viewModel = viewModel) } }
        return viewModel
    }

    private fun awaitNode(matcher: SemanticsMatcher) {
        composeRule.waitUntil(TIMEOUT_MILLIS) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitDraft(
        viewModel: TimelineViewModel,
        draft: TimelineDraft?,
    ) {
        // The state flows through the main looper, which waitUntil does not run; waitForIdle does.
        composeRule.waitForIdle()
        viewModel.uiState.value.content
            ?.draft shouldBe draft
    }

    private fun showDayWithDraft(navigation: TimelineNavigation = TimelineNavigation()): TimelineViewModel {
        val viewModel = show(navigation)
        viewModel.onAction(TimelineAction.ShowMode(TimelineMode.DAY))
        viewModel.onAction(TimelineAction.SetDraft(TODAY, 600, 660))
        awaitNode(hasTestTag(DRAFT_TAG))
        composeRule.waitForIdle()
        return viewModel
    }

    @Test
    fun `a long press and drag draws a box that creates an event with its times`() {
        var created: Triple<Jdn, Int, Int>? = null
        val navigation = TimelineNavigation(onCreateEvent = { day, start, end -> created = Triple(day, start, end) })
        val viewModel = show(navigation)
        viewModel.onAction(TimelineAction.ShowMode(TimelineMode.DAY))
        awaitNode(hasTestTag(dayTag(TODAY)))
        composeRule.waitForIdle()
        val hourPx = with(composeRule.density) { HOUR_HEIGHT.toPx() }
        val grid = composeRule.onNodeWithTag(GRID_TAG).fetchSemanticsNode().boundsInRoot
        val column = composeRule.onNodeWithTag(dayTag(TODAY)).fetchSemanticsNode().positionInRoot
        withClue("grid $grid, column $column, hour $hourPx px") {
            // Now is 08:00, so the grid opens scrolled to 07:00 and shows at least until 10:30.
            (grid.top - column.y) shouldBe (7 * hourPx plusOrMinus 1f)
            (grid.bottom - column.y >= 10.5f * hourPx) shouldBe true
        }

        // Touch positions are relative to the visible part of the column, which starts at 07:00.
        val visibleTop = grid.top - column.y
        composeRule.onNodeWithTag(dayTag(TODAY)).performTouchInput {
            down(Offset(centerX, 9 * hourPx - visibleTop))
            moveBy(Offset.Zero, delayMillis = LONG_PRESS_MILLIS)
            moveTo(Offset(centerX, 9.75f * hourPx - visibleTop))
            moveTo(Offset(centerX, 10.5f * hourPx - visibleTop))
            up()
        }
        awaitDraft(viewModel, TimelineDraft(TODAY, 540, 630))
        awaitNode(hasText(app.getString(R.string.timeline_draft, "09:00", "10:30")))
        composeRule.onNodeWithText(app.getString(R.string.timeline_draft_create)).performClick()

        composeRule.waitUntil(TIMEOUT_MILLIS) { created == Triple(TODAY, 540, 630) }
        awaitDraft(viewModel, null)
    }

    @Test
    fun `the resize handle and accessibility actions change the box`() {
        val viewModel = showDayWithDraft()
        val stepPx = with(composeRule.density) { HOUR_HEIGHT.toPx() } / 4

        composeRule.onNodeWithTag(RESIZE_TAG).performTouchInput {
            swipeDown(startY = centerY, endY = centerY + viewConfiguration.touchSlop + 2.5f * stepPx)
        }
        awaitDraft(viewModel, TimelineDraft(TODAY, 600, 690))
        val box = composeRule.onNodeWithTag(DRAFT_TAG)
        box.performCustomAccessibilityActionWithLabel(app.getString(R.string.timeline_draft_later))
        awaitDraft(viewModel, TimelineDraft(TODAY, 615, 705))
        box.performCustomAccessibilityActionWithLabel(app.getString(R.string.timeline_draft_shorter))
        awaitDraft(viewModel, TimelineDraft(TODAY, 615, 690))
        box.performCustomAccessibilityActionWithLabel(app.getString(R.string.timeline_draft_earlier))
        box.performCustomAccessibilityActionWithLabel(app.getString(R.string.timeline_draft_longer))
        awaitDraft(viewModel, TimelineDraft(TODAY, 600, 690))
        awaitNode(hasContentDescription(app.getString(R.string.timeline_draft, "10:00", "11:30")))
    }

    @Test
    fun `keys move and resize the box, Enter creates and Escape cancels`() {
        var created: Triple<Jdn, Int, Int>? = null
        val navigation = TimelineNavigation(onCreateEvent = { day, start, end -> created = Triple(day, start, end) })
        val viewModel = showDayWithDraft(navigation)
        val box = composeRule.onNodeWithTag(DRAFT_TAG)
        box.requestFocus()

        box.performKeyInput { pressKey(Key.DirectionDown) }
        awaitDraft(viewModel, TimelineDraft(TODAY, 615, 675))
        box.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionDown) } }
        awaitDraft(viewModel, TimelineDraft(TODAY, 615, 690))
        box.performKeyInput { pressKey(Key.Enter) }
        composeRule.waitUntil(TIMEOUT_MILLIS) { created == Triple(TODAY, 615, 690) }

        viewModel.onAction(TimelineAction.SetDraft(TODAY, 600, 660))
        awaitNode(hasTestTag(DRAFT_TAG))
        composeRule.onNodeWithTag(DRAFT_TAG).run {
            requestFocus()
            performKeyInput { pressKey(Key.Escape) }
        }
        awaitDraft(viewModel, null)
    }

    @Test
    fun `events open, and tabs, previous, next and today change the shown days`() {
        var opened: Pair<String, TimelineEventKind>? = null
        days.events.value = mapOf(TODAY to listOf(timed("m", 540, 600, title = "Meeting")))
        show(TimelineNavigation(onOpenEvent = { id, kind -> opened = id to kind }))
        val meeting = app.getString(R.string.timeline_event_timed, "Meeting", "09:00", "10:00")
        awaitNode(hasContentDescription(meeting))

        awaitNode(hasText("${english.monthNames.gregorian[3]} 2026"))
        composeRule.onNodeWithContentDescription(meeting).performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { opened == ("m" to TimelineEventKind.PERSONAL) }
        composeRule.onNodeWithTag(dayTag(TODAY + 1)).assertExists()

        composeRule.onNodeWithText(app.getString(R.string.timeline_mode_day)).performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { nodeCount(dayTag(TODAY + 1)) == 0 }
        composeRule.onNodeWithText(app.getString(R.string.timeline_next)).performClick()
        awaitNode(hasTestTag(dayTag(TODAY + 1)))
        composeRule.onNodeWithText(app.getString(R.string.timeline_previous)).performClick()
        composeRule.onNodeWithText(app.getString(R.string.timeline_previous)).performClick()
        awaitNode(hasTestTag(dayTag(TODAY - 1)))
        composeRule.onNodeWithText(app.getString(R.string.timeline_today)).performClick()
        awaitNode(hasTestTag(dayTag(TODAY)))
    }

    @Test
    fun `zoom actions and pinches change the hour height`() {
        val viewModel = show()
        awaitNode(hasTestTag(GRID_TAG))

        val grid = composeRule.onNodeWithTag(GRID_TAG)
        grid.performCustomAccessibilityActionWithLabel(app.getString(R.string.timeline_zoom_in))
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            viewModel.uiState.value.content
                ?.zoom == TimelineGeometry.ZOOM_STEP
        }
        grid.performCustomAccessibilityActionWithLabel(app.getString(R.string.timeline_zoom_out))
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            viewModel.uiState.value.content
                ?.zoom == 1f
        }
        grid.performTouchInput {
            pinch(
                start0 = center - Offset(0f, 20f),
                end0 = center - Offset(0f, 120f),
                start1 = center + Offset(0f, 20f),
                end1 = center + Offset(0f, 120f),
            )
        }
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            (
                viewModel.uiState.value.content
                    ?.zoom ?: 1f
            ) > 1f
        }
    }

    @Test
    fun `a loading timeline shows progress`() {
        show(now = null)

        awaitNode(hasContentDescription(app.getString(R.string.timeline_loading)))
    }

    private fun nodeCount(tag: String): Int = composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().size

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
        const val LONG_PRESS_MILLIS = 1_000L
    }
}
