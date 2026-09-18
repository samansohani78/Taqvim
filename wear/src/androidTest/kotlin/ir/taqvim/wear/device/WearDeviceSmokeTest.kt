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
        swipeBack(WearRoutes.TODAY)
        openScreen(openTag(WearRoutes.CONVERTER), WearRoutes.CONVERTER)
        swipeBack(WearRoutes.TODAY)
        openScreen(openTag(WearRoutes.SETTINGS), WearRoutes.SETTINGS)
        swipeBack(WearRoutes.TODAY)
        assertInFront()
    }

    @Test
    fun theMonthScreenSteps() {
        openScreen(openTag(WearRoutes.MONTH), WearRoutes.MONTH)
        awaitTag(MONTH_NEXT_TAG).click()
        awaitScreen(WearRoutes.MONTH)
        awaitTag(MONTH_PREVIOUS_TAG).click()
        awaitScreen(WearRoutes.MONTH)
        assertInFront()
    }

    @Test
    fun theConverterStepsEveryField() {
        openScreen(openTag(WearRoutes.CONVERTER), WearRoutes.CONVERTER)
        // `StepperRow` is the only converter control with a content description (`wear_converter_decrease`).
        val steppers = device.findObjects(By.clickable(true)).filter { !it.contentDescription.isNullOrBlank() }
        check(steppers.size >= STEPPERS) { "the converter shows ${steppers.size} steppers, expected $STEPPERS" }
        steppers.forEach { stepper ->
            stepper.click()
            device.waitForIdle(IDLE_MILLIS)
        }
        awaitScreen(WearRoutes.CONVERTER)
        assertInFront()
    }

    @Test
    fun everySettingsChoiceListOpens() {
        openScreen(openTag(WearRoutes.SETTINGS), WearRoutes.SETTINGS)
        CHOICES.forEach { route ->
            openScreen(openTag(route), route)
            swipeBack(WearRoutes.SETTINGS)
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

        private const val IDLE_MILLIS = 500L
    }
}
