/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

/** Colors of one hue and chroma at any L* tone 0‥100. */
public data class TonalPalette(
    public val hue: Double,
    public val chroma: Double,
) {
    /** The opaque color of this palette at [tone]; chroma is reduced where the sRGB gamut requires it. */
    public fun tone(tone: Int): Int = ColorMath.fromLch(Lch(tone.toDouble(), chroma, hue))
}

/** A generated color scheme: one opaque `0xAARRGGBB` color for each [ColorRole]. */
public data class SchemeColors(
    private val colors: List<Int>,
) {
    init {
        require(colors.size == ColorRole.entries.size) { "expected ${ColorRole.entries.size} colors" }
    }

    /** The color of [role]. */
    public operator fun get(role: ColorRole): Int = colors[role.ordinal]
}

/**
 * Builds a complete scheme from a single seed color (T-700 custom seed). The seed's CIE LCh hue drives five palettes
 * whose chroma is fixed per role family; every role then takes the L* tone listed in [ColorRole]. Because contrast
 * depends only on luminance, and luminance only on L*, the contrast of each role pair is the same for every seed.
 */
public object SchemeGenerator {
    /** Taqvim's brand green, also the launcher background. */
    public const val DEFAULT_SEED: Int = 0xFF0B6E4F.toInt()

    private const val MIN_PRIMARY_CHROMA = 36.0
    private const val SECONDARY_CHROMA = 16.0
    private const val TERTIARY_CHROMA = 28.0
    private const val TERTIARY_HUE_SHIFT = 60.0
    private const val NEUTRAL_CHROMA = 4.0
    private const val NEUTRAL_VARIANT_CHROMA = 8.0
    private const val ERROR_HUE = 40.0
    private const val ERROR_CHROMA = 64.0
    private const val FULL_TURN = 360.0

    /** Roles whose tone is replaced in black (AMOLED) schemes, which draw content on pure black. */
    private val BLACK_TONES: Map<ColorRole, Int> =
        mapOf(
            ColorRole.BACKGROUND to 0,
            ColorRole.SURFACE to 0,
            ColorRole.SURFACE_DIM to 0,
            ColorRole.SURFACE_CONTAINER_LOWEST to 0,
            ColorRole.SURFACE_CONTAINER_LOW to 4,
            ColorRole.SURFACE_CONTAINER to 6,
            ColorRole.SURFACE_CONTAINER_HIGH to 10,
            ColorRole.SURFACE_CONTAINER_HIGHEST to 14,
            ColorRole.SURFACE_BRIGHT to 18,
        )

    /** The palettes derived from [seed]. */
    public fun palettes(seed: Int): Map<PaletteKey, TonalPalette> {
        val lch = ColorMath.toLch(seed)
        val tertiaryHue = (lch.hue + TERTIARY_HUE_SHIFT) % FULL_TURN
        return mapOf(
            PaletteKey.PRIMARY to TonalPalette(lch.hue, maxOf(lch.chroma, MIN_PRIMARY_CHROMA)),
            PaletteKey.SECONDARY to TonalPalette(lch.hue, SECONDARY_CHROMA),
            PaletteKey.TERTIARY to TonalPalette(tertiaryHue, TERTIARY_CHROMA),
            PaletteKey.ERROR to TonalPalette(ERROR_HUE, ERROR_CHROMA),
            PaletteKey.NEUTRAL to TonalPalette(lch.hue, NEUTRAL_CHROMA),
            PaletteKey.NEUTRAL_VARIANT to TonalPalette(lch.hue, NEUTRAL_VARIANT_CHROMA),
        )
    }

    /** The scheme for [seed]: light, dark, or [black] (a dark scheme on pure black) at [contrast]. */
    public fun generate(
        seed: Int,
        dark: Boolean,
        black: Boolean = false,
        contrast: ContrastLevel = ContrastLevel.STANDARD,
    ): SchemeColors {
        val palettes = palettes(seed)
        val isDark = dark || black
        return SchemeColors(
            ColorRole.entries.map { role ->
                val tone = BLACK_TONES[role]?.takeIf { black } ?: role.tone(isDark, contrast)
                palettes.getValue(role.palette).tone(tone)
            },
        )
    }
}
