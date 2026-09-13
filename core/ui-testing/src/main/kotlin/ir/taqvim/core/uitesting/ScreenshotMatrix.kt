/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.uitesting

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import com.github.takahirom.roborazzi.captureRoboImage
import org.robolectric.RuntimeEnvironment

/**
 * The screenshot matrix required for every screen and widget (quality bar):
 * light/dark/black × LTR/RTL × font scale 1.0/1.3/2.0 × phone/tablet/fold = 54 environments.
 */
object ScreenshotMatrix {
    /** Font scales of the matrix. */
    val FONT_SCALES: List<Float> = listOf(1.0f, 1.3f, 2.0f)

    /** Every environment of the full matrix, in a stable order. */
    fun full(): List<ScreenshotEnvironment> =
        ScreenshotDevice.entries.flatMap { device ->
            ScreenshotTheme.entries.flatMap { theme ->
                listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).flatMap { direction ->
                    FONT_SCALES.map { scale -> ScreenshotEnvironment(device, theme, direction, scale) }
                }
            }
        }

    /** Parameters for `ParameterizedRobolectricTestRunner` (one environment per test run). */
    fun parameters(environments: List<ScreenshotEnvironment> = full()): List<Array<Any>> =
        environments.map { arrayOf<Any>(it) }
}

/** Module-relative directory of committed reference screenshots; equals the Roborazzi `outputDir`. */
const val SCREENSHOT_DIRECTORY: String = "src/test/screenshots"

private val SCREEN_NAME = Regex("""^[a-z][a-z0-9_]*$""")

/** Module-relative path of the screenshot of [screen] in [environment]. */
fun screenshotPath(
    screen: String,
    environment: ScreenshotEnvironment,
): String {
    require(SCREEN_NAME.matches(screen)) { "Screen name must be lower_snake_case: '$screen'" }
    return "$SCREENSHOT_DIRECTORY/$screen/${environment.id}.png"
}

/**
 * Configures Robolectric for [environment]'s device, renders [content] inside [WithScreenshotEnvironment] and
 * captures `src/test/screenshots/<screen>/<environment id>.png`. One call per test: a Compose rule accepts a
 * single `setContent`. Requires `@GraphicsMode(NATIVE)` on the test class.
 */
fun ComposeContentTestRule.captureScreenshot(
    screen: String,
    environment: ScreenshotEnvironment,
    content: @Composable () -> Unit,
) {
    val path = screenshotPath(screen, environment)
    RuntimeEnvironment.setQualifiers(environment.device.qualifiers)
    setContent { WithScreenshotEnvironment(environment, content) }
    onRoot().captureRoboImage(path)
}
