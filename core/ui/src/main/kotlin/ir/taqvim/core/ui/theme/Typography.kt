/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private const val BOLD_STEP = 200

/** [weight] one bold step heavier (+200, at most [FontWeight.Black]); an unspecified weight counts as normal. */
public fun emboldened(weight: FontWeight?): FontWeight {
    val base = weight?.weight ?: FontWeight.Normal.weight
    return FontWeight((base + BOLD_STEP).coerceAtMost(FontWeight.Black.weight))
}

/**
 * The Material 3 type scale in [fontFamily] (the platform default when `null`), with every style one weight step
 * heavier when [bold] is set.
 */
public fun taqvimTypography(
    fontFamily: FontFamily? = null,
    bold: Boolean = false,
): Typography {
    val base = Typography()
    if (fontFamily == null && !bold) return base
    val adjust: (TextStyle) -> TextStyle = { style ->
        style.copy(
            fontFamily = fontFamily ?: style.fontFamily,
            fontWeight = if (bold) emboldened(style.fontWeight) else style.fontWeight,
        )
    }
    return base.copy(
        displayLarge = adjust(base.displayLarge),
        displayMedium = adjust(base.displayMedium),
        displaySmall = adjust(base.displaySmall),
        headlineLarge = adjust(base.headlineLarge),
        headlineMedium = adjust(base.headlineMedium),
        headlineSmall = adjust(base.headlineSmall),
        titleLarge = adjust(base.titleLarge),
        titleMedium = adjust(base.titleMedium),
        titleSmall = adjust(base.titleSmall),
        bodyLarge = adjust(base.bodyLarge),
        bodyMedium = adjust(base.bodyMedium),
        bodySmall = adjust(base.bodySmall),
        labelLarge = adjust(base.labelLarge),
        labelMedium = adjust(base.labelMedium),
        labelSmall = adjust(base.labelSmall),
    )
}
