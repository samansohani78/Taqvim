/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-1801 frame timing of the world map (T-1301): opened through `taqvim://map`, then panned and pinch-zoomed while the
 * day/night and other shaded layers are redrawn. The map fills the screen, so gestures go to the app's root node.
 * Nightly runs fail on a regression of more than 10 % against the committed baseline.
 */
@RunWith(AndroidJUnit4::class)
class MapScreenBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun mapPanAndZoom(): Unit =
        rule.measureRepeated(
            packageName = APP_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            iterations = ITERATIONS,
            // WARM, like the other scroll journeys: with COLD the framework kills the app between the setup and the
            // measured block, so a journey that opens its screen in the setup would measure an empty screen.
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                openLink(MAP_LINK)
            },
        ) {
            val root = checkNotNull(device.wait(Until.findObject(By.pkg(APP_PACKAGE).depth(0)), UI_TIMEOUT_MILLIS))
            withSafeGestureMargin(root)
            val centerX = device.displayWidth / 2
            val centerY = device.displayHeight / 2
            val offset = device.displayWidth / PAN_DIVISOR
            repeat(GESTURES) {
                root.pinchOpen(PINCH_PERCENT)
                device.waitForIdle()
                device.swipe(centerX + offset, centerY, centerX - offset, centerY, PAN_STEPS)
                device.swipe(centerX, centerY + offset, centerX, centerY - offset, PAN_STEPS)
                device.waitForIdle()
                root.pinchClose(PINCH_PERCENT)
                device.waitForIdle()
            }
        }

    private companion object {
        /** The world map link of ADR-0016 (`DeepLinks` in `:app`). */
        const val MAP_LINK = "taqvim://map"
        const val ITERATIONS = 5
        const val GESTURES = 3
        const val PINCH_PERCENT = 0.6f
        const val PAN_DIVISOR = 4
        const val PAN_STEPS = 20
        const val UI_TIMEOUT_MILLIS = 5_000L
    }
}
