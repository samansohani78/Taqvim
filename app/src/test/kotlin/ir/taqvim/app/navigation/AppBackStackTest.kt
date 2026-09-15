/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** ADR-0015 back stack: tabs replace the stack above the calendar, screens push once, back pops to the calendar. */
class AppBackStackTest {
    private val calendar = AppBackStack()

    @Test
    fun `screens are pushed once and popped back to the calendar`() {
        val editor = AppDestination.EventEditor(7)
        val stack = calendar.push(editor).push(editor)

        stack.entries shouldBe listOf(AppDestination.Calendar, editor)
        stack.selectedTab shouldBe TopLevelTab.CALENDAR
        stack.pop() shouldBe calendar
        calendar.pop() shouldBe calendar
        calendar.canPop shouldBe false
    }

    @Test
    fun `a tab sits above the calendar and keeps the screens opened from it`() {
        val year = calendar.select(TopLevelTab.MORE).push(AppDestination.Year)

        year.entries shouldBe listOf(AppDestination.Calendar, AppDestination.More, AppDestination.Year)
        year.selectedTab shouldBe TopLevelTab.MORE
        year.top shouldBe AppDestination.Year
        year.pop().top shouldBe AppDestination.More
        year.pop().pop() shouldBe calendar
    }

    @Test
    fun `selecting a tab closes the screens of the previous tab and reselecting returns to its screen`() {
        val year = calendar.select(TopLevelTab.MORE).push(AppDestination.Year)

        year.select(TopLevelTab.TIMES).entries shouldBe listOf(AppDestination.Calendar, AppDestination.Times)
        year.select(TopLevelTab.MORE).entries shouldBe listOf(AppDestination.Calendar, AppDestination.More)
        year.select(TopLevelTab.CALENDAR) shouldBe calendar
        year.push(AppDestination.Tools).selectedTab shouldBe TopLevelTab.TOOLS
        year.push(AppDestination.Calendar) shouldBe calendar
    }

    @Test
    fun `the stack must start at the calendar`() {
        shouldThrow<IllegalArgumentException> { AppBackStack(listOf(AppDestination.Times)) }
        shouldThrow<IllegalArgumentException> { AppBackStack(emptyList()) }
    }

    @Test
    fun `every destination survives saving and unreadable saved text restores the calendar`() {
        val stack = AppBackStack(listOf(AppDestination.Calendar) + ALL_OTHERS)

        AppBackStack.decode(AppBackStack.encode(stack)) shouldBe stack
        AppBackStack.decode("not a back stack") shouldBe calendar
        AppBackStack.decode("""[{"type":"ir.taqvim.app.navigation.AppDestination.Times"}]""") shouldBe calendar
    }

    @Test
    fun `any sequence of navigation keeps a valid stack`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.list(Arb.element(OPERATIONS), 0..30)) { operations ->
                val stack = operations.fold(calendar) { current, operation -> operation(current) }

                stack.entries.first() shouldBe AppDestination.Calendar
                stack.entries.zipWithNext().none { (a, b) -> a == b } shouldBe true
                stack.entries.drop(2).none { TopLevelTab.of(it) != null } shouldBe true
                AppBackStack.decode(AppBackStack.encode(stack)) shouldBe stack
            }
        }

    private companion object {
        val ALL_OTHERS: List<AppDestination> =
            listOf(
                AppDestination.Times,
                AppDestination.Year,
                AppDestination.Agenda,
                AppDestination.Astronomy,
                AppDestination.WorldMap,
                AppDestination.MapPick,
                AppDestination.Compass,
                AppDestination.Level,
                AppDestination.Search,
                AppDestination.LocationSettings,
                AppDestination.AthanSettings,
                AppDestination.Timeline(2_461_121),
                AppDestination.Timeline(),
                AppDestination.EventEditor(42),
                AppDestination.EventEditor(),
                AppDestination.Pending(PendingFeature.SHIFT_WORK),
                AppDestination.Pending(PendingFeature.SETTINGS),
                AppDestination.Pending(PendingFeature.WIDGETS),
                AppDestination.Subscriptions,
                AppDestination.About,
                AppDestination.Settings(),
                AppDestination.Settings("THEME"),
                AppDestination.Tools,
                AppDestination.More,
            )

        val OPERATIONS: List<(AppBackStack) -> AppBackStack> =
            ALL_OTHERS.map { destination -> { stack: AppBackStack -> stack.push(destination) } } +
                TopLevelTab.entries.map { tab -> { stack: AppBackStack -> stack.select(tab) } } +
                listOf({ stack: AppBackStack -> stack.pop() })
    }
}
