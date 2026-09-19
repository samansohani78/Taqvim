/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear.device

import androidx.test.uiautomator.By
import ir.taqvim.wear.MONTH_NEXT_TAG
import ir.taqvim.wear.MONTH_PREVIOUS_TAG
import ir.taqvim.wear.WearRoutes
import ir.taqvim.wear.openTag
import java.util.regex.Pattern
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Opens every screen of the watch app on a real Wear OS device (Gradle Managed Device `wearApi34`, T-1600) with the
 * date language set to Persian and to English: today, the month with its month steps, the converter with its steppers
 * and settings with each of its four choice lists. A crash, ANR or missing screen fails the test.
 */
@RunWith(Parameterized::class)
class WearDeviceSmokeTest(
    private val language: String,
) {
    @Before
    fun setUp() {
        assumeWatch()
        useLanguage(language)
        launchWatchApp()
    }

    @Test
    fun todayOpensTheOtherScreensAndSwipingGoesBack() {
        openScreen(openTag(WearRoutes.MONTH), WearRoutes.MONTH)
        swipeBack(WearRoutes.MONTH, WearRoutes.TODAY)
        openScreen(openTag(WearRoutes.CONVERTER), WearRoutes.CONVERTER)
        swipeBack(WearRoutes.CONVERTER, WearRoutes.TODAY)
        openScreen(openTag(WearRoutes.SETTINGS), WearRoutes.SETTINGS)
        swipeBack(WearRoutes.SETTINGS, WearRoutes.TODAY)
        assertInFront()
    }

    @Test
    fun theMonthScreenSteps() {
        openScreen(openTag(WearRoutes.MONTH), WearRoutes.MONTH)
        tapTag(MONTH_NEXT_TAG)
        awaitScreen(WearRoutes.MONTH)
        tapTag(MONTH_PREVIOUS_TAG)
        awaitScreen(WearRoutes.MONTH)
        assertInFront()
    }

    @Test
    fun theConverterStepsEveryField() {
        openScreen(openTag(WearRoutes.CONVERTER), WearRoutes.CONVERTER)
        // `StepperRow` is the only converter control with a content description (`wear_converter_decrease`). The
        // converter list is taller than the watch screen and composes only what is on it, so the steppers are
        // collected and tapped pass by pass while the list scrolls down.
        val stepped = mutableSetOf<String>()
        repeat(CONVERTER_PASSES) {
            // The descriptions are read first: tapping one recomposes the list, which makes the other handles stale.
            // The stepper buttons report `clickable=false` (their content description sits on the modifier), so they
            // are collected by description rather than by By.clickable.
            val descriptions =
                device
                    .findObjects(By.desc(Pattern.compile(".+")))
                    .mapNotNull { stepper -> runCatching { stepper.contentDescription }.getOrNull() }
                    .filter { it.isNotBlank() }
            descriptions.filter { stepped.add(it) }.forEach { description -> tapDescription(description) }
            scrollScreen(down = true)
        }
        check(stepped.size >= STEPPERS) { "the converter shows ${stepped.size} steppers, expected $STEPPERS" }
        awaitScreen(WearRoutes.CONVERTER)
        assertInFront()
    }

    @Test
    fun everySettingsChoiceListOpens() {
        openScreen(openTag(WearRoutes.SETTINGS), WearRoutes.SETTINGS)
        CHOICES.forEach { route ->
            openScreen(openTag(route), route)
            goBack(route, WearRoutes.SETTINGS)
        }
        assertInFront()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun languages(): List<String> = listOf("fa", "en")

        /** The choice lists of the settings summary (`WearRoutes`). */
        private val CHOICES =
            listOf(WearRoutes.LANGUAGE, WearRoutes.CALENDAR, WearRoutes.METHOD, WearRoutes.CITY)

        /** The converter has a minus and a plus button per field (year, month, day). */
        private const val STEPPERS = 6

        /** How many times the converter list is scrolled while collecting its steppers. */
        private const val CONVERTER_PASSES = 6
    }
}
