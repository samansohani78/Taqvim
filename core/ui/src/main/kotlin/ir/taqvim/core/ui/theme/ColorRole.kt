/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

/** The tonal palettes a scheme is built from. */
public enum class PaletteKey {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    ERROR,
    NEUTRAL,
    NEUTRAL_VARIANT,
}

/** How far apart foreground and background tones are placed. */
public enum class ContrastLevel {
    /** Text roles reach at least 4.5:1 (WCAG 2.2 §1.4.3). */
    STANDARD,

    /** Text roles reach at least 7:1 (WCAG 2.2 §1.4.6). */
    HIGH,
}

/**
 * Every Material 3 color role with the L* tone it takes from its [palette] in light and dark schemes, at standard and
 * high contrast. Tones are chosen so each text role keeps the contrast its [ContrastLevel] promises against every
 * surface it is drawn on (verified in `SchemeContrastTest`).
 */
public enum class ColorRole(
    public val palette: PaletteKey,
    public val light: Int,
    public val dark: Int,
    public val highLight: Int,
    public val highDark: Int,
) {
    PRIMARY(PaletteKey.PRIMARY, 40, 80, 25, 90),
    ON_PRIMARY(PaletteKey.PRIMARY, 100, 20, 100, 0),
    PRIMARY_CONTAINER(PaletteKey.PRIMARY, 90, 30, 35, 80),
    ON_PRIMARY_CONTAINER(PaletteKey.PRIMARY, 30, 90, 100, 0),
    INVERSE_PRIMARY(PaletteKey.PRIMARY, 80, 40, 90, 25),
    SECONDARY(PaletteKey.SECONDARY, 40, 80, 25, 90),
    ON_SECONDARY(PaletteKey.SECONDARY, 100, 20, 100, 0),
    SECONDARY_CONTAINER(PaletteKey.SECONDARY, 90, 30, 35, 80),
    ON_SECONDARY_CONTAINER(PaletteKey.SECONDARY, 30, 90, 100, 0),
    TERTIARY(PaletteKey.TERTIARY, 40, 80, 25, 90),
    ON_TERTIARY(PaletteKey.TERTIARY, 100, 20, 100, 0),
    TERTIARY_CONTAINER(PaletteKey.TERTIARY, 90, 30, 35, 80),
    ON_TERTIARY_CONTAINER(PaletteKey.TERTIARY, 30, 90, 100, 0),
    BACKGROUND(PaletteKey.NEUTRAL, 98, 6, 98, 6),
    ON_BACKGROUND(PaletteKey.NEUTRAL, 10, 90, 0, 100),
    SURFACE(PaletteKey.NEUTRAL, 98, 6, 98, 6),
    ON_SURFACE(PaletteKey.NEUTRAL, 10, 90, 0, 100),
    SURFACE_VARIANT(PaletteKey.NEUTRAL_VARIANT, 90, 30, 90, 25),
    ON_SURFACE_VARIANT(PaletteKey.NEUTRAL_VARIANT, 30, 80, 15, 95),
    SURFACE_TINT(PaletteKey.PRIMARY, 40, 80, 25, 90),
    INVERSE_SURFACE(PaletteKey.NEUTRAL, 20, 90, 15, 95),
    INVERSE_ON_SURFACE(PaletteKey.NEUTRAL, 95, 20, 100, 0),
    ERROR(PaletteKey.ERROR, 40, 80, 25, 90),
    ON_ERROR(PaletteKey.ERROR, 100, 20, 100, 0),
    ERROR_CONTAINER(PaletteKey.ERROR, 90, 30, 35, 80),
    ON_ERROR_CONTAINER(PaletteKey.ERROR, 30, 90, 100, 0),
    OUTLINE(PaletteKey.NEUTRAL_VARIANT, 50, 60, 30, 90),
    OUTLINE_VARIANT(PaletteKey.NEUTRAL_VARIANT, 80, 30, 35, 80),
    SCRIM(PaletteKey.NEUTRAL, 0, 0, 0, 0),
    SURFACE_BRIGHT(PaletteKey.NEUTRAL, 98, 24, 98, 30),
    SURFACE_DIM(PaletteKey.NEUTRAL, 87, 6, 85, 6),
    SURFACE_CONTAINER(PaletteKey.NEUTRAL, 94, 12, 92, 14),
    SURFACE_CONTAINER_HIGH(PaletteKey.NEUTRAL, 92, 17, 90, 20),
    SURFACE_CONTAINER_HIGHEST(PaletteKey.NEUTRAL, 90, 22, 88, 25),
    SURFACE_CONTAINER_LOW(PaletteKey.NEUTRAL, 96, 10, 95, 10),
    SURFACE_CONTAINER_LOWEST(PaletteKey.NEUTRAL, 100, 4, 100, 4),
    PRIMARY_FIXED(PaletteKey.PRIMARY, 90, 90, 90, 90),
    PRIMARY_FIXED_DIM(PaletteKey.PRIMARY, 80, 80, 80, 80),
    ON_PRIMARY_FIXED(PaletteKey.PRIMARY, 10, 10, 0, 0),
    ON_PRIMARY_FIXED_VARIANT(PaletteKey.PRIMARY, 30, 30, 0, 0),
    SECONDARY_FIXED(PaletteKey.SECONDARY, 90, 90, 90, 90),
    SECONDARY_FIXED_DIM(PaletteKey.SECONDARY, 80, 80, 80, 80),
    ON_SECONDARY_FIXED(PaletteKey.SECONDARY, 10, 10, 0, 0),
    ON_SECONDARY_FIXED_VARIANT(PaletteKey.SECONDARY, 30, 30, 0, 0),
    TERTIARY_FIXED(PaletteKey.TERTIARY, 90, 90, 90, 90),
    TERTIARY_FIXED_DIM(PaletteKey.TERTIARY, 80, 80, 80, 80),
    ON_TERTIARY_FIXED(PaletteKey.TERTIARY, 10, 10, 0, 0),
    ON_TERTIARY_FIXED_VARIANT(PaletteKey.TERTIARY, 30, 30, 0, 0),
    ;

    /** Tone of this role for a light or [dark] scheme at [contrast]. */
    public fun tone(
        dark: Boolean,
        contrast: ContrastLevel,
    ): Int =
        when (contrast) {
            ContrastLevel.STANDARD -> if (dark) this.dark else light
            ContrastLevel.HIGH -> if (dark) highDark else highLight
        }
}
