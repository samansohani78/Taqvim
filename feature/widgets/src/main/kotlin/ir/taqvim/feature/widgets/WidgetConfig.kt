/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import ir.taqvim.core.model.CalendarSystem
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet

/** Widget background color: a role of the current theme (dynamic where available) or plain black or white. */
enum class WidgetBackground {
    SURFACE,
    PRIMARY_CONTAINER,
    SECONDARY_CONTAINER,
    BLACK,
    WHITE,
}

/**
 * The settings of one placed widget (T-1200 configuration screen).
 *
 * @property transparencyPercent 0 (opaque) … [MAX_TRANSPARENCY], in steps of [TRANSPARENCY_STEP].
 * @property scalePercent text and element scale, one of [SCALES].
 * @property contents optional parts switched on; only parts the widget offers are kept.
 * @property secondaryCalendar calendar of the secondary date line, or `null` for the language's second calendar.
 */
data class WidgetConfig(
    val background: WidgetBackground = WidgetBackground.SURFACE,
    val transparencyPercent: Int = 0,
    val scalePercent: Int = DEFAULT_SCALE,
    val contents: ImmutableSet<WidgetContent> = WidgetContent.entries.toImmutableSet(),
    val secondaryCalendar: CalendarSystem? = null,
) {
    /** This configuration with every value in range for a widget of [kind]. */
    fun normalizedFor(kind: WidgetKind): WidgetConfig =
        copy(
            transparencyPercent = roundToStep(transparencyPercent.coerceIn(0, MAX_TRANSPARENCY), TRANSPARENCY_STEP),
            scalePercent = SCALES.minBy { kotlin.math.abs(it - scalePercent) },
            contents = contents.filter { it in kind.contents }.toImmutableSet(),
        )

    fun shows(content: WidgetContent): Boolean = content in contents

    companion object {
        /** Fully transparent widgets would be invisible, so transparency stops short of it. */
        const val MAX_TRANSPARENCY: Int = 90
        const val TRANSPARENCY_STEP: Int = 10
        const val DEFAULT_SCALE: Int = 100
        val SCALES: List<Int> = listOf(75, 100, 125, 150)

        /** The configuration of a newly placed widget of [kind]: every optional part it offers switched on. */
        fun defaultFor(kind: WidgetKind): WidgetConfig = WidgetConfig(contents = kind.contents.toImmutableSet())

        private fun roundToStep(
            value: Int,
            step: Int,
        ): Int = (value.toFloat() / step).roundToInt() * step
    }
}

/** Color and size arithmetic of widget configurations. */
object WidgetAppearance {
    private const val ALPHA_SHIFT = 24
    private const val CHANNEL = 0xFF
    private const val RGB_MASK = 0xFFFFFF
    private const val PERCENT = 100

    /** [argb] with its alpha reduced by [transparencyPercent] (0 keeps it, 100 makes it fully transparent). */
    fun withTransparency(
        argb: Int,
        transparencyPercent: Int,
    ): Int {
        val alpha = (argb ushr ALPHA_SHIFT) and CHANNEL
        val kept = PERCENT - transparencyPercent.coerceIn(0, PERCENT)
        val newAlpha = (alpha * kept.toFloat() / PERCENT).roundToInt()
        return (newAlpha shl ALPHA_SHIFT) or (argb and RGB_MASK)
    }

    /** [value] (a text size or dimension) scaled by [scalePercent]. */
    fun scaled(
        value: Float,
        scalePercent: Int,
    ): Float = value * scalePercent / PERCENT
}
