/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.i18n.TextDirection

/** The user's choice of light or dark appearance. */
public enum class ThemeMode {
    /** Follow the system dark-mode setting. */
    SYSTEM,
    LIGHT,
    DARK,

    /** Dark on pure black, for OLED screens. */
    BLACK,
}

/**
 * Theme preferences (T-700). A plain value: persistence belongs to `:data:preferences`. The custom font and background
 * image are not part of it because they are files; [TaqvimTheme] receives them already loaded.
 *
 * @property dynamicColor use the wallpaper-derived system palette where the platform provides one (Android 12+).
 * @property seedColor opaque `0xAARRGGBB` seed of the generated scheme when dynamic color is off or unavailable.
 * @property gradient draw screen backgrounds as a gradient instead of a flat surface.
 * @property highContrast place text roles at 7:1 contrast or more.
 * @property boldText render all text one weight step heavier.
 */
public data class ThemeSettings(
    public val mode: ThemeMode = ThemeMode.SYSTEM,
    public val dynamicColor: Boolean = true,
    public val seedColor: Int = SchemeGenerator.DEFAULT_SEED,
    public val gradient: Boolean = false,
    public val highContrast: Boolean = false,
    public val boldText: Boolean = false,
)

/** Where a scheme's colors come from. */
public enum class ColorSource {
    /** The platform's dynamic scheme, used as is. */
    PLATFORM_DYNAMIC,

    /** A generated scheme seeded with the platform's dynamic primary color (dynamic color with high contrast). */
    DYNAMIC_SEED,

    /** A generated scheme seeded with [ThemeSettings.seedColor]. */
    CUSTOM_SEED,
}

/** The appearance [ThemeSettings] resolve to on a given device. */
public data class Appearance(
    public val dark: Boolean,
    public val black: Boolean,
    public val source: ColorSource,
    public val contrast: ContrastLevel,
)

/**
 * Resolves these settings for a device whose system dark mode is [systemDark] and which [supportsDynamicColor].
 * The platform dynamic scheme has no high-contrast variant under app control, so high contrast with dynamic color
 * generates a scheme from the dynamic primary color instead.
 */
public fun ThemeSettings.resolve(
    systemDark: Boolean,
    supportsDynamicColor: Boolean,
): Appearance {
    val contrast = if (highContrast) ContrastLevel.HIGH else ContrastLevel.STANDARD
    val source =
        when {
            !dynamicColor || !supportsDynamicColor -> ColorSource.CUSTOM_SEED
            highContrast -> ColorSource.DYNAMIC_SEED
            else -> ColorSource.PLATFORM_DYNAMIC
        }
    val dark =
        when (mode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK, ThemeMode.BLACK -> true
        }
    return Appearance(dark = dark, black = mode == ThemeMode.BLACK, source = source, contrast = contrast)
}

/** Compose layout direction for the app language's writing [TextDirection]. */
public fun TextDirection.toLayoutDirection(): LayoutDirection =
    when (this) {
        TextDirection.LTR -> LayoutDirection.Ltr
        TextDirection.RTL -> LayoutDirection.Rtl
    }
