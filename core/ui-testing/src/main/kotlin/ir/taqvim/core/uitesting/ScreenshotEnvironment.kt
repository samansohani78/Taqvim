/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.uitesting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.Locales
import androidx.compose.ui.test.then
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

/** Device classes every screen and widget is captured on (docs/PLAN.md T-005). Sizes are dp rendered at mdpi. */
enum class ScreenshotDevice(
    val widthDp: Int,
    val heightDp: Int,
) {
    PHONE(412, 915),
    TABLET(1280, 800),
    FOLD(673, 841),
    ;

    /** Robolectric resource qualifiers reproducing this device; mdpi so image pixels equal dp. */
    val qualifiers: String
        get() = "w${widthDp}dp-h${heightDp}dp-${if (widthDp > heightDp) "land" else "port"}-mdpi"

    /** File-name-safe id. */
    val id: String
        get() = name.lowercase()
}

/** Theme variants of the screenshot matrix. [BLACK] is the AMOLED dark variant provided by the app theme (T-700). */
enum class ScreenshotTheme {
    LIGHT,
    DARK,
    BLACK,
    ;

    /** Whether the system should report dark mode. */
    val isDark: Boolean
        get() = this != LIGHT
}

/** One cell of the screenshot matrix. The locale defaults to Persian for RTL and English for LTR. */
data class ScreenshotEnvironment(
    val device: ScreenshotDevice = ScreenshotDevice.PHONE,
    val theme: ScreenshotTheme = ScreenshotTheme.LIGHT,
    val layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    val fontScale: Float = 1f,
    val localeTag: String = if (layoutDirection == LayoutDirection.Rtl) "fa" else "en",
) {
    /** Stable file-name-safe id, e.g. `phone_dark_rtl_fs130_fa`. */
    val id: String
        get() =
            listOf(
                device.id,
                theme.name.lowercase(),
                layoutDirection.name.lowercase(),
                "fs${(fontScale * PERCENT).roundToInt()}",
                localeTag,
            ).joinToString("_")

    private companion object {
        const val PERCENT = 100
    }
}

/** Theme variant requested by the current screenshot; the app theme maps it to its palette (T-700). */
val LocalScreenshotTheme: ProvidableCompositionLocal<ScreenshotTheme> =
    staticCompositionLocalOf { ScreenshotTheme.LIGHT }

/** Renders [content] with the dark mode, layout direction, font scale and locale of [environment]. */
@Composable
fun WithScreenshotEnvironment(
    environment: ScreenshotEnvironment,
    content: @Composable () -> Unit,
) {
    val override =
        DeviceConfigurationOverride.DarkMode(environment.theme.isDark) then
            DeviceConfigurationOverride.LayoutDirection(environment.layoutDirection) then
            DeviceConfigurationOverride.FontScale(environment.fontScale) then
            DeviceConfigurationOverride.Locales(LocaleList(environment.localeTag))
    CompositionLocalProvider(LocalScreenshotTheme provides environment.theme) {
        DeviceConfigurationOverride(override, content)
    }
}
