/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import com.github.takahirom.roborazzi.captureRoboImage
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarSystem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** T-1600: the watch screens on a small round watch (Robolectric), with screenshots of today and the month. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "fa-w227dp-h227dp-small-round-watch-xhdpi")
class WearScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val setup = WearFixtures.setup()
    private val today = WearFixtures.calculator.today(setup, WearFixtures.NOWRUZ_MORNING)
    private val month =
        WearMonthBuilder.build(
            setup,
            WearFixtures.calculator.lookup(setup.islamicVariant),
            WearFixtures.NOWRUZ_MORNING.toJdn(setup.zone),
        )

    @Test
    fun `today shows the date, the next prayer and opens the other screens`() {
        val opened = mutableListOf<String>()
        compose.setContent {
            WearTestFrame { TodayScreen(TodayUiState(today, "تهران"), onOpen = { opened += it }) }
        }

        compose.onNode(hasText(today.primaryDate)).assertExists()
        compose.onRoot().captureRoboImage("src/test/screenshots/wear_today/round_fa.png")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("تنظیمات"))
        compose.onNode(hasText("تنظیمات")).performClick()
        opened shouldBe listOf(WearRoutes.SETTINGS)
    }

    @Test
    fun `month shows the grid and moves to the next month`() {
        val shown = mutableListOf<Int>()
        compose.setContent { WearTestFrame { MonthScreen(MonthUiState(month), onShow = { shown += it }) } }

        compose.onNode(hasText(month.title)).assertExists()
        compose.onRoot().captureRoboImage("src/test/screenshots/wear_month/round_fa.png")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("ماه بعد"))
        compose.onNode(hasText("ماه بعد")).performClick()
        shown shouldBe listOf(1)
    }

    @Test
    fun `converter steps fields and switches calendars`() {
        val steps = mutableListOf<Pair<ConverterField, Int>>()
        val date = PersianCalendarSystem.date(1405, 1, 1)
        val state =
            ConverterUiState(
                source = CalendarSystem.PERSIAN,
                calendars = setup.calendars.map { it.system },
                date = date,
                sourceText = today.primaryDate,
                results = WearConverter.results(setup, PersianCalendarSystem, date),
            )
        compose.setContent {
            WearTestFrame { ConverterScreen(state, onStep = { f, d -> steps += f to d }, onSwitch = {}) }
        }

        compose.onNode(hasContentDescription("سال قبل")).performClick()
        steps shouldBe listOf(ConverterField.YEAR to -1)
    }

    @Test
    fun `a choice list marks the selection and reports the tapped key`() {
        val chosen = mutableListOf<String>()
        val choices = listOf(WearChoice("a", "الف"), WearChoice("b", "ب"))
        compose.setContent {
            WearTestFrame { ChoiceScreen("زبان", choices, selected = "a", onChoose = { chosen += it }) }
        }

        compose.onNode(hasText("انتخاب‌شده")).assertExists()
        compose.onNode(hasText("ب")).performClick()
        chosen shouldBe listOf("b")
    }
}

/**
 * The watch theme and app scaffold (dark background, as on a watch) around screen content. The time text is left out so
 * screenshots do not depend on the clock.
 */
@Composable
private fun WearTestFrame(content: @Composable () -> Unit) {
    MaterialTheme { AppScaffold(timeText = {}) { content() } }
}
