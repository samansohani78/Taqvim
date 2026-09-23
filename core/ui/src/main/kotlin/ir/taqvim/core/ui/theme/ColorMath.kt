/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** A CIE L*C*h(ab) color: [lightness] 0‥100 (the "tone"), [chroma] ≥ 0 and [hue] in degrees 0‥360. */
public data class Lch(
    public val lightness: Double,
    public val chroma: Double,
    public val hue: Double,
)

/**
 * Color conversions for opaque `0xAARRGGBB` colors: sRGB transfer function and D65 matrices (IEC 61966-2-1),
 * CIE 1976 L*a*b* (CIE 15:2004) and the WCAG 2.2 relative luminance and contrast ratio. See docs/PROVENANCE.md.
 */
public object ColorMath {
    private const val CHANNEL_MAX = 255.0
    private const val TRANSFER_THRESHOLD = 0.04045
    private const val LINEAR_THRESHOLD = 0.0031308
    private const val LINEAR_SLOPE = 12.92
    private const val GAMMA = 2.4
    private const val GAMMA_OFFSET = 0.055
    private const val GAMMA_SCALE = 1.055
    private const val CONTRAST_FLARE = 0.05
    private const val LAB_EPSILON = 6.0 / 29.0
    private const val LAB_OFFSET = 4.0 / 29.0
    private const val LAB_THRESHOLD = LAB_EPSILON * LAB_EPSILON * LAB_EPSILON
    private const val LAB_SLOPE = 3 * LAB_EPSILON * LAB_EPSILON
    private const val LIGHTNESS_SCALE = 116.0
    private const val LIGHTNESS_OFFSET = 16.0
    private const val A_SCALE = 500.0
    private const val B_SCALE = 200.0
    private const val HALF_TURN = 180.0
    private const val FULL_TURN = 360.0

    /**
     * How far outside 0‥1 a linear channel may sit and still count as inside the gamut.
     *
     * The sRGB↔XYZ matrices above are the published four-decimal values (IEC 61966-2-1), so they are only self-
     * consistent to about 1e-4: converting an in-gamut corner such as pure yellow back through them lands 6.2e-5
     * outside the range. A tolerance tighter than the matrices' own precision made [fromLch] treat that corner as
     * out of gamut and desaturate it — 0xFFFFFF00 came back as 0xFFFFF9C5 — so the tolerance matches their
     * precision. It is far below half of an 8-bit step, so no colour inside the gamut is moved by it.
     */
    private const val GAMUT_TOLERANCE = 1e-4
    private const val CHROMA_SEARCH_STEPS = 24
    private const val OPAQUE = 0xFF000000.toInt()
    private const val BYTE = 0xFF
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8

    /** D65 white point, from the row sums of the sRGB-to-XYZ matrix. */
    private const val WHITE_X = 0.9505
    private const val WHITE_Z = 1.089

    /** WCAG 2.2 relative luminance of [argb] (0 for black, 1 for white). */
    public fun relativeLuminance(argb: Int): Double {
        val (r, g, b) = linearChannels(argb)
        return luminance(r, g, b)
    }

    /** WCAG 2.2 contrast ratio between two colors, from 1 to 21. */
    public fun contrastRatio(
        first: Int,
        second: Int,
    ): Double {
        val a = relativeLuminance(first)
        val b = relativeLuminance(second)
        return (max(a, b) + CONTRAST_FLARE) / (min(a, b) + CONTRAST_FLARE)
    }

    /** [argb] as CIE L*C*h(ab). */
    @Suppress("MagicNumber") // IEC 61966-2-1 sRGB → XYZ matrix.
    public fun toLch(argb: Int): Lch {
        val (r, g, b) = linearChannels(argb)
        val x = 0.4124 * r + 0.3576 * g + 0.1805 * b
        val y = luminance(r, g, b)
        val z = 0.0193 * r + 0.1192 * g + 0.9505 * b
        val fy = labF(y)
        val a = A_SCALE * (labF(x / WHITE_X) - fy)
        val bStar = B_SCALE * (fy - labF(z / WHITE_Z))
        val hue = Math.toDegrees(atan2(bStar, a))
        return Lch(LIGHTNESS_SCALE * fy - LIGHTNESS_OFFSET, hypot(a, bStar), hue - FULL_TURN * floor(hue / FULL_TURN))
    }

    /**
     * The opaque sRGB color of [lch]'s lightness and hue with the largest chroma up to [Lch.chroma] that lies inside
     * the sRGB gamut. Lightness is kept, so contrast between tones does not depend on the hue.
     */
    public fun fromLch(lch: Lch): Int {
        val lightness = lch.lightness.coerceIn(0.0, LIGHTNESS_SCALE - LIGHTNESS_OFFSET)
        if (inGamut(lightness, lch.chroma, lch.hue)) return toArgb(linearFromLch(lightness, lch.chroma, lch.hue))
        var low = 0.0
        var high = lch.chroma
        repeat(CHROMA_SEARCH_STEPS) {
            val middle = (low + high) / 2
            if (inGamut(lightness, middle, lch.hue)) low = middle else high = middle
        }
        return toArgb(linearFromLch(lightness, low, lch.hue))
    }

    private fun inGamut(
        lightness: Double,
        chroma: Double,
        hue: Double,
    ): Boolean = linearFromLch(lightness, chroma, hue).all { it in -GAMUT_TOLERANCE..1 + GAMUT_TOLERANCE }

    @Suppress("MagicNumber") // IEC 61966-2-1 XYZ → sRGB matrix.
    private fun linearFromLch(
        lightness: Double,
        chroma: Double,
        hue: Double,
    ): List<Double> {
        val radians = hue * Math.PI / HALF_TURN
        val fy = (lightness + LIGHTNESS_OFFSET) / LIGHTNESS_SCALE
        val x = WHITE_X * labFInverse(fy + chroma * cos(radians) / A_SCALE)
        val y = labFInverse(fy)
        val z = WHITE_Z * labFInverse(fy - chroma * sin(radians) / B_SCALE)
        return listOf(
            3.2406 * x - 1.5372 * y - 0.4986 * z,
            -0.9689 * x + 1.8758 * y + 0.0415 * z,
            0.0557 * x - 0.2040 * y + 1.0570 * z,
        )
    }

    @Suppress("MagicNumber") // WCAG 2.2 / IEC 61966-2-1 luminance coefficients.
    private fun luminance(
        r: Double,
        g: Double,
        b: Double,
    ): Double = 0.2126 * r + 0.7152 * g + 0.0722 * b

    private fun linearChannels(argb: Int): List<Double> =
        listOf(argb shr RED_SHIFT, argb shr GREEN_SHIFT, argb).map { linearize((it and BYTE) / CHANNEL_MAX) }

    private fun linearize(encoded: Double): Double =
        if (encoded <= TRANSFER_THRESHOLD) {
            encoded / LINEAR_SLOPE
        } else {
            ((encoded + GAMMA_OFFSET) / GAMMA_SCALE).pow(GAMMA)
        }

    private fun encode(linear: Double): Int {
        val clamped = linear.coerceIn(0.0, 1.0)
        val encoded =
            if (clamped <= LINEAR_THRESHOLD) {
                clamped * LINEAR_SLOPE
            } else {
                GAMMA_SCALE * clamped.pow(1 / GAMMA) - GAMMA_OFFSET
            }
        return (encoded * CHANNEL_MAX).roundToInt()
    }

    private fun toArgb(linear: List<Double>): Int {
        val (r, g, b) = linear.map(::encode)
        return OPAQUE or (r shl RED_SHIFT) or (g shl GREEN_SHIFT) or b
    }

    private fun labF(t: Double): Double = if (t > LAB_THRESHOLD) cbrt(t) else t / LAB_SLOPE + LAB_OFFSET

    private fun labFInverse(t: Double): Double = if (t > LAB_EPSILON) t * t * t else LAB_SLOPE * (t - LAB_OFFSET)
}
